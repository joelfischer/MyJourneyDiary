package ac.uk.notts.mrl.MyJourneyDiary;

	/**
	 * @author jef@cs.nott.ac.uk
	 * Do the initialisation of the map, the actual recording is started from onResume() when the user returns from 
     * answering the questions @see Question3, @see Question3reasons or choosing to answer later @see ChooseQ.
     */

import java.util.List;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
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

public class OverviewMap extends MapActivity {
	
	private static final int JOURNEY_ID_COL = 0;

	private boolean startRecording = false;
	private boolean isRecording = false;
	 
	private int userSetDistance;
	private LinearLayout linearLayout;
	MapView mapView;
	ZoomControls mZoom;
	List<Overlay> mapOverlays;
	Drawable drawable;
	Drawable pushpin;
	PushpinOverlay pushpinOverlay;
	MyLocationOverlay mMyLocationOverlay;
	private MapController mapController;
	private SQLiteDatabase db;
	private SQLiteDatabase userdata;
	private SQLiteDatabase reminders;
	private int journeyId;
	private boolean exitQuestions = false;
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
        pushpin = this.getResources().getDrawable(R.drawable.pushpin_gif);
        pushpinOverlay = new PushpinOverlay(pushpin);
        
		mapController = mapView.getController();
		
		//display current location on map on first fix
		mMyLocationOverlay = new MyLocationOverlay(this, mapView);
        mMyLocationOverlay.runOnFirstFix(new Runnable() { public void run() {
            mapView.getController().animateTo(mMyLocationOverlay.getMyLocation());
        }});
        mapView.getOverlays().add(mMyLocationOverlay);
        mapView.getController().setZoom(15);
        mapView.setClickable(true);
        mapView.setEnabled(true);     
//        mapView.setBuiltInZoomControls(true);
    	
    	//check if the user has already set a time for a reminder, if not, let them do it! 
    	reminders = new ReminderSQLHelper(OverviewMap.this).getReadableDatabase();
    	Cursor c = reminders.rawQuery("SELECT morning_reminder_hr FROM reminder", null);
    	if (c.getCount()==0) {
    		//no reminders set yet, start activity to do so
    		startActivity(new Intent(OverviewMap.this, SetReminders.class));
    	}
    	c.close();
    	reminders.close();
	}
    	
    	private void startRecording() {
    		isRecording = true;
    		    		
			OverviewMap.this.getIntent().removeExtra("startRecording");
			
			//create db to record locations 
			if (db == null || (!db.isOpen()))
				db = (new LocationsSQLHelper(OverviewMap.this)).getWritableDatabase();
			
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
				userdata = new UserDataSQLHelper(OverviewMap.this).getWritableDatabase();
			
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

			}
			cc.close();
			userdata.close();

			//start a service that writes locations to db in background
			bindService(new Intent(IRemoteService.class.getName()).putExtra("args", journeyId+"/"+userSetDistance),
                    mConnection, Context.BIND_AUTO_CREATE);
			mIsBound = true;
    	}


		//need buttons to start, stop, show journeys, change reminders
    	public boolean onCreateOptionsMenu(android.view.Menu menu) {
    		MenuItem item = menu.add(0, 0, 0, "START recording journey");
    		MenuItem item2 = menu.add(0, 1, 1, "STOP recording journey");
    		MenuItem item3 = menu.add(0,2,2, "Show journeys");
    		MenuItem item4 = menu.add(0,2,2, "Change reminders");
    		
    		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
    			public boolean onMenuItemClick(MenuItem item) {
    				
    				if (!isRecording) {
    					//have the user provide some more info before we actually start recording...
    					startActivity(new Intent(OverviewMap.this, SettingsActivity.class));
    					return true;
    				}
    				else if (isRecording) {
    					Toast.makeText(OverviewMap.this, "You are already recording!", Toast.LENGTH_SHORT).show();
    					return false; 
    				}
    				return true;
    			}
    		});
    		item2.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				public boolean onMenuItemClick(MenuItem item) {
					startRecording = false;
					if (!isRecording) {
						Toast.makeText(OverviewMap.this, "You need to start recording first!", Toast.LENGTH_SHORT).show();
						return false;
					}
					isRecording = false;
					OverviewMap.this.getIntent().removeExtra("isRecording");
					
					if (db != null || !db.isOpen())
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
						startActivity(new Intent(OverviewMap.this, Question1E.class));
					}
					
					return true;
				}
			});
    		
    		item3.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				public boolean onMenuItemClick(MenuItem item) {
					if (isRecording) {
    					Toast.makeText(OverviewMap.this, "Please finish recording first!", Toast.LENGTH_SHORT).show();
    					return false;
					}
					else if (!isRecording) {
					startActivity(new Intent (OverviewMap.this, LocationListView.class));
					return true;
					}
					return true;
				}
			});
    		
    		item4.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				public boolean onMenuItemClick(MenuItem item) {
					if (isRecording) {
    					Toast.makeText(OverviewMap.this, "Please finish recording first!", Toast.LENGTH_SHORT).show();
    					return false;
					}
					else if (!isRecording) {
						startActivity(new Intent (OverviewMap.this, SetReminders.class));
						return true;
					}
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
//                Toast.makeText(OverviewMap.this, "Connected",
//                        Toast.LENGTH_SHORT).show();
            }

            public void onServiceDisconnected(ComponentName className) {
                // This is called when the connection with the service has been
                // unexpectedly disconnected -- that is, its process crashed.
                mService = null;

                // As part of the sample, tell the user what happened.
                Toast.makeText(OverviewMap.this, "Recording Service disconnected",
                        Toast.LENGTH_SHORT).show();
            }
        };
        
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
        
       /**
        * We need a handler to deal with callbacks from a remote service, as not running in same process.
        * The handler receives one message for each pin to be placed on the map, as determined by the remote service. 
        * @see RemoteService 
        */
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
	   	
	   	if (startRecording) 
	   		startRecording();
	   	
	   	try {
	   		exitQuestions = this.getIntent().getBooleanExtra("exitQuestions", false);
	   	}catch (Exception e) {
	   		Log.e("OverviewMap",e.toString());
	   	}
	   	Log.d("OverviewMap","exitQuestions: "+exitQuestions);    
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
	

}