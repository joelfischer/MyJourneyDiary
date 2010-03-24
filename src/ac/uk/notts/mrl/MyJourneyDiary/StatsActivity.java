package ac.uk.notts.mrl.MyJourneyDiary;
/**
 * @author jef@cs.nott.ac.uk 
 * Show stats of journey and export as .kml and .csv to sd card.
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class StatsActivity extends Activity {
	
	protected SQLiteDatabase db;
	private SQLiteDatabase userdata;
	int journeyNo;
	double totalDistance = 0.0;
	String distance ="";
	String startDate = "";
	String endDate = "";
	long startTime = 0;
	long endTime = 0;
	long travelTime = 0;
	String purpose = "";
	String transport ="";
	int beforeJourney = -1;
	String q1 = "";
	String q2y = "";
	String q2n = "";
	String comment = "";
	String beforeJourneyStr = "";
	private boolean emulator = false;
	private BufferedWriter bufferedWriter;
	private BufferedReader bufferedReader;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.stats);
		
		//get journeyNo for this stats page from starting intent (HistoricJourneyView)
		journeyNo = StatsActivity.this.getIntent().getExtras().getInt("journeyNo");
		
		//get db
		db = new LocationsSQLHelper(StatsActivity.this).getReadableDatabase();
		Cursor c = db.rawQuery("SELECT distance,time,timestamp FROM locations WHERE journeyId="+journeyNo, null);
		
		//read db
		if (c.getCount()!=0) {
			
			//compute distance...inaccurate at the moment because of inaccurate section lengths so we don't show user
			while (c.moveToNext()) {
				try {
					double lastSection = c.getDouble(0);				
					Log.d("StatsActivity", " TotalSoFar: "+totalDistance+" + lastDistance: "+lastSection);
					totalDistance = totalDistance+lastSection;
					Log.d("StatsActivity", " = "+totalDistance);
				}catch (NullPointerException npe) {
					Log.e("StatsActivity",npe.toString());
				}
			}
			
			c.moveToFirst();
			try {
				startDate = c.getString(1);				
			}catch (NullPointerException npe) {
				Log.e("StatsActivity",npe.toString());
			}
			try {
				startTime = c.getLong(2);				
			}catch (NullPointerException npe) {
				Log.e("StatsActivity",npe.toString());
			}
			c.moveToLast();
			try {
				endDate = c.getString(1);				
			}catch (NullPointerException npe) {
				Log.e("StatsActivity",npe.toString());
			}
			try {
				endTime = c.getLong(2);				
			}catch (NullPointerException npe) {
				Log.e("StatsActivity",npe.toString());
			}
		}
		c.close();
		db.close();
		
		travelTime = (endTime - startTime)/1000/60; 
		
		userdata = new UserDataSQLHelper(StatsActivity.this).getReadableDatabase();
		Cursor c2 = userdata.rawQuery("SELECT purpose,transport,beforeJourney,q1,q2y,q2n,comment FROM userdata WHERE journeyId="+journeyNo, null);
		if (c2.getCount()>0) {
//			Log.d("StatsActivity", "c.getCount(): " +c2.getCount());
			//in case we have rows in userdata not pertaining to a recorded journey (e.g. because no GPS signal and user 
			//tries recording it again), we need to go to the last row. 
			c2.moveToLast();
			try {
				purpose = c2.getString(0);
			} catch (NullPointerException npe) {
				Log.e("StatsActivity", "no purpose found: "+npe.toString());
			}
			try {
				transport = c2.getString(1);
			} catch (NullPointerException npe) {
				Log.e("StatsActivity", "no transport found: "+npe.toString());
			}
			try {
				beforeJourney = c2.getInt(2);
			} catch (NullPointerException npe) {
				Log.e("StatsActivity", "no beforeJourney found: "+npe.toString());
			}
			try {
				q1 = c2.getString(3);
			} catch (NullPointerException npe) {
				Log.e("StatsActivity", "no q1 found: "+npe.toString());
			}
			try {
				q2y = c2.getString(4);
			} catch (NullPointerException npe) {
				Log.e("StatsActivity", "no q2y found: "+npe.toString());
			}
			try {
				q2n = c2.getString(5);
			} catch (NullPointerException npe) {
				Log.e("StatsActivity", "no q2n found: "+npe.toString());
			}
			try {
				comment = c2.getString(6);
			} catch (NullPointerException npe) {
				Log.e("StatsActivity", "no comment found: "+npe.toString());
			}
		}
		c2.close();
		userdata.close();
		if (q2n == null)
			q2n = "";
		
		Button expButton = (Button) findViewById(R.id.export_button);
		TableLayout tl = (TableLayout)findViewById(R.id.stats_table);
		
		//construct rows and cells
		TableRow tr1 = new TableRow(StatsActivity.this);
		TextView tv1 = new TextView(StatsActivity.this);
		tv1.setText("Overview of journey "+journeyNo);
		tv1.setTextSize(16);
		tr1.addView(tv1);
		
		TableRow tr2 = new TableRow(StatsActivity.this);
		TextView tv2 = new TextView(StatsActivity.this);
		tv2.setText("Start of journey: ");
		tr2.addView(tv2);
		TextView tv3 = new TextView(StatsActivity.this);
		tv3.setText(startDate);
		tr2.addView(tv3);
		
		TableRow tr3 = new TableRow(StatsActivity.this);
		TextView tv4 = new TextView(StatsActivity.this);
		tv4.setText("End of journey: ");
		tr3.addView(tv4);
		TextView tv5 = new TextView(StatsActivity.this);
		tv5.setText(endDate);
		tr3.addView(tv5);
		
		TableRow tr7 = new TableRow(StatsActivity.this);
		TextView tv12 = new TextView(StatsActivity.this);
		tv12.setText("Time travelled: ");
		tr7.addView(tv12);
		TextView tv13 = new TextView(StatsActivity.this);
		tv13.setText(""+travelTime+" mins");
		tr7.addView(tv13);
		
		TableRow tr4 = new TableRow(StatsActivity.this);
		TextView tv6 = new TextView(StatsActivity.this);
		tv6.setText("Distance travelled: ");
		tr4.addView(tv6);
		TextView tv7 = new TextView(StatsActivity.this);
		distance = ""+totalDistance;
		if (distance.contains(".")) {
			distance = distance.substring(0, distance.indexOf(".")+2);
		}
		tv7.setText(distance +" m");
		if (totalDistance > 1000) {
			totalDistance = totalDistance/1000;
			String KmDistance = (""+totalDistance);
			if (KmDistance.contains(".")) {
				KmDistance = KmDistance.substring(0, KmDistance.indexOf(".")+3);
			}
			tv7.setText(KmDistance +" km");
		}
		tr4.addView(tv7);
		
		TableRow tr5 = new TableRow(StatsActivity.this);
		TextView tv8 = new TextView(StatsActivity.this);
		tv8.setText("Transportation mode: ");
		tr5.addView(tv8);
		TextView tv9 = new TextView(StatsActivity.this);
		tv9.setText(transport);
		tr5.addView(tv9);
		
		TableRow tr6 = new TableRow(StatsActivity.this);
		TextView tv10 = new TextView(StatsActivity.this);
		tv10.setText("Purpose: ");
		tr6.addView(tv10);
		TextView tv11 = new TextView(StatsActivity.this);
		tv11.setText(purpose);
		tr6.addView(tv11);
		
		TableRow tr8 = new TableRow(StatsActivity.this);
		TextView tv14 = new TextView(StatsActivity.this);
		tv14.setText("Questions answered \n before journey: ");
		tr8.addView(tv14);
		TextView tv15 = new TextView(StatsActivity.this);
		if (beforeJourney==1) {
			tv15.setText("yes");
			beforeJourneyStr = "yes";
		}
		else if (beforeJourney==0) {
			tv15.setText("no");
			beforeJourneyStr = "no";
		}
		tr8.addView(tv15);
		
		TableRow tr9 = new TableRow(StatsActivity.this);
		TextView tv16 = new TextView(StatsActivity.this);
		tv16.setText("Shared this journey? ");
		tr9.addView(tv16);
		TextView tv17 = new TextView(StatsActivity.this);
		tv17.setText(q1);
		tr9.addView(tv17);
		
		TableRow tr10 = new TableRow(StatsActivity.this);
		TextView tv18 = new TextView(StatsActivity.this);
		tv18.setText("Would have shared \n this journey? ");
		tr10.addView(tv18);
		TextView tv19 = new TextView(StatsActivity.this);
		tv19.setText(q2n);
		tr10.addView(tv19);
		
		TableRow tr11 = new TableRow(StatsActivity.this);
		TextView tv20 = new TextView(StatsActivity.this);
		tv20.setText("Role: ");
		tr11.addView(tv20);
		TextView tv21 = new TextView(StatsActivity.this);
		tv21.setText(q2y);
		tr11.addView(tv21);
		
		TableRow tr12 = new TableRow(StatsActivity.this);
		TextView tv22 = new TextView(StatsActivity.this);
		tv22.setText("Comment: ");
		tr12.addView(tv22);
		TextView tv23 = new TextView(StatsActivity.this);
		tv23.setText(comment);
		tr12.addView(tv23);
		
		//seperator
		TableRow sep = new TableRow(StatsActivity.this);
		sep.setMinimumHeight(10);
		TableRow sep1 = new TableRow(StatsActivity.this);
		sep1.setMinimumHeight(10);
		TableRow sep2 = new TableRow(StatsActivity.this);
		sep2.setMinimumHeight(10);
		TableRow sep3 = new TableRow(StatsActivity.this);
		sep3.setMinimumHeight(10);
		
		//add rows to table
		tl.addView(tr1);
		tl.addView(sep);
		tl.addView(tr2);
		tl.addView(tr3);
		tl.addView(tr7);
		tl.addView(sep1);
//		tl.addView(tr4);	//not adding distance as inaccurate at the moment
		tl.addView(sep2);
		tl.addView(tr5);
		tl.addView(sep3);
		tl.addView(tr6);
		tl.addView(tr8);
		tl.addView(tr9);
		if (!q2n.equals(""))
			tl.addView(tr10);
		tl.addView(tr11);
		tl.addView(tr12);
		
		expButton.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				if (!emulator) {
					//prepare writing csv+kml file to sd card
			        File sdCard = Environment.getExternalStorageDirectory();
					File store = new File(sdCard.getAbsolutePath()+"/JourneyDiary");
					if (!store.isDirectory())
					{
						store.mkdir();
						Log.d(getClass().getSimpleName(), "just made dir");
					}
					Log.d("StatsActivity", "sdCard:"+sdCard.toString());
					
					//write userdata to .csv file
//					File file = new File(store, "Journey"+journeyNo+".csv");
					File file = new File(store, "Journeys.csv");
					if (!file.isFile())
					{
						try {
							file.createNewFile();
						} catch (IOException e) {
							Log.e(getClass().getSimpleName(), e.toString());
							Log.d(getClass().getSimpleName(), "just made file");
						}
						try {
							 bufferedWriter = new BufferedWriter(new FileWriter(file));
						} catch (IOException e) {
							Log.e(getClass().getSimpleName(), e.toString());
						}
						try {
							bufferedWriter.write("journeyId,startDate,endDate,travelTime,transport,purpose,beforeJourney,shared,role,wouldShared,comment, \n"
									+journeyNo +","+ startDate +","+endDate +","+ travelTime +","+ transport +","+ purpose +","+beforeJourney +","+ q1 +","+ q2y +","+q2n +",'"+comment +"'" );
						}catch (IOException e) {
							Log.e("StatsActivity", "writing to sdcard failed: "+e.toString());
						}
						try {
							bufferedWriter.close();
						} catch (IOException e) {
							Log.e("StatsActivity",e.toString());
						}
					}
					else if (file.isFile()) {
						try {
							bufferedReader = new BufferedReader(new FileReader(file));
						} catch (IOException e) {
							Log.e(getClass().getSimpleName(), e.toString());
						}
						String prevContents ="";
						String line;
						boolean firstRow=true;
						try {
							while ((line = bufferedReader.readLine()) != null) {
								if (firstRow) {
									prevContents = line;
									Log.d("StatsActivity", "prevContents: "+prevContents);
								}
								if (!firstRow) {
									Log.d("StatsActivity", "prevContents: "+prevContents+" line: "+line);
									prevContents = prevContents+"\n"+line;
								}
								firstRow = false;
							}
						} catch (IOException e) {
							Log.e(getClass().getSimpleName(), e.toString());
						}
						try {
							 bufferedWriter = new BufferedWriter(new FileWriter(file));
							 bufferedWriter.write(prevContents +"\n"+ 
									 journeyNo +","+ startDate +","+endDate +","+ travelTime +","+ transport +
									 ","+ purpose +","+beforeJourney +","+ q1 +","+ q2y +","+q2n +",'"+comment +"'" );
							 bufferedWriter.close();
						} catch (IOException e) {
							Log.e("StatsActivity",e.toString());
						}
					}
					
					
					//write kml file to sdcard
					File kmlFile = new File(store, "Journey"+journeyNo+".kml");
					if (!kmlFile.isFile())
					{
						try {
							kmlFile.createNewFile();
						} catch (IOException e) {
							Log.e(getClass().getSimpleName(), e.toString());
							Log.d(getClass().getSimpleName(), "just made kmlFile");
						}
					}
					try {
						 bufferedWriter = new BufferedWriter(new FileWriter(kmlFile));
					} catch (IOException e) {
						Log.e(getClass().getSimpleName(), e.toString());
					}
					//write "preamble"
					try {
						bufferedWriter.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n" 
								+ "<kml xmlns=\"http://www.opengis.net/kml/2.2\"> \n" 
								+ "<Document> \n" 
								+ "<name>Journey "+journeyNo+"</name>\n"
								+ "<description>Mode of transport: "+transport+".\n" 
								+ "Purpose: "+purpose+".\n"
								+ "Start time: "+startDate +" \n"
								+ "End time: "+endDate +" \n"
								+ "Time travelled: "+travelTime +" mins \n"
//								+ "Distance: "+distance+" m \n "			//inaccurate so don't print out
								+ "Q's answered before journey: "+beforeJourneyStr +" \n"
								+ "Shared journey?: "+q1 +"\n"
								+ "Would have shared journey?: "+q2n+" \n"
								+ "Role: "+q2y +" \n"
								+ "Comment: "+comment +". \n"
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
			    	
					double lat = 0;
					double lng = 0;
					long timestamp = 0;
					String time = "";
					
					if (!db.isOpen()) {
						db = new LocationsSQLHelper(StatsActivity.this).getReadableDatabase();
					}
					Cursor c = db.rawQuery("SELECT lat,long,time,timestamp FROM locations WHERE journeyId="+journeyNo, null);
					if (c.getCount() > 0) {
						c.moveToFirst();
						try {
							lat = c.getLong(0)/1E6;
							lng = c.getLong(1)/1E6;
							time = c.getString(2);
							timestamp = c.getLong(3);
						}catch (NullPointerException npe) {
							Log.d("StatsActivity", npe.toString());
						}
						//write first placemark as starting point 
						try {
		    				bufferedWriter.write("<Placemark> \n"
		    						+ "<name>Start</name>"
		    						+ "<description>Time: "+time+" \n" 
									+ "Timestamp: "+timestamp+ "\n" 
									+ "</description> \n" 
		    						+ "<Point><coordinates>"+lng+ "," +""+lat +",50</coordinates></Point> \n"
		    						+ "</Placemark> \n" 
		    						+ "<Placemark> \n " 
									+ "<name>path</name> \n "
									+ "<styleUrl>#yellowLineGreenPoly</styleUrl> \n" 
									+ "<LineString> \n"
									+ "<extrude>1</extrude> \n "
									+ "<tessellate>1</tessellate> \n"
									+ "<altitudeMode>relativeToGround</altitudeMode> \n"
									+ "<coordinates> \n"
									+ ""+lng + "," +""+lat +",50 \n");
	    				}catch (IOException e) {
	    					Log.e("OverviewMap", e.toString());
	    				}
	    				//add all the other waypoints
	    				while (c.moveToNext()) {
	    					try {
								lat = c.getLong(0)/1E6;
								lng = c.getLong(1)/1E6;
							}catch (NullPointerException npe) {
								Log.d("StatsActivity", npe.toString());
							}
	    					try {
								bufferedWriter.write(""+lng + "," +""+lat +",50 \n");	//displays altitude of line at 50
							} catch (IOException e) {
								Log.e(getClass().getSimpleName(), e.toString());
							}
	    				}

	    				try {
							bufferedWriter.write("</coordinates> \n" 
									+ "</LineString> \n"
									+ "</Placemark> \n"
//									+ "</Document> \n "
//									+ "</kml>"
									);
						} catch (IOException e) {
							Log.e(getClass().getSimpleName(), e.toString());
						}
						c.moveToFirst();
						while (c.moveToNext()) {
							try {
								lat = c.getLong(0)/1E6;
								lng = c.getLong(1)/1E6;
								time = c.getString(2);
								timestamp = c.getLong(3);
								
							}catch (NullPointerException npe) {
								Log.d("StatsActivity", npe.toString());
							}
	    					try {
								bufferedWriter.write("<Placemark> \n " +
										"<description>Time: "+time+" \n" +
										"Timestamp: "+timestamp+ "\n" +
										"</description> \n" +
										"<Point><coordinates>" +
										lng + "," +""+lat +",50</coordinates></Point>\n" +
										"</Placemark>\n");	
							} catch (IOException e) {
								Log.e(getClass().getSimpleName(), e.toString());
							}
						}
						//write closings
	    				try {
							bufferedWriter.write(
									  "</Document> \n "
									+ "</kml>"
									);
						} catch (IOException e) {
							Log.e(getClass().getSimpleName(), e.toString());
						}
					}
					c.close();
					db.close();
							
					try {
						bufferedWriter.close();
					} catch (IOException e) {
						Log.e("StatsActivity",e.toString());
					}
					Toast.makeText(StatsActivity.this, "Successfully exported to sdcard", Toast.LENGTH_SHORT).show();
				}
			}
		});
			
	}
	
	
		
		
	

}
