package ac.uk.notts.mrl.MyJourneyDiary;
/**
 * @author jef@cs.nott.ac.uk 
 * Reads a journey selected in @see LocationListView from the db and shows it on map.
 */

import java.util.ArrayList;
import java.util.List;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.ZoomControls;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class HistoricJourneyView extends MapActivity {
	
	private LinearLayout linearLayout; 
	private MapView mapView;
	private ZoomControls mZoom;
	private List<Overlay> mapOverlays;
	private Drawable pushpin;
	private PushpinOverlay pushpinOverlay;
	private SQLiteDatabase locationData;
	private SQLiteDatabase userData;
	private MapController mapController;
	private int journeyNo;
	private int pinDistance = 250;	//default if transport = car or similar
	private String transportMode = "";
	private ProgressDialog progress;
 	
	 public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        setContentView(R.layout.main);
	        
	        linearLayout = (LinearLayout) findViewById(R.id.zoomview);
	        mapView = (MapView) findViewById(R.id.mapview);
	        mZoom = (ZoomControls) mapView.getZoomControls();
	        
	        linearLayout.addView(mZoom);
	        
	        //show user we're working on it...
//			progress = ProgressDialog.show(HistoricJourneyView.this, "", "Populating map...", true, false);
	        progress = new ProgressDialog(this);
			progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progress.setMessage("Populating map...");
			progress.setCancelable(false);
			progress.show();
			//spawn and AsyncThread to do the work
			new WorkerThread().execute();
	        
	 }
	 
	 private Handler handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				progress.dismiss();
				Log.d("HistoricJourneyView", "cancelled dialog");
			}
		};	 
	 
	 public boolean onCreateOptionsMenu(android.view.Menu menu) {
 		MenuItem item = menu.add(0, 0, 0, "Show details");
 		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
 			public boolean onMenuItemClick(MenuItem item) {
 				
 				startActivity(new Intent(HistoricJourneyView.this, StatsActivity.class).putExtra("journeyNo", journeyNo));
				
 				return true;
 				
 			}
 		});
 		return true;
     }
	 
	 class WorkerThread extends AsyncTask <Void, Integer, Integer> {

     	WorkerThread() {}

			protected Integer doInBackground(Void... params) {
				 //get overlays
		        mapOverlays = mapView.getOverlays();
		        pushpin = HistoricJourneyView.this.getResources().getDrawable(R.drawable.pushpin_gif);
		        
		        pushpinOverlay = new PushpinOverlay(pushpin);
		        
		        Bundle extras = HistoricJourneyView.this.getIntent().getExtras();
		        journeyNo = extras.getInt("journeyNo");
		        Log.d("HistoricJourneyView","journeyNo:"+journeyNo);
		        
		        mapController = mapView.getController();
		        
		        locationData = new LocationsSQLHelper(HistoricJourneyView.this).getReadableDatabase();
		        userData = new UserDataSQLHelper(HistoricJourneyView.this).getReadableDatabase();
		        Cursor c = locationData.rawQuery("SELECT lat,long,distance FROM locations " +
		        		"WHERE journeyId="+journeyNo , null);
		        Cursor cu = userData.rawQuery("SELECT transport FROM userdata " +
		        		"WHERE journeyId ="+journeyNo, null);
		        
		        //determine pinDistance by transport mode
		        if (cu.getCount() > 0) {
		        	cu.moveToFirst();
		        	try {
		        		transportMode = cu.getString(0);
		        	} catch (NullPointerException npe) {
		        		Log.e("HistoricJourneyView", npe.toString());
		        	}
		        }
		        cu.close();
		        userData.close();
		        
		        if (!transportMode.equals("")) {
		        	if (transportMode.equals("walking")) 
		        		pinDistance = 50;
		        	if (transportMode.equals("cycling") || transportMode.equals("other"))
		        		pinDistance = 100;
		        }
//		        Log.d("HistoricJourneyView","pinDistance: "+pinDistance);
		        
		        
		        ArrayList<GeoPoint> waypoints = new ArrayList<GeoPoint>();
		        if (c.getCount()!=0) {
		        	int lat = 0;
		        	int lng = 0;
		        	double distanceToLastPin = 0.0;
		        	double distance = 0.0;
		        	c.moveToFirst();
		        	try {
		        		lat = c.getInt(0);
		        		lng = c.getInt(1);
		        	} catch (NullPointerException npe) {
		        		Log.e("HistoricJourneyView", npe.toString());	
		        	}
		        	GeoPoint start = new GeoPoint(lat,lng);
		        	waypoints.add(start);
		        	OverlayItem startPin = new OverlayItem(start,"","");
		        	pushpinOverlay.addOverlay(startPin);
		        	
		        	int i = 0;
		        	while(c.moveToNext()) {
		        		i++;
		        		try {
			        		lat = c.getInt(0);
			        		lng = c.getInt(1);
			        		distance = c.getDouble(2);
			        	} catch (NullPointerException npe) {
			        		Log.e("HistoricJourneyView", npe.toString());	
			        	}
			        	distanceToLastPin += distance; 
//			        	Log.d("HistoricJourneyView","distanceToLastPin: "+distanceToLastPin);
			        	
			        	if (distanceToLastPin >= pinDistance) {
			        		distanceToLastPin = 0.0;
//			        		Log.d("HistoricJourneyView","distanceToLastPin reset: "+distanceToLastPin);
			        		GeoPoint point = new GeoPoint(lat,lng);
				        	waypoints.add(point);
				        	OverlayItem pin = new OverlayItem(point,"","");
				        	pushpinOverlay.addOverlay(pin);
//				        	Log.d("HistoricJourneyView","pin set");
			        	}
			        	publishProgress((int) ((i / (float) c.getCount()) * 100));
		        	}
		        	mapOverlays.add(pushpinOverlay);
		        	GeoPoint center = waypoints.get(0);
		        	mapController.setCenter(center);
		        	mapView.getController().setZoom(14);
		        	
		        }
		        c.close();
		        locationData.close();
		        handler.sendEmptyMessage(0);
//		        progress.cancel();
		        
				return RESULT_OK;
			}
			
			protected void onProgressUpdate (Integer... progress) {
//				Log.d("HistoricJourneyView", "progress:"+progress[0]);
				HistoricJourneyView.this.progress.setProgress(progress[0]);
			}
			
			protected void onPostExecute(Integer... result) {
				Log.d("HistoricJourneyView","result: "+result[0]);
				if (result[0] == RESULT_OK && HistoricJourneyView.this.progress != null) 
					HistoricJourneyView.this.progress.dismiss();
			}
     	
     }

	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}

}
