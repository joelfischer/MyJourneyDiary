package ac.uk.notts.mrl.MyJourneyDiary;

import java.io.BufferedWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ZoomControls;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class OverviewMapOLD2 extends MapActivity {
	
	private static final double DEGREES_TO_RADIANS = (double)(180/Math.PI); 
	private static final int EARTH_RADIUS = 6378140;
	private static final int DISTANCE_BETWEEN_RECS = 20;	//in metres
	private static final int JOURNEY_ID_COL = 0;
	private static final long LISTENER_INTERVAL = 1000*3; //in milliseconds

	private boolean startRecording = false;
	private boolean isRecording = false;
	 
	private int userSetDistance;
//	private String purpose = "";
//	private String transport = "";
//	private String comment = "";
//	private String q1 = "";
//	private String q2y="";
//	private String q2n="";
	private LinearLayout linearLayout;
	MapView mapView;
	ZoomControls mZoom;
	List<Overlay> mapOverlays;
	Drawable drawable;
	Drawable pushpin;
	HelloItemizedOverlay itemizedOverlay;
	PushpinOverlay pushpinOverlay;
	MyLocationOverlay mMyLocationOverlay;
	private LocationManager locMgr;
//	private BufferedWriter bufferedWriter;
	private MyLocationListener locListener;
	private MapController mapController;
	private PowerManager pm;
	private WakeLock stayAwake;
	private SQLiteDatabase db;
	private SQLiteDatabase userdata;
	private SQLiteDatabase reminders;
//	private boolean hasFocus = false;
	private int journeyId;
	private boolean exitQuestions = false;
	private WorkerThread thread;
	private IRemoteService mService = null;		//first!
    private boolean mIsBound;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("OverviewMap", "onCreate");
        setContentView(R.layout.main);
                
        linearLayout = (LinearLayout) findViewById(R.id.zoomview);
        mapView = (MapView) findViewById(R.id.mapview);
        mZoom = (ZoomControls) mapView.getZoomControls();
        
        linearLayout.addView(mZoom);
        
        //get overlays
        mapOverlays = mapView.getOverlays();
//        drawable = this.getResources().getDrawable(R.drawable.androidmarker);
        pushpin = this.getResources().getDrawable(R.drawable.pushpin_gif);

        
//        itemizedOverlay = new HelloItemizedOverlay(drawable);
        
        pushpinOverlay = new PushpinOverlay(pushpin);
        
        //some test locations to debug
        GeoPoint point = new GeoPoint(19240000,-99120000);
        OverlayItem overlayitem = new OverlayItem(point, "", "");
        
        GeoPoint point2 = new GeoPoint(52957499, -1196373);
        OverlayItem overlayitem2 = new OverlayItem(point2, "", "");
        
        //testmap at work 
        GeoPoint point3 = new GeoPoint(52953478, -1187102);
        OverlayItem overlayitem3 = new OverlayItem(point3, "", "");
        
        
//        itemizedOverlay.addOverlay(overlayitem);
//        itemizedOverlay.addOverlay(overlayitem3);
//        mapOverlays.add(itemizedOverlay);
		
		mapController = mapView.getController();
		
		mMyLocationOverlay = new MyLocationOverlay(this, mapView);
        mMyLocationOverlay.runOnFirstFix(new Runnable() { public void run() {
            mapView.getController().animateTo(mMyLocationOverlay.getMyLocation());
//            Looper.prepare();
            Log.d("OverviewMap", "location:" +mMyLocationOverlay.getMyLocation().toString());
//            Toast.makeText(OverviewMap.this, "Location:"+mMyLocationOverlay.getMyLocation().toString(), Toast.LENGTH_SHORT).show();
//            mapController.setCenter(mMyLocationOverlay.getMyLocation());
        }});
        mapView.getOverlays().add(mMyLocationOverlay);
        mapView.getController().setZoom(15);
        mapView.setClickable(true);
        mapView.setEnabled(true);     
//        mapView.setBuiltInZoomControls(true);
        
        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        
        locMgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        
    	
    	Location loc = locMgr.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    	if (loc != null) {
    		Log.d("OverviewMap", "currentLocation lat:"+loc.getLatitude()+", long:" + loc.getLongitude());
    	}	
    	
//    	try {
//    		 startRecording = this.getIntent().getBooleanExtra("startRecording",false);
//    	} catch (Exception e) {
//    		Log.e("OverviewMap", "boolean extra "+e.toString());
//    	}
//    	
//    	Log.d("OverviewMap","startRecording "+startRecording);
//    	
//    	if (startRecording)
//    		startRecording();
    	
    	//check if the user has already set a time for a reminder, if not, let them do it! 
    	reminders = new ReminderSQLHelper(OverviewMapOLD2.this).getReadableDatabase();
    	Cursor c = reminders.rawQuery("SELECT morning_reminder_hr FROM reminder", null);
    	if (c.getCount()==0) {
    		//no reminders set yet, start activity to do so
    		startActivity(new Intent(OverviewMapOLD2.this, SetReminders.class));
    	}
    	c.close();
    	reminders.close();
	}
    
    public class WorkerThread implements Runnable {
    	
    	public boolean isRunning = false;

     	WorkerThread() {}
     		
		public void run() {
			locMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, LISTENER_INTERVAL, DISTANCE_BETWEEN_RECS,	
	    			locListener = new MyLocationListener());
			this.isRunning = true;
		}
    }
    
   	public class MyLocationListener implements LocationListener {
		public void onLocationChanged(Location location) {
			//debug
//    		Log.d("OverviewMap","onLocationChanged");
			if (location != null) {
				
				if (db == null || !db.isOpen())
					db = new LocationsSQLHelper(OverviewMapOLD2.this).getWritableDatabase();
				
				double lat = (double) (location.getLatitude() );
				double lng = (double) (location.getLongitude() );
				int latCorr = (int) (location.getLatitude() * 1E6);
				int lngCorr = (int) (location.getLongitude() * 1E6);
				GeoPoint currentLocation = new GeoPoint(latCorr, lngCorr);
				OverlayItem pushpinLoc = new OverlayItem(currentLocation, "", "");
				
				//only add an overlay if the distance to the last one is at least 10m
				List<Overlay> pinList = mapView.getOverlays();
				if (pinList.size()>0) {
//					Log.d("OverviewMap", "pinList.size:"+pinList.size());
					//check if we have a PushpinOverlay
					if (pinList.contains(pushpinOverlay)) {
						//OK, get last added overlay
//						Log.d("OverviewMap","pushpinOverlay detected");
					PushpinOverlay lastAddedOverlay = (PushpinOverlay)pinList.get(pinList.lastIndexOf(pushpinOverlay));
    					if (lastAddedOverlay.size()>0) {
    						//get last added item
    						OverlayItem lastPushpin = (OverlayItem)lastAddedOverlay.getItem(lastAddedOverlay.size()-1);
    						GeoPoint lastLocation = lastPushpin.getPoint();
    						
    						//get distance between current and last location 
    						double distance = calculateDistance((double)lastLocation.getLatitudeE6()/1E6, (double)lastLocation.getLongitudeE6()/1E6, 
    								lat, lng); 
    						//debug
//    						Log.d("OverviewMap", "locations:"+(double)lastLocation.getLatitudeE6()/1E6+","+ (double)lastLocation.getLongitudeE6()/1E6+", "+ 
//    								location.getLatitude()+","+location.getLongitude());
//    						Log.d("OverviewMap","Distance to last added pin:" +distance +" m");
    						
    						if (distance > userSetDistance) {
    							//put pushpins on map relative to mode of transport (=>userSetDistance)
    							pushpinOverlay.addOverlay(pushpinLoc);
			    				mapView.getOverlays().add(pushpinOverlay);
//			    				mapView.refreshDrawableState();
			    	            mapController.setCenter(currentLocation);
//			    				Log.d("OverviewMap", "distance big enough: "+userSetDistance+", just added new pushpin");
//			    				Toast.makeText(
//			    						getBaseContext(),
//			    						"latitude [" + location.getLatitude()
//			    								+ "] longitude [" + location.getLongitude()
//			    								+ "]", Toast.LENGTH_SHORT).show();
			    				
    						}
    						if (distance > DISTANCE_BETWEEN_RECS) {
        						//record waypoints to db every 20m
			    				db.execSQL("INSERT INTO locations (journeyId,lat,long,distance,timestamp) VALUES ("+journeyId+","+latCorr+","+lngCorr+
			    						","+distance+","+System.currentTimeMillis()+")");
    						}
    						
    					}
					}
					else {
						//no pushpinOverlay in Overlays yet, add first one:
						pushpinOverlay.addOverlay(pushpinLoc);
	    				mapView.getOverlays().add(pushpinOverlay);
	    				mapView.refreshDrawableState();
//	    				Log.d("OverviewMap","added FIRST pushpin");
	    				
	    				//db.execSQL("INSERT INTO locations (journeyId,lat,long) VALUES ("+journeyId+","+latCorr+","+lngCorr+")");
	    				//debug
	    				db.execSQL("INSERT INTO locations (journeyId,lat,long,timestamp) VALUES ("+journeyId+","+latCorr+","+lngCorr+
	    						","+System.currentTimeMillis()+")");
					}
				}
				
				
//				mapController.setCenter(point);
				
				
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
		
    	private void resumeRecording() {
    		if (stayAwake == null || !stayAwake.isHeld()) {
	    		stayAwake = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "");
				stayAwake.acquire();
    		}
			
			//create db to record locations 
			if (db == null || (!db.isOpen()))
				db = (new LocationsSQLHelper(OverviewMapOLD2.this)).getWritableDatabase();
			
			//determine journeyId
			Cursor c = db.rawQuery("SELECT journeyId FROM locations" +
            		" WHERE id=(SELECT max(id) FROM locations)", null);
			if (c.getCount()>0) {
				c.moveToFirst();
	            journeyId = 0;
	            try {
	            	journeyId = c.getInt(JOURNEY_ID_COL);
	            } catch (NullPointerException npe) {
	            	Log.e("OverviewMap", npe.toString());
	            }
	            Log.d("OverviewMap", "Resumed JourneyId:"+journeyId);
			}
			c.close();
			db.close();
			thread = new WorkerThread();
			thread.run();
    	}
    	
    	private void startRecording() {
    		isRecording = true;
    		
    		//not necessary with service?
//    		stayAwake = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "");
//			stayAwake.acquire();
    		
			OverviewMapOLD2.this.getIntent().removeExtra("startRecording");
			
			//create db to record locations 
			if (db == null || (!db.isOpen()))
				db = (new LocationsSQLHelper(OverviewMapOLD2.this)).getWritableDatabase();
			
			//determine journeyId
			Cursor c = db.rawQuery("SELECT journeyId FROM locations" +
            		" WHERE id=(SELECT max(id) FROM locations)", null);
			if (c.getCount()>0) {
				c.moveToFirst();
	            journeyId = 0;
	            try {
	            	journeyId = c.getInt(JOURNEY_ID_COL);
	            } catch (NullPointerException npe) {
	            	Log.e("OverviewMap", npe.toString());
	            }
	            journeyId++;
	            Log.d("OverviewMap", "JourneyId:"+journeyId);
			}
			else {
				//first row
				journeyId = 1;
				Log.d("OverviewMap", "FIRST JourneyId:"+journeyId);
			}
			c.close();
            db.close();
			
            //update the userdata DB with current journeyId
			if (userdata == null || (!userdata.isOpen()))
				userdata = new UserDataSQLHelper(OverviewMapOLD2.this).getWritableDatabase();
			
			userdata.execSQL("UPDATE userdata SET journeyId="+journeyId+" WHERE id=(SELECT MAX(id) FROM userdata)");
			
			//determine pin distance for map from mode of transport 
			Cursor cc = userdata.rawQuery("SELECT purpose,transport,beforeJourney,q1,q2y,q2n,comment FROM userdata WHERE id=(SELECT MAX(id) FROM userdata)", null);
			Log.d("OverviewMap","cc: "+cc.getCount());
			if (cc.getCount()!=0) {
				cc.moveToFirst();
				String transport = cc.getString(1);
				if (transport.equals("walking")) {
					this.userSetDistance = 30;
				}
				if ((transport.equals("cycling"))||(transport.equals("--"))) {
					this.userSetDistance = 100;
				}
				if ((transport.equals("bus"))||(transport.equals("car"))||(transport.equals("taxi"))||
						(transport.equals("train"))||(transport.equals("tram"))||(transport.equals("other"))) {
					this.userSetDistance = 250;
				}
				Log.d("OverviewMap","transport: "+transport);
				Log.d("OverviewMap:Distance: ",""+userSetDistance);
//				if (transport != null) {
//					OverviewMap.this.transport = transport;
//				}
				
//				String purpose = cc.getString(0);
//				if (!purpose.equals(""))
//					this.purpose = purpose;
//				
//				int beforeJourney = cc.getInt(2);
//				
//				if (beforeJourney == 1) {
//					
//					String q1 = cc.getString(3);
//					if (!q1.equals("")) 
//						this.q1 = q1;
//					String q2y = cc.getString(4);
//					if (q2y == null)
//						q2y = "";
//					if (!q2y.equals(""))
//						this.q2y = q2y;
//					String q2n = cc.getString(5);
//					if (q2n == null)
//						q2n = "";
//					if (!q2n.equals(""))
//						this.q2n = q2n;
//					String prelComment = "";
//					try {
//						prelComment = cc.getString(6);
//					} catch (NullPointerException npe) {
//						Log.e("OverviewMap","NPE: "+npe.toString());
//					}
//					if (prelComment != null) {
//						if (!prelComment.equals(""))
//							this.comment = prelComment;
//					}
//				}
			}
			cc.close();
			userdata.close();
//
//			thread = new WorkerThread();
//			thread.run();
			
			//start a service that writes locations to db in background?
			bindService(new Intent(IRemoteService.class.getName()).putExtra("args", journeyId+"/"+userSetDistance),
                    mConnection, Context.BIND_AUTO_CREATE);
			mIsBound = true;
    	}


		//need a button to start stop
    	public boolean onCreateOptionsMenu(android.view.Menu menu) {
    		MenuItem item = menu.add(0, 0, 0, "START recording journey");
    		MenuItem item2 = menu.add(0, 1, 1, "STOP recording journey");
    		MenuItem item3 = menu.add(0,2,2, "Show journeys");
    		MenuItem item4 = menu.add(0,2,2, "Change reminders");
    		MenuItem item5 = menu.add(0,3,0, "Service rec start");
    		MenuItem item6 = menu.add(0,3,0, "Service rec stop");
    		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
    			public boolean onMenuItemClick(MenuItem item) {
    				
    				if (!isRecording) {
    					startActivity(new Intent(OverviewMapOLD2.this, SettingsActivity.class));
    					return true;
    				}
    				else if (isRecording) {
    					Toast.makeText(OverviewMapOLD2.this, "You are already recording!", Toast.LENGTH_SHORT).show();
    					return false; 
    				}
    				return true;
    			}
    		});
    		item2.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				public boolean onMenuItemClick(MenuItem item) {
					startRecording = false;
					if (!isRecording) {
						Toast.makeText(OverviewMapOLD2.this, "You need to start recording first!", Toast.LENGTH_SHORT).show();
						return false;
					}
					isRecording = false;
					OverviewMapOLD2.this.getIntent().removeExtra("isRecording");
//					try {
//						locMgr.removeUpdates(locListener);
//					} catch (IllegalArgumentException e) {
//						Log.e("OveviewMap",e.toString());
//						Toast.makeText(OverviewMap.this, "You need to start recording first!", Toast.LENGTH_SHORT).show();
//						return false;
//					}
//					try {
//						stayAwake.release();
//					}catch (NullPointerException npe) {
//						Log.e("OverviewMap",npe.toString());
//						Toast.makeText(OverviewMap.this, "You need to start recording first!", Toast.LENGTH_SHORT).show();
//						return false;
//					}catch (RuntimeException e) {
//						Log.e("OverviewMap",e.toString());
//					}
					db.close();
					
					if (mIsBound) {
    					if (mService != null) {
    	                    try {
    	                        mService.unregisterCallback(mCallback);
    	                    } catch (RemoteException e) {
    	                        // There is nothing special we need to do if the service
    	                        // has crashed.
    	                    }
    					}   
    				}
    				// Detach our existing connection.
                    unbindService(mConnection);
                    mIsBound = false;
					
//					Toast.makeText(OverviewMap.this, "Successfully recorded journey", Toast.LENGTH_SHORT).show();
					
					if (exitQuestions) {
						exitQuestions = false;
						startActivity(new Intent(OverviewMapOLD2.this, Question1E.class));
					}
					
					return true;
				}
			});
    		
    		item3.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				public boolean onMenuItemClick(MenuItem item) {
					if (isRecording) {
    					Toast.makeText(OverviewMapOLD2.this, "Please finish recording first!", Toast.LENGTH_SHORT).show();
    					return false;
					}
					else if (!isRecording) {
					startActivity(new Intent (OverviewMapOLD2.this, LocationListView.class));
					return true;
					}
					return true;
				}
			});
    		
    		item4.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				public boolean onMenuItemClick(MenuItem item) {
					if (isRecording) {
    					Toast.makeText(OverviewMapOLD2.this, "Please finish recording first!", Toast.LENGTH_SHORT).show();
    					return false;
					}
					else if (!isRecording) {
						startActivity(new Intent (OverviewMapOLD2.this, SetReminders.class));
						return true;
					}
					return true;
				}
			});
    		
    		item5.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
    			public boolean onMenuItemClick(MenuItem item) {
    				bindService(new Intent(IRemoteService.class.getName()).putExtra("args", journeyId+"/"+userSetDistance),
    	                    mConnection, Context.BIND_AUTO_CREATE);
    				mIsBound = true;
    				return true;
    			}
    		});
    		
    		item6.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
    			public boolean onMenuItemClick(MenuItem item) {
    				if (mIsBound) {
    					if (mService != null) {
    	                    try {
    	                        mService.unregisterCallback(mCallback);
    	                    } catch (RemoteException e) {
    	                        // There is nothing special we need to do if the service
    	                        // has crashed.
    	                    }
    					}   
    				}
    				// Detach our existing connection.
                    unbindService(mConnection);
                    mIsBound = false;
    			
    				return true;
    			}
    		});
    		
    		
    		
    		return true;
    	}
    	
    	private ServiceConnection mConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className,
                    IBinder service) {
                // This is called when the connection with the service has been
                // established, giving us the service object we can use to
                // interact with the service.  We are communicating with our
                // service through an IDL interface, so get a client-side
                // representation of that from the raw service object.
                mService = IRemoteService.Stub.asInterface(service);


                // We want to monitor the service for as long as we are
                // connected to it.
                try {
                    mService.registerCallback(mCallback);
                } catch (RemoteException e) {
                    // In this case the service has crashed before we could even
                    // do anything with it; we can count on soon being
                    // disconnected (and then reconnected if it can be restarted)
                    // so there is no need to do anything here.
                }
                
                // As part of the sample, tell the user what happened.
                Toast.makeText(OverviewMapOLD2.this, "Connected",
                        Toast.LENGTH_SHORT).show();
            }

            public void onServiceDisconnected(ComponentName className) {
                // This is called when the connection with the service has been
                // unexpectedly disconnected -- that is, its process crashed.
                mService = null;

                // As part of the sample, tell the user what happened.
                Toast.makeText(OverviewMapOLD2.this, "Disconnected",
                        Toast.LENGTH_SHORT).show();
            }
        };
        
     // ----------------------------------------------------------------------
        // Code showing how to deal with callbacks.
        // ----------------------------------------------------------------------
        
        /**
         * This implementation is used to receive callbacks from the remote
         * service.
         */
        private IRemoteServiceCallback mCallback = new IRemoteServiceCallback.Stub() {
            /**
             * This is called by the remote service regularly to tell us about
             * new values.  Note that IPC calls are dispatched through a thread
             * pool running in each process, so the code executing here will
             * NOT be running in our main thread like most other things -- so,
             * to update the UI, we need to use a Handler to hop over there.
             */
            public void valueChanged(String value) {
                mHandler.sendMessage(mHandler.obtainMessage(NEW_PIN, value));
            }
        };
        
        private static final int NEW_PIN = 1;
        
        private Handler mHandler = new Handler() {
            @Override public void handleMessage(Message msg) {
            	
                switch (msg.what) {
                    case NEW_PIN:                        
                        //get location from Service
                        String loc = (String)msg.obj;
                        Log.d("OverviewMap" ,"fromService:"+loc);
                        int lat = Integer.parseInt(loc.substring(0, loc.indexOf("/")));
                        int lng = Integer.parseInt(loc.substring(loc.indexOf("/")+1));
                        GeoPoint currentLocation = new GeoPoint(lat,lng);
                        
                        //add to map
                        OverlayItem pushpinLoc = new OverlayItem (currentLocation, "", "");
                        pushpinOverlay.addOverlay(pushpinLoc);
	    				mapView.getOverlays().add(pushpinOverlay);
//	    				mapView.refreshDrawableState();
	    	            mapController.setCenter(currentLocation);
                        break;
                    default:
                        super.handleMessage(msg);
                }
            }
            
        };
    	
    
    	
	protected void onPause() {
		super.onPause();
		Log.d("OverviewMap", "onPause");
		mMyLocationOverlay.disableMyLocation();
        Log.d("OverviewMap", "myLocationOverlay:" + mMyLocationOverlay.isMyLocationEnabled());     

	}
	
	@Override
	protected void onRestart() {
		super.onRestart();
		Log.d("OverviewMap","onRestart");
	}
    
    protected void onResume() {
        super.onResume();
        Log.d("OverviewMap", "onResume");
        mMyLocationOverlay.enableMyLocation();
     
        Log.d("OverviewMap", "myLocationOverlay:" + mMyLocationOverlay.isMyLocationEnabled());
        
   		startRecording = this.getIntent().getBooleanExtra("startRecording",false);
   		isRecording = this.getIntent().getBooleanExtra("isRecording",false);
	   	
	   	Log.d("OverviewMap","startRec: "+startRecording+" isRecording: "+isRecording);
	   	
	   	if (thread == null) {
	   		Log.d("OverviewMap", "thread==null");
	   	}
		if (thread != null) {
			Log.d("OverviewMap","WorkerThread isRunning: "+thread.isRunning);
		}
	   	
	   	if (startRecording) 
	   		startRecording();
//	   	else if (isRecording && thread == null)
//	   		resumeRecording();
//	   	
	   	try {
	   		exitQuestions = this.getIntent().getBooleanExtra("exitQuestions", false);
	   	}catch (Exception e) {
	   		Log.e("OverviewMap",e.toString());
	   	}
	   	Log.d("OverviewMap","exitQuestions: "+exitQuestions);
	   	
        
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	Log.d("OverviewMap","onSaveInstanceState");
    	super.onSaveInstanceState(outState);
    	Set<String> keys = outState.keySet();
    	for (Iterator i = keys.iterator(); i.hasNext();) {
    		String key = (String)i.next();
    		Log.d("OverviewMap","key:" +key);
    	}
    }
    
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
    	Log.d("OverviewMap","onRestoreInstanceState");
    	super.onRestoreInstanceState(savedInstanceState);
    	Set<String> keys = savedInstanceState.keySet();
    	for (Iterator i = keys.iterator(); i.hasNext();) {
    		String key = (String)i.next();
    		Log.d("OverviewMap","key:" +key);
    	}
    	
    }

    @Override
    protected void onStop() {
        mMyLocationOverlay.disableMyLocation();
        super.onStop();
        mapView.setEnabled(false);
        Log.d("OverviewMap","onStop");
    }
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	Log.d("OverviewMap", "onDestroy");
    }
      

	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}
	
//	public void onWindowFocusChanged(boolean hasFocus) {
//    	super.onWindowFocusChanged(hasFocus);
//    	
//    	this.hasFocus = hasFocus;
//    	Log.d("OverviewMap", "hasFocus="+hasFocus);
//    	if (hasFocus) {
//            db = (new LocationsSQLHelper(OverviewMap.this)).getWritableDatabase();
//            Log.d("OverviewMap","just created new DB");
//    	}
//    	else if (!hasFocus) {
//    		db.close();
//    	}
//    }
}