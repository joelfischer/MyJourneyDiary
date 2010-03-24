package ac.uk.notts.mrl.MyJourneyDiary;
/**
 * @author jef@cs.nott.ac.uk
 * This is for testing purposes only and not used anywhere else currently.
 */

import ac.uk.notts.mrl.MyJourneyDiary.R;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ZoomControls;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;

public class CurrentLocation extends MapActivity {
	
	private MapController mapController;
	private MapView mapView;
	private LocationManager locMgr;
	private LinearLayout linearLayout;
	private ZoomControls mZoom;
	
	
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle); 
		setContentView(R.layout.alt);
		
		linearLayout = (LinearLayout) findViewById(R.id.zoomview);
        mapView = (MapView) findViewById(R.id.mapview);
        mZoom = (ZoomControls) mapView.getZoomControls();
        
        linearLayout.addView(mZoom);
        
        mapController = mapView.getController();
        
        locMgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    	LocationListener locListener = new LocationListener() {
    		public void onLocationChanged(Location location) {
    			if (location != null) {
//    				Toast.makeText(
//    						getBaseContext(),
//    						"New location latitude [" + location.getLatitude()
//    								+ "] longitude [" + location.getLongitude()
//    								+ "]", Toast.LENGTH_SHORT).show();
    				int lat = (int) (location.getLatitude() * 1E6);
    				int lng = (int) (location.getLongitude() * 1E6);
    				GeoPoint point = new GeoPoint(lat, lng);
    				mapController.setCenter(point);
    				
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

    		
    	};
    	locMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0,
    			locListener);
    	
    	Location loc = locMgr.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    	if (loc != null) {
    		Log.d("OverviewMap", "currentLocation lat:"+loc.getLatitude()+", long:" + loc.getLongitude());
    	}
	}
	
	
	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}

}
