package ac.uk.notts.mrl.MyJourneyDiary;
/**
 * @author jef@cs.nott.ac.uk 
 * Shown if first question is answered "no".
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

public class Question2nE extends Activity {
	
	private SQLiteDatabase db;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.question2n_e);
		
		db = new UserDataSQLHelper(Question2nE.this).getWritableDatabase();
		
		Button yesButton = (Button) findViewById(R.id.yes_button);
		yesButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				db.execSQL("UPDATE userdata SET q2n='yes' WHERE id=(SELECT max(id) FROM userdata)");
				startActivity(new Intent(Question2nE.this, Question2yE.class));		
			}
		});
		
		Button noButton = (Button) findViewById(R.id.no_button);
		noButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				db.execSQL("UPDATE userdata SET q2n='no' WHERE id=(SELECT max(id) FROM userdata)");
				startActivity(new Intent(Question2nE.this, Question3reasonsE.class));		
			}
		});
		
//		Button maybeButton = (Button) findViewById(R.id.maybe_button);
//		maybeButton.setOnClickListener(new OnClickListener() {
//
//			public void onClick(View v) {
//				db.execSQL("UPDATE userdata SET q2n='maybe' WHERE id=(SELECT max(id) FROM userdata)");
//				startActivity(new Intent(Question2nE.this, Question2yE.class));		
//			}
//		});
		
	}
	
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		
		if (hasFocus) {
			if (db==null || (!db.isOpen())) {
				db = (new UserDataSQLHelper(Question2nE.this)).getWritableDatabase();
				Log.d("Question2nE","just created new DB");
			}
	        
		}
		else if (!hasFocus) {
			//db.close();
		}
	}

	public void onPause() {
		super.onPause();
		Log.d("Question2ne","onPause");
		db.close();
	}

}
