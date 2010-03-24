package ac.uk.notts.mrl.MyJourneyDiary;
/**
 * @author jef@cs.nott.ac.uk 
 */

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class UserDataSQLHelper extends SQLiteOpenHelper {
	
	 private static final String DATABASE_NAME = "userdata_04.db"; 	
 	 private static final int DATABASE_VERSION = 2;
	 private static final String TABLE_NAME = "userdata";   
	 private static final String TAG = "UserDataSQLHelper";
	
	UserDataSQLHelper (Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE " + TABLE_NAME +
				" (" + 
				" id INTEGER PRIMARY KEY AUTOINCREMENT," +
				" journeyId INTEGER," +
				" purpose VARCHAR(20)," +
				" transport VARCHAR(20)," +
				" beforeJourney BOOL," + 
				" q1 VARCHAR(5)," +
				" q2y VARCHAR(10)," +
				" q2n VARCHAR(5)," +
				" comment VARCHAR(200)" +
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
