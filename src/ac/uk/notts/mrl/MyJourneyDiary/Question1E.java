package ac.uk.notts.mrl.MyJourneyDiary;
/**
 * @author jef@cs.nott.ac.uk
 */

import ac.uk.notts.mrl.MyJourneyDiary.R;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class Question1E extends Activity {
	
	private SQLiteDatabase db;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.question1_e);
		
		db = new UserDataSQLHelper(Question1E.this).getWritableDatabase();
		
		Button yesButton = (Button) findViewById(R.id.yes_button);
		yesButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				db.execSQL("UPDATE userdata SET q1='yes' WHERE id=(SELECT max(id) FROM userdata)");
				
				//determine transport mode
				String transport="";
				Cursor c = db.rawQuery("SELECT transport FROM userdata WHERE id=(SELECT max(id) FROM userdata)", null);
				if (c.getCount()>0) {
					c.moveToFirst();
					try {
						transport = c.getString(0);
					}catch (Exception e) {
						Log.e("Question1", e.toString());
					}
					c.close();
				}
				//only ask next question if transport mode is by car. 
				if (transport.equals("car"))
					startActivity(new Intent(Question1E.this, Question2yE.class));
				else 
					startActivity(new Intent(Question1E.this, Question3E.class));
			}
		});
		
		Button noButton = (Button) findViewById(R.id.no_button);
		noButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				db.execSQL("UPDATE userdata SET q1='no' WHERE id=(SELECT max(id) FROM userdata)");
				startActivity(new Intent(Question1E.this, Question2nE.class));		
			}
		});
		
	}
	
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		
		if (hasFocus) {
			if (db==null || (!db.isOpen())) {
				db = (new UserDataSQLHelper(Question1E.this)).getWritableDatabase();
				Log.d("Question1E","just created new DB");
			}
	        
		}
		else if (!hasFocus) {
			//db.close();
		}
	}

	public void onPause() {
		super.onPause();
		Log.d("Question1E","onPause");
		db.close();
	}
	
	

}
