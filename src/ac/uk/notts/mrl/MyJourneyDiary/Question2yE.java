package ac.uk.notts.mrl.MyJourneyDiary;
/**
 * @author jef@cs.nott.ac.uk
 * Showed if answer to @see Question1E was yes. 
 */

import ac.uk.notts.mrl.MyJourneyDiary.R;
import android.app.Activity;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class Question2yE extends Activity {
	
	private SQLiteDatabase db;
	
	public void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.question2y_e);
		
		db = new UserDataSQLHelper(Question2yE.this).getWritableDatabase();

		Button yesButton = (Button) findViewById(R.id.giver_button);
		yesButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				db.execSQL("UPDATE userdata SET q2y='driver' WHERE id=(SELECT max(id) FROM userdata)");
				startActivity(new Intent(Question2yE.this, Question3E.class));		
			}
		});
		
		Button noButton = (Button) findViewById(R.id.taker_button);
		noButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				db.execSQL("UPDATE userdata SET q2y='passenger' WHERE id=(SELECT max(id) FROM userdata)");
				startActivity(new Intent(Question2yE.this, Question3E.class));		
			}
		});
		
	}
	
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		
		if (hasFocus) {
			if (db==null || (!db.isOpen())) {
				db = (new UserDataSQLHelper(Question2yE.this)).getWritableDatabase();
				Log.d("Question2yE","just created new DB");
			}
	        
		}
		else if (!hasFocus) {
			//db.close();
		}
	}

	public void onPause() {
		super.onPause();
		Log.d("Question2yE","onPause");
		db.close();
	}

}
