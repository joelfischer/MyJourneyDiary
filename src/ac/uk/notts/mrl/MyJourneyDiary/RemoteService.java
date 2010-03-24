package ac.uk.notts.mrl.MyJourneyDiary;

/**
 * @author jef@cs.nott.ac.uk
 * The remote service that handles recording of the journeys. It maintains a LocationListener, writes locations to db 
 * and returns location for pushpins to the UI @see OverviewMap. It will run persistently in the background so recording
 * is possible while the user does other things. The main starting point is onBind() which is called by OverviewMap's call 
 * of bindService. 
 * 
 */

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;

/**
 * This is an implementation of an application service that runs in a
 * different process than the application. Because it can be in another
 * process, we must use IPC to interact with it.  
 */
public class RemoteService extends Service {
	
	private static final int DISTANCE_BETWEEN_RECS = 20;	//in metres
	private static final long LISTENER_INTERVAL = 1000*3; //in milliseconds
	private static final double DEGREES_TO_RADIANS = (double)(180/Math.PI); 
	private static final int EARTH_RADIUS = 6378140;
    /**
     * This is a list of callbacks that have been registered with the
     * service.  Note that this is package scoped (instead of private) so
     * that it can be accessed more efficiently from inner classes. It is defined in IDL, 
     * @see IRemoteServiceCallback.aidl
     */
    final RemoteCallbackList<IRemoteServiceCallback> mCallbacks
            = new RemoteCallbackList<IRemoteServiceCallback>();
    
    int mValue = 0;
    NotificationManager mNM;
    
    private int userSetDistance = 20;
    private String geoPoint="";
    private MyLocationListener locListener;
    private LocationManager locMgr;
    private SQLiteDatabase db;
    private int journeyId = 0;
    private WorkerThread thread;
    
    @Override
    public void onCreate() {
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        locMgr = (LocationManager)getSystemService(LOCATION_SERVICE);
        
        // Display a notification about us starting.
        showNotification();        
    }
    

    @Override
    public void onDestroy() {
        // Cancel the persistent notification.
        mNM.cancel(R.string.remote_service_started);

        // Tell the user we stopped.
        Toast.makeText(this, "Sucessfully recorded journey", Toast.LENGTH_SHORT).show();
        
        locMgr.removeUpdates(locListener);
        
        // Unregister all callbacks.
        mCallbacks.kill();
    }
    
    

    @Override
    public IBinder onBind(Intent intent) {
    	String args = intent.getStringExtra("args");
    	journeyId = Integer.parseInt(args.substring(0, args.indexOf("/")));
    	userSetDistance = Integer.parseInt (args.substring(args.indexOf("/")+1));
    	
    	Log.d("RemoteService", "journeyId:"+journeyId +", userSetDistance:"+userSetDistance);
    	
    	//run the thread that does the work
        thread = new WorkerThread();
        thread.run();
        
    	return mBinder;
    }

    /**
     * The IRemoteInterface is defined through IDL
     * @see IRemoteService.aidl 
     */
    private final IRemoteService.Stub mBinder = new IRemoteService.Stub() {
        public void registerCallback(IRemoteServiceCallback cb) {
            if (cb != null) mCallbacks.register(cb);
        }
        public void unregisterCallback(IRemoteServiceCallback cb) {
            if (cb != null) mCallbacks.unregister(cb);
        }
    };
   
    private static final int REPORT_MSG = 1;
    
    /**
     * Our Handler used to execute operations on the main thread.  
     */
    private final Handler mHandler = new Handler() {
        @Override public void handleMessage(Message msg) {
            switch (msg.what) {
                
                case REPORT_MSG: {
                	
                    String value = (String)msg.obj;
                    Log.d("RemoteService","lat/lng:"+value);
                    
                    // Broadcast to all clients the new value.
                    final int N = mCallbacks.beginBroadcast();
                    for (int i=0; i<N; i++) {
                        try {
                            mCallbacks.getBroadcastItem(i).valueChanged(value);
                        } catch (RemoteException e) {
                            // The RemoteCallbackList will take care of removing
                            // the dead object for us.
                        }
                    }
                    mCallbacks.finishBroadcast();
                } break;
                default:
                    super.handleMessage(msg);
            }
        }
    };
    
public class WorkerThread implements Runnable {
    	
    	public boolean isRunning = false;

     	WorkerThread() {}
     		
