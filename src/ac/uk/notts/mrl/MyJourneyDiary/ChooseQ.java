package ac.uk.notts.mrl.MyJourneyDiary;
/**
 * @author jef@cs.nott.ac.uk
 * Here, users get to choose if they want to start recording now or answer more questions (Q1-3) first.
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

public class ChooseQ extends Activity {
	
	private SQLiteDatabase db;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.chooseq);
		
		db = new UserDataSQLHelper(ChooseQ.this).getWritableDatabase();
		
		Button nowButton = (Button) findViewById(R.id.now_button);
		nowButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
		        db.execSQL("UPDATE userdata SET beforeJourney=1 WHERE id=(SELECT max(id) FROM userdata)");
				startActivity(new Intent(ChooseQ.this, Question1.class));		
			}
		});
		
		Button laterButton = (Button) findViewById(R.id.later_button);
		laterButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
		        db.execSQL("UPDATE userdata SET beforeJourney=0 WHERE id=(SELECT max(id) FROM userdata)");
				Intent intent = new Intent(ChooseQ.this, OverviewMap.class).putExtra("startRecording", true);
				intent.putExtra("exitQuestions", true);
				intent.putExtra("isRecording", true);
				startActivity(intent);		
			}
		});
	}
	
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		
		if (hasFocus) {
			if (db==null || (!db.isOpen())) {
				db = (new UserDataSQLHelper(ChooseQ.this)).getWritableDatabase();
				Log.d("ChooseQ","just created new DB");
			}
	        
		}
		else if (!hasFocus) {
			//db.close();
		}
	}

	public void onPause() {
		super.onPause();
		Log.d("ChooseQ","onPause");
		db.close();
	}

}
