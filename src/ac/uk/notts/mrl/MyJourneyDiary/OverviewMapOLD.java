package ac.uk.notts.mrl.MyJourneyDiary;
/**
 * @author jef@cs.nott.ac.uk
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import ac.uk.notts.mrl.MyJourneyDiary.R;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
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

public class OverviewMapOLD extends MapActivity {
	
	private static final double DEGREES_TO_RADIANS = (double)(180/Math.PI); 
	private static final int EARTH_RADIUS = 6378140;
	private static final int DISTANCE_BETWEEN_PINS = 20;	//in metres
	private static final int JOURNEY_ID_COL = 0;
	private static final long LISTENER_INTERVAL = 1000*3; //in milliseconds
	
	private boolean emulator = true;
	private boolean startRecording = false;
	 
	private int userSetDistance;
	private String purpose = "";
	private String transport = "";
	private String comment = "";
	private String q1 = "";
	private String q2y="";
	private String q2n="";
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
	private BufferedWriter bufferedWriter;
	private MyLocationListener locListener;
	private MapController mapController;
	private PowerManager pm;
	private WakeLock stayAwake;
	private SQLiteDatabase db = null;
	private SQLiteDatabase userdata;
	private SQLiteDatabase reminders;
	private boolean hasFocus = false;
	private int journeyId;
	private boolean exitQuestions = false;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        linearLayout = (LinearLayout) findViewById(R.id.zoomview);
        mapView = (MapView) findViewById(R.id.mapview);
        mZoom = (ZoomControls) mapView.getZoomControls();
        
        linearLayout.addView(mZoom);
        
        //get overlays
        mapOverlays = mapView.getOverlays();
        drawable = this.getResources().getDrawable(R.drawable.androidmarker);
        pushpin = this.getResources().getDrawable(R.drawable.pushpin_gif);
        
        itemizedOverlay = new HelloItemizedOverlay(drawable);
        pushpinOverlay = new PushpinOverlay(pushpin);
        
        //some test locations to debug
        GeoPoint point = new GeoPoint(19240000,-99120000);
        OverlayItem overlayitem = new OverlayItem(point, "", "");
        
        GeoPoint point2 = new GeoPoint(52957499, -1196373);
        OverlayItem overlayitem2 = new OverlayItem(point2, "", "");
        
        
//        itemizedOverlay.addOverlay(overlayitem);
//        itemizedOverlay.addOverlay(overlayitem2);
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
        mapView.getController().setZoom(18);
        mapView.setClickable(true);
        mapView.setEnabled(true);
        
        
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
    	reminders = new ReminderSQLHelper(OverviewMapOLD.this).getReadableDatabase();
    	Cursor c = reminders.rawQuery("SELECT morning_reminder_hr FROM reminder", null);
    	if (c.getCount()==0) {
    		//no reminders set yet, start activity to do so
    		startActivity(new Intent(OverviewMapOLD.this, SetReminders.class));
    	}
    	
    	
	}
    
   	public class MyLocationListener implements LocationListener {
		public void onLocationChanged(Location location) {
    		Log.d("OverviewMap","instantiatingLocationListener");
			if (location != null) {
				
				double lat = (double) (location.getLatitude() );
				double lng = (double) (location.getLongitude() );
				int latCorr = (int) (location.getLatitude() * 1E6);
				int lngCorr = (int) (location.getLongitude() * 1E6);
				GeoPoint currentLocation = new GeoPoint(latCorr, lngCorr);
				OverlayItem pushpinLoc = new OverlayItem(currentLocation, "", "");
				
				//only add an overlay if the distance to the last one is at least 10m
				List<Overlay> pinList = mapView.getOverlays();
				if (pinList.size()>0) {
					Log.d("OverviewMap", "pinList.size:"+pinList.size());
					//check if we have a PushpinOverlay
					if (pinList.contains(pushpinOverlay)) {
						//OK, get last added overlay
						Log.d("OverviewMap","pushpinOverlay detected");
					PushpinOverlay lastAddedOverlay = (PushpinOverlay)pinList.get(pinList.lastIndexOf(pushpinOverlay));
    					if (lastAddedOverlay.size()>0) {
    						//get last added item
    						OverlayItem lastPushpin = (OverlayItem)lastAddedOverlay.getItem(lastAddedOverlay.size()-1);
    						GeoPoint lastLocation = lastPushpin.getPoint();
    						
    						//get distance between current and last location 
    						double distance = calculateDistance((double)lastLocation.getLatitudeE6()/1E6, (double)lastLocation.getLongitudeE6()/1E6, 
    								location.getLatitude(), location.getLongitude()); 
    						Log.d("OverviewMap", "locations:"+(double)lastLocation.getLatitudeE6()/1E6+","+ (double)lastLocation.getLongitudeE6()/1E6+", "+ 
    								location.getLatitude()+","+location.getLongitude());
    						Log.d("OverviewMap","Distance to last added pin:" +distance +" m");
    						
    						if (distance > userSetDistance) {
    							pushpinOverlay.addOverlay(pushpinLoc);
			    				mapView.getOverlays().add(pushpinOverlay);
			    				mapView.refreshDrawableState();
			    	            mapController.setCenter(currentLocation);
			    				Log.d("OverviewMap", "distance big enough: "+userSetDistance+", just added new pushpin");
//			    				Toast.makeText(
//			    						getBaseContext(),
//			    						"latitude [" + location.getLatitude()
//			    								+ "] longitude [" + location.getLongitude()
//			    								+ "]", Toast.LENGTH_SHORT).show();
			    				//save for later
			    				db.execSQL("INSERT INTO locations (journeyId,lat,long,distance,timestamp) VALUES ("+journeyId+","+latCorr+","+lngCorr+
			    						","+distance+","+System.currentTimeMillis()+")");
			    				
			    				if (!emulator) {
			    				//also write to kml outfile
				    				try {
										bufferedWriter.write(""+lng + "," +""+lat +",50 \n");	//displays altitude of line at 50
									} catch (IOException e) {
										Log.e(getClass().getSimpleName(), e.toString());
									}
			    				}
    						}
    						
    					}
					}
					else {
						//no pushpinOverlay in Overlays yet, add first one:
						pushpinOverlay.addOverlay(pushpinLoc);
	    				mapView.getOverlays().add(pushpinOverlay);
	    				mapView.refreshDrawableState();
	    				Log.d("OverviewMap","added FIRST pushpin");
	    				
	    				if (!emulator) {
		    				try {
			    				bufferedWriter.write("<Placemark> \n"
			    						+ "<name>Start</name>"
			    						+ "<Point><coordinates>"+lng+ "," +""+lat +",50</coordinates></Point> \n"
			    						+ "</Placemark> \n" 
			    						+ "<Placemark> \n " 
										+ "<name>waypoint</name> \n "
										+ "<description>a point on the way</description> \n"
										+ "<styleUrl>#yellowLineGreenPoly</styleUrl> \n" 
										+ "<LineString> \n"
										+ "<extrude>1</extrude> \n "
										+ "<tessellate>1</tessellate> \n"
										+ "<altitudeMode>absolute</altitudeMode> \n"
										+ "<coordinates> \n");
		    				}catch (IOException e) {
		    					Log.e("OverviewMap", e.toString());
		    				}
	    				}
	    				
	    				
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
			// TODO Auto-generated method stub
			
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
    	
    	private void startRecording() {
    		stayAwake = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "");
			stayAwake.acquire();
			OverviewMapOLD.this.getIntent().removeExtra("startRecording");
			
			//create db to record locations 
			if (db == null || (!db.isOpen()))
				db = (new LocationsSQLHelper(OverviewMapOLD.this)).getWritableDatabase();
			
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
	            c.close();
			}
			else {
				//first row
				journeyId = 1;
				Log.d("OverviewMap", "FIRST JourneyId:"+journeyId);
			}
			if (userdata == null || (!userdata.isOpen()))
				userdata = new UserDataSQLHelper(OverviewMapOLD.this).getReadableDatabase();
			
			userdata.execSQL("UPDATE userdata SET journeyId="+journeyId+" WHERE id=(SELECT MAX(id) FROM userdata)");
			
			Cursor cc = userdata.rawQuery("SELECT purpose,transport,beforeJourney,q1,q2y,q2n,comment FROM userdata WHERE id=(SELECT MAX(id) FROM userdata)", null);
			Log.d("OverviewMap","cc: "+cc.getCount());
			if (cc.getCount()!=0) {
				cc.moveToFirst();
				String transport = cc.getString(1);
				if (transport.equals("walking")) {
					this.userSetDistance = 20;
				}
				if ((transport.equals("cycling"))||(transport.equals("--"))) {
					this.userSetDistance = 75;
				}
				if ((transport.equals("bus"))||(transport.equals("car"))||(transport.equals("train"))||(transport.equals("tram"))||(transport.equals("other"))) {
					this.userSetDistance = 250;
				}
				Log.d("OverviewMap","transport: "+transport);
				Log.d("OverviewMap:Distance: ",""+userSetDistance);
				if (transport != null) {
					OverviewMapOLD.this.transport = transport;
				}
				String purpose = cc.getString(0);
				if (!purpose.equals(""))
					this.purpose = purpose;
				
				int beforeJourney = cc.getInt(2);
				
				if (beforeJourney == 1) {
					
					String q1 = cc.getString(3);
					if (!q1.equals("")) 
						this.q1 = q1;
					String q2y = cc.getString(4);
					if (!q2y.equals(""))
						this.q2y = q2y;
					String q2n = cc.getString(5);
					if (!q2n.equals(""))
						this.q2n = q2n;
					String prelComment = "";
					try {
						prelComment = cc.getString(6);
					} catch (NullPointerException npe) {
						Log.e("OverviewMap","NPE: "+npe.toString());
					}
					if (prelComment != null) {
						if (!prelComment.equals(""))
							this.comment = prelComment;
					}
				}
			}
			cc.close();
			userdata.close();
           
			if (!emulator) {
				//prepare writing kml file to sd card
		        File sdCard = Environment.getExternalStorageDirectory();
				File store = new File("/sdcard/LocationDiary");
				if (!store.isDirectory())
				{
					store.mkdir();
					Log.d(getClass().getSimpleName(), "just made dir");
				}
				Log.d(getClass().getSimpleName(), "sdCard:"+sdCard.toString());
				File file = new File(store, ""+System.currentTimeMillis()+".kml");
				if (!file.isFile())
				{
					try {
						file.createNewFile();
					} catch (IOException e) {
						Log.e(getClass().getSimpleName(), e.toString());
						Log.d(getClass().getSimpleName(), "just made file");
					}
				}
				try {
					 bufferedWriter = new BufferedWriter(new FileWriter(file));
				} catch (IOException e) {
					Log.e(getClass().getSimpleName(), e.toString());
				}
				try {
					bufferedWriter.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n" 
							+ "<kml xmlns=\"http://www.opengis.net/kml/2.2\"> \n" 
							+ "<Document> \n" 
							+ "<name>Journey "+journeyId+"</name>\n"
							+ "<description>Mode of travel: "+transport+".\n" 
							+ "Purpose: "+purpose+". \n"
							+ "</description> \n" 
							+ "<Style id=\"yellowLineGreenPoly\"> \n"
							+ "<LineStyle>\n"
							+ "<color>7f00ffff></color> \n"
							+ "<width>3</width> \n"
							+ "</LineStyle> \n"
							+ "<PolyStyle>\n"
							+ "<color>7f00ff00</color> \n"
							+ "</PolyStyle>\n"
							+ "</Style> \n"
							);
		    	} catch (IOException e) {
		    		Log.e(getClass().getSimpleName(), e.toString());
		    	}
			}
			
			locMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, LISTENER_INTERVAL, userSetDistance,	
	    			locListener = new MyLocationListener() );
    	}


		//need a button to start stop
    	public boolean onCreateOptionsMenu(android.view.Menu menu) {
    		MenuItem item = menu.add(0, 0, 0, "START recording journey");
    		MenuItem item2 = menu.add(0, 1, 1, "STOP recording journey");
    		MenuItem item3 = menu.add(0,2,2, "Show journeys");
    		MenuItem item4 = menu.add(0,2,2, "Change reminders");
    		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
    			public boolean onMenuItemClick(MenuItem item) {
    				
    				startActivity(new Intent(OverviewMapOLD.this, SettingsActivity.class));
    			
    				return true;
    			}
    		});
    		item2.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				public boolean onMenuItemClick(MenuItem item) {
					startRecording = false;
					try {
						locMgr.removeUpdates(locListener);
					} catch (IllegalArgumentException e) {
						Log.e("OveviewMap",e.toString());
						Toast.makeText(OverviewMapOLD.this, "You need to start recording first!", Toast.LENGTH_SHORT).show();
						return false;
					}
					try {
						stayAwake.release();
					}catch (NullPointerException npe) {
						Log.e("OverviewMap",npe.toString());
						Toast.makeText(OverviewMapOLD.this, "You need to start recording first!", Toast.LENGTH_SHORT).show();
						return false;
					}catch (RuntimeException e) {
						Log.e("OverviewMap",e.toString());
					}
					db.close();
					if (!emulator) {
						try {
							bufferedWriter.write("</coordinates> \n" 
									+ "</LineString> \n"
									+ "</Placemark> \n"
									+ "</Document> \n "
									+ "</kml>");
							bufferedWriter.close();
						} catch (IOException e) {
							Log.e(getClass().getSimpleName(), e.toString());
						}
					}
					Toast.makeText(OverviewMapOLD.this, "Successfully recorded journey", Toast.LENGTH_SHORT).show();
					
					if (exitQuestions) {
						exitQuestions = false;
						startActivity(new Intent(OverviewMapOLD.this, Question1E.class));
					}
					
					return true;
				}
			});
    		
    		item3.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				public boolean onMenuItemClick(MenuItem item) {
					startActivity(new Intent (OverviewMapOLD.this, LocationListView.class));
					return true;
				}
			});
    		
    		item4.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				public boolean onMenuItemClick(MenuItem item) {
					startActivity(new Intent (OverviewMapOLD.this, SetReminders.class));
					return true;
				}
			});
    		
    		return true;
    	}
    	
	protected void onPause() {
		super.onPause();
		Log.d("OverviewMap", "onPause");
		mMyLocationOverlay.disableMyLocation();
        Log.d("OverviewMap", "myLocationOverlay:" + mMyLocationOverlay.isMyLocationEnabled());

	}
    
    protected void onResume() {
        super.onResume();
        Log.d("OverviewMap", "onResume");
        mMyLocationOverlay.enableMyLocation();
     
        Log.d("OverviewMap", "myLocationOverlay:" + mMyLocationOverlay.isMyLocationEnabled());
        
        try {
   		 startRecording = this.getIntent().getBooleanExtra("startRecording",false);
	   	} catch (Exception e) {
	   		Log.e("OverviewMap", "startRecording from Intent: "+e.toString());
	   	}
	   	Log.d("OverviewMap","startRec: "+startRecording);
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