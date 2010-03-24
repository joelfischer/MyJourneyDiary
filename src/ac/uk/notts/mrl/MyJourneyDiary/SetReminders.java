package ac.uk.notts.mrl.MyJourneyDiary;
/**
 * @author jef@cs.nott.ac.uk
 * Activity to set reminders which triggers the @see ReminderSchedulerService.
 */

import ac.uk.notts.mrl.MyJourneyDiary.R;
import android.app.Activity;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;

public class SetReminders extends Activity {
	
	private TextView morningTime;
	private TextView eveningTime;
	private Button setMorningTime;
	private Button setEveningTime;
	private Button doneButton;
	
	private int morningHr;
	private int morningMin;
	private int eveningHr;
	private int eveningMin;
	
	static final int TIME_DIALOG_ID_M = 0;
	static final int TIME_DIALOG_ID_E = 1;
	
	private SQLiteDatabase db;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.reminders);
		
		morningTime = (TextView) findViewById(R.id.morningTimeDisplay);
		setMorningTime = (Button) findViewById(R.id.pickMorningTime);
		doneButton = (Button) findViewById(R.id.done_button);
		
		setMorningTime.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				showDialog(TIME_DIALOG_ID_M);
			}
		});
		
		
		//default to show when nothing in db
        morningHr = 0;
        morningMin = 0;
        
        eveningHr = 0;
        eveningMin = 0;
       
        
        eveningTime = (TextView) findViewById(R.id.eveningTimeDisplay);
		setEveningTime = (Button) findViewById(R.id.pickEveningTime);
		
		setEveningTime.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				showDialog(TIME_DIALOG_ID_E);	
			}
		});
		
		db = new ReminderSQLHelper(SetReminders.this).getWritableDatabase();
		Cursor cu = db.rawQuery("SELECT morning_reminder_hr, morning_reminder_min, evening_reminder_hr, evening_reminder_min FROM reminder ", null);
		if (cu.getCount()!=0) {
			//we have some data
			cu.moveToFirst();
			try {
				morningHr = cu.getInt(0);
				morningMin = cu.getInt(1);
				eveningHr = cu.getInt(2);
				eveningMin = cu.getInt(3);
			}catch (Exception e) {
				Log.e("SetReminders", e.toString());
			}
		}
		cu.close();
        updateMorningDisplay();

        // display the current date
        updateEveningDisplay();
        
        doneButton.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				db.close();
				startService(new Intent(SetReminders.this, ReminderSchedulerService.class));
				startActivity(new Intent(SetReminders.this, OverviewMap.class));
				
			}
		});
	
        
	}
	
	private static String pad(int c) {
		if (c>=10)
			return String.valueOf(c);
		else 
			return "0" + String.valueOf(c);
	}
	
    private void updateMorningDisplay() {
    	morningTime.setText(
    			new StringBuilder()
    			.append("Please set your reminders. This only has to be done once. \n \n ")
    			.append("This reminder will remind you of using the app during the day. " +
    					"Please set it to an am time.\n \n Morning reminder set to: ")
    			.append(pad(morningHr)).append(":")
    			.append(pad(morningMin)));
    }
    
    private void updateEveningDisplay() {
    	eveningTime.setText(
    			new StringBuilder()
    			.append(" \n \n This reminder will remind you to complete the online questionnaire at the " +
    					"end of the day. Please set it to a pm time.\n \n Evening reminder set to: ")
    			.append(pad(eveningHr)).append(":")
    			.append(pad(eveningMin)));
    }
    
 // the callback received when the user "sets" the time in the dialog
    private TimePickerDialog.OnTimeSetListener morningTimeSetListener =
        new TimePickerDialog.OnTimeSetListener() {
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                morningHr = hourOfDay;
                morningMin = minute;
                
                Cursor c = db.rawQuery("SELECT morning_reminder_hr FROM reminder", null);
                if (c.getCount()==0) {
                	db.execSQL("INSERT INTO reminder (morning_reminder_hr,morning_reminder_min) VALUES ("+morningHr+","+ morningMin+")");
                }
                else if (c.getCount()>0) {
                	db.execSQL("UPDATE reminder SET morning_reminder_hr="+morningHr+",morning_reminder_min="+morningMin+" WHERE id=1");
                }
                c.close();
                
                updateMorningDisplay();
            }
        };
        
    private TimePickerDialog.OnTimeSetListener eveningTimeSetListener =
        new TimePickerDialog.OnTimeSetListener() {
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                eveningHr = hourOfDay;
                eveningMin = minute;
                
                Cursor c = db.rawQuery("SELECT evening_reminder_hr FROM reminder", null);
                if (c.getCount()==0) {
                	db.execSQL("INSERT INTO reminder (evening_reminder_hr,evening_reminder_min) VALUES ("+eveningHr+","+ eveningMin+")");
                }
                else if (c.getCount()>0) {
                	db.execSQL("UPDATE reminder SET evening_reminder_hr="+eveningHr+",evening_reminder_min="+eveningMin+" WHERE id=1");
                }
                c.close();
                
                updateEveningDisplay();
            }
        };
        
        @Override
		protected Dialog onCreateDialog(int id) {
		    switch (id) {
		    case TIME_DIALOG_ID_M:
		        return new TimePickerDialog(this,
		                morningTimeSetListener, morningHr, morningMin, true);
		    case TIME_DIALOG_ID_E:
		    	return new TimePickerDialog(this,
		                eveningTimeSetListener, eveningHr, eveningMin, true);
		    }
		    return null;
		}
        
        public void onWindowFocusChanged(boolean hasFocus) {
        	super.onWindowFocusChanged(hasFocus);
        	
        	Log.d("SetReminders", "hasFocus="+hasFocus);
        	if (hasFocus) {
        		if (db==null) {
        			db = (new ReminderSQLHelper(SetReminders.this)).getWritableDatabase();
        			Log.d("Settings","just created new DB");
        		}
                
        	}
        	else if (!hasFocus) {
        		//db.close();
        	}
        }

        public void onPause() {
        	super.onPause();
        	Log.d("SetReminders","onPause");
//        	db.close();
        }
    

		
		
	

}
