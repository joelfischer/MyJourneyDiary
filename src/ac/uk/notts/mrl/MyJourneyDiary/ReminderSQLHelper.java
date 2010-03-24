package ac.uk.notts.mrl.MyJourneyDiary;
/**
 * @author jef@cs.nott.ac.uk
 */

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class ReminderSQLHelper extends SQLiteOpenHelper {
	
	 private static final String DATABASE_NAME = "reminder_02.db"; 	
 	 private static final int DATABASE_VERSION = 2;
	 private static final String TABLE_NAME = "reminder";   
	 private static final String TAG = "ReminderSQLHelper";
	
	ReminderSQLHelper (Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE " + TABLE_NAME +
				" (" + 
				" id INTEGER PRIMARY KEY AUTOINCREMENT," +
				" morning_reminder_hr INTEGER," +
				" morning_reminder_min INTEGER," +
				" evening_reminder_hr INTEGER," +
				" evening_reminder_min INTEGER" +
				");");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS notes");
        onCreate(db);
		
	}

}
