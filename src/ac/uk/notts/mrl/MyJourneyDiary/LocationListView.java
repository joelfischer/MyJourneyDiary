package ac.uk.notts.mrl.MyJourneyDiary;
/**
 * @author jef@cs.nott.ac.uk
 * Displays all recorded journeys in a list, can be exported through the context menu or shown on map.
 */

import java.util.ArrayList;

import ac.uk.notts.mrl.MyJourneyDiary.HistoricJourneyView.WorkerThread;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class LocationListView extends ListActivity {
		
	private SQLiteDatabase db;
	private SQLiteDatabase userdata;
	private ProgressDialog activeProgress;
	private int journeyNumber = 0;
	private ProgressDialog progress;

	protected void onCreate(Bundle savedInstanceState) {
			
			super.onCreate(savedInstanceState);
			
			db = (new LocationsSQLHelper(LocationListView.this)).getReadableDatabase();
			userdata = new UserDataSQLHelper(LocationListView.this).getReadableDatabase();
			
			//
			//show user we're working on it...
//			progress = ProgressDialog.show(LocationListView.this, "", "Loading journeys...", true, false);
			progress = new ProgressDialog(this);
			progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progress.setMessage("Loading journeys...");
			progress.setCancelable(false);
			progress.show();
			//spawn and AsyncThread to do the work
			new WorkerThread().execute();
			
			getListView().setTextFilterEnabled(true);
			registerForContextMenu(getListView());				
	}
	

	 private Handler handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				progress.dismiss();
//				Log.d("LocationListView", "cancelled dialog");
			}
		};	 
		
		class WorkerThread extends AsyncTask <Void, Integer, String[]> {

			protected String[] doInBackground(Void... params) {
				String[] journeys = null;
				Cursor c = db.rawQuery("SELECT journeyId,time FROM locations", null);
				int journeyId = 0;
				int journeyCounter = 0;
				int tentJourneyId = 0;
				String tentDate = null;
				String date = null;
				String transport = "";
				ArrayList <String> journeyDates = new ArrayList<String>();
				if (c.getCount()>0) {
					c.moveToFirst();
					try {
						journeyId = c.getInt(0);
						date = c.getString(1);
						journeyDates.add(date);
						journeyCounter++;
					} catch (NullPointerException npe) {
						Log.e("LocationListView", npe.toString());
					}
					while (c.moveToNext()) {
						try {
							tentJourneyId = c.getInt(0);
							tentDate = c.getString(1);
						} catch (NullPointerException npe) {
							Log.e("LocationListView", npe.toString());
						}
						if (tentJourneyId>journeyId) {
							journeyId = tentJourneyId;
							journeyCounter++;
							date = tentDate;
							journeyDates.add(date);
						}
					}
					c.close();
					db.close();
					
//					ArrayList <String> transports = new ArrayList<String>();
//					c = userdata.rawQuery("SELECT transport FROM userdata", null);
//					if (c.getCount()>0) {
//						c.moveToFirst();
//						transport = c.getString(0);
//						transports.add(transport);
//						while (c.moveToNext()) {
//							transport = c.getString(0);
//							transports.add(transport);
//						}
//					}
//					c.close();
					
					
					//debug
//						Log.d("LocationListView", "journeyDates: "+journeyDates.size()+", transports: "+transports.size()+ ", journeyCounter: "+journeyCounter);
					
					ArrayList<String> journeysList = new ArrayList<String>();
					for (int i = 1; i <= journeyCounter; i++) {
						String transportNew = "";
						c = userdata.rawQuery("SELECT transport FROM userdata WHERE journeyId="+i, null);
						if (c.getCount()>0) {
							//in case we have rows in userdata not pertaining to a recorded journey (e.g. because no GPS signal and user 
							//tries recording it again), we need to go to the last row.
							c.moveToLast();
							transportNew = c.getString(0);
						}
						journeysList.add("Journey "+i +", " + journeyDates.get(i-1)+
								", "+transportNew);	
						publishProgress((int) ((i / (float) journeyCounter) * 100));
					}
					journeys = (String[]) journeysList.toArray(new String[journeysList.size()]);	
					c.close();
					userdata.close();
					
					
				}
				
				else {
					journeys = new String [] {
							
							"You have no journeys."
					};
				}
				
				return journeys;
			}
			
			protected void onProgressUpdate (Integer... progress) {
//				Log.d("LocationListView", "progress:"+progress[0]);
				LocationListView.this.progress.setProgress(progress[0]);
			}
			
			protected void onPostExecute(String[] result) {
				handler.sendEmptyMessage(0);
				Log.d("LocationListView","result: "+result[0]);
				if (LocationListView.this.progress != null) 
					LocationListView.this.progress.dismiss();
				
				LocationListView.this.setListAdapter(new ArrayAdapter<String>(LocationListView.this, android.R.layout.simple_list_item_1, result));	
			}		
		}
		
	

	protected void onListItemClick (ListView l, View v, int position, long id) {
				
		Object listItem = l.getItemAtPosition(position);
		
		Log.d("LocationListView", ""+listItem.toString());
		
		String journeyNo = listItem.toString().replaceAll("Journey ", "");
		journeyNo = journeyNo.substring(0,journeyNo.indexOf(","));
		journeyNumber = Integer.parseInt(journeyNo);	
		
		startActivity(new Intent (LocationListView.this, HistoricJourneyView.class).putExtra("journeyNo", journeyNumber));

	}
	

	
	public void onCreateContextMenu (ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		MenuItem showItem = menu.add(0, 0, 0, "Show on map");
		MenuItem exportItem = menu.add(0, 0, 0, "Info and export");
		MenuItem cancelItem = menu.add(0, 0, 0, "X Cancel");
		Log.d("LocationListView", "ContextMenuInfo: "+menuInfo.toString());
		AdapterView.AdapterContextMenuInfo adMenuInfo = (AdapterView.AdapterContextMenuInfo)menuInfo;
		TextView target = (TextView)adMenuInfo.targetView;
		String content = (String)target.getText();
		long id = adMenuInfo.id;
		int pos = adMenuInfo.position;
		Log.d("LocationListView", "AdapterContextMenuInfo. ID: "+id + ", targetView: "+target.toString() +", pos: "+pos +" content: "+content);
		
		showItem.setTitle("Show "+(content.subSequence(0, content.indexOf(",")))+" on map");
		exportItem.setTitle("Info and export "+(content.subSequence(0, content.indexOf(","))));
		
		showItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
 			public boolean onMenuItemClick(MenuItem item) {
 				
 				
 				String journey = (String)item.getTitle();
 				journey = journey.replaceAll("Show Journey ", "");
 				journey = journey.replaceAll(" on map", "");
				journeyNumber = Integer.parseInt(journey);
 				Log.d("LocationListView", "journey: "+journey +" journeyNo:"+journeyNumber+"X");
 				
 				startActivity(new Intent (LocationListView.this, HistoricJourneyView.class).putExtra("journeyNo", journeyNumber));
				
 				return true;
 				
 			}
 		});
		
		exportItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			
			public boolean onMenuItemClick(MenuItem item) {
				String journey = (String)item.getTitle();
 				journeyNumber = Integer.parseInt(journey.replaceAll("Info and export Journey ", ""));
 				Log.d("LocationListView", "journey: "+journey +" journeyNo:"+journeyNumber+"X");
 				
 				startActivity(new Intent (LocationListView.this, StatsActivity.class).putExtra("journeyNo", journeyNumber));
 				
				return true;
			}
		});
		
		cancelItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {

			public boolean onMenuItemClick(MenuItem item) {
				
				return false;
			}
		});
		
	}
}