		public void run() {
			locMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, LISTENER_INTERVAL, DISTANCE_BETWEEN_RECS,	
	    			locListener = new MyLocationListener());
			this.isRunning = true;
		}
    }

	private GeoPoint lastPushpin;
    
	/**
	 * Implementation of LocationListener that writes locations to db ca. every 20m. 
	 * Also, it checks distance of current pushpin location to the last in order to determine if to send location back to the UI
	 * so pushpins will be put on map. Distancse depend on the mode of transport.
	 *
	 */
    public class MyLocationListener implements LocationListener {
		public void onLocationChanged(Location location) {
			//debug
    		Log.d("OverviewMap","onLocationChanged");
			if (location != null) {
				
				if (db == null || !db.isOpen())
					db = new LocationsSQLHelper(RemoteService.this).getWritableDatabase();
				
				double lat = (double) (location.getLatitude() );
				double lng = (double) (location.getLongitude() );
				int latCorr = (int) (location.getLatitude() * 1E6);
				int lngCorr = (int) (location.getLongitude() * 1E6);
				GeoPoint currentLocation = new GeoPoint(latCorr, lngCorr);
				
				//make location lastPushpin if run for first time
				if (lastPushpin == null) {
					lastPushpin = currentLocation;
					geoPoint = latCorr +"/"+lngCorr;
					
					//send to interface
					mHandler.sendMessage(Message.obtain(mHandler, REPORT_MSG, geoPoint));
					db.execSQL("INSERT INTO locations (journeyId,lat,long,timestamp) VALUES ("+journeyId+","+latCorr+","+lngCorr+
    						","+System.currentTimeMillis()+")");
				}
				else if (lastPushpin != null) {
					//not first location, check distance...
					//get distance between current and last pushpin location 
					double distance = calculateDistance((double)lastPushpin.getLatitudeE6()/1E6, (double)lastPushpin.getLongitudeE6()/1E6, 
							lat, lng); 
					
					if (distance > userSetDistance) {
						//OK, put on map...
						
						//send to handler
						geoPoint = latCorr+"/"+lngCorr;
						mHandler.sendMessage(Message.obtain(mHandler, REPORT_MSG, geoPoint));
						
						lastPushpin = currentLocation;
					}
					
					if (distance > DISTANCE_BETWEEN_RECS) {
						//record waypoints to db every 20m
	    				db.execSQL("INSERT INTO locations (journeyId,lat,long,distance,timestamp) VALUES ("+journeyId+","+latCorr+","+lngCorr+
	    						","+distance+","+System.currentTimeMillis()+")");
					}
				}
			}
				
		}

		public void onProviderDisabled(String provider) {
			// TODO Auto-generated method stub
			
		}

		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub
			
		}

		public void onStatusChanged(String provider, int status,
				Bundle extras) {
//			Log.d("OverviewMap","LocationProvider: "+provider);
//			
//			if (status == LocationProvider.OUT_OF_SERVICE) {
//				Log.w("OverviewMap","Location provider OUT_OF_SERVICE");
//			}
//			if (status == LocationProvider.TEMPORARILY_UNAVAILABLE) {
//				Log.w("OverviewMap","Location provider TEMPORARILY_UNAVAILABLE");
//			}
//			if (status == LocationProvider.AVAILABLE) {
//				Log.w("OverviewMap","Location provider AVAILABLE");
//			}
		}
	}
    
    private double calculateDistance(double aLat, double aLong, double bLat, double bLong) {
		double distance;
		
		 double aLatRad =  (aLat / DEGREES_TO_RADIANS);
         double aLongRad =  (aLong / DEGREES_TO_RADIANS);
         double bLatRad =  (bLat / DEGREES_TO_RADIANS);
         double bLongRad =  (bLong / DEGREES_TO_RADIANS);

         // Calculate the length of the arc that subtends point a and b, using the spherical law of cosines, 
         //see http://www.movable-type.co.uk/scripts/latlong.html
         double t1 = Math.cos(aLatRad)*Math.cos(aLongRad)*Math.cos(bLatRad)*Math.cos(bLongRad);
         double t2 = Math.cos(aLatRad)*Math.sin(aLongRad)*Math.cos(bLatRad)*Math.sin(bLongRad);
         double t3 = Math.sin(aLatRad)*Math.sin(bLatRad);
         double tt = Math.acos(t1 + t2 + t3); 
		
         //distance in meters
         distance = tt * EARTH_RADIUS;
		
		return distance;
	}

    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.remote_service_started);

        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.record, text,
                System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, OverviewMap.class), 0);

        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, getText(R.string.remote_service_label),
                       text, contentIntent);

        // Send the notification.
        // We use a string id because it is a unique number.  We use it later to cancel.
        mNM.notify(R.string.remote_service_started, notification);
    }
}
