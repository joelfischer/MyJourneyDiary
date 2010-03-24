package ac.uk.notts.mrl.MyJourneyDiary;
/**
 * @author jef@cs.nott.ac.uk 
 */

import ac.uk.notts.mrl.MyJourneyDiary.R;
import android.app.Activity;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class Question3reasonsE extends Activity {
	
	private SQLiteDatabase db;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.q3reasons_e);
		
		db = new UserDataSQLHelper(Question3reasonsE.this).getWritableDatabase();
		
		final EditText edittext = (EditText) findViewById(R.id.reasons_entry);
		edittext.setOnKeyListener(new OnKeyListener() {
		    public boolean onKey(View v, int keyCode, KeyEvent event) {
		        // If the event is a key-down event on the "enter" button
		        if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
		            (keyCode == KeyEvent.KEYCODE_ENTER)) {
		          // Perform action on key press
		          Toast.makeText(Question3reasonsE.this, "Thanks for your comment", Toast.LENGTH_SHORT).show();
		          String comment = edittext.getText().toString();
		          if (comment.contains("'"))
		        	  comment = comment.replace("'", "");
		          
		          //TODO: check if we need string tokenizer
		          db.execSQL("UPDATE userdata SET comment='"+comment+"' WHERE id=(SELECT max(id) FROM userdata)");
		          
		          return true;
		        }
		        return false;
		    }
		});	
		
		Button startButton = (Button) findViewById(R.id.done_button);
		startButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				Intent intent = new Intent(Question3reasonsE.this, OverviewMap.class).putExtra("startRecording", false);
				intent.putExtra("exitQuestions",false);
				startActivity(intent);				
				String comment = edittext.getText().toString();
//		          db.execSQL("UPDATE settings SET comment='"+ comment + 
//					"' WHERE id=(SELECT MAX(id) FROM settings)");
			}
		});
	}
	
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		
		if (hasFocus) {
			if (db==null || (!db.isOpen())) {
				db = (new UserDataSQLHelper(Question3reasonsE.this)).getWritableDatabase();
			}
	        
		}
		else if (!hasFocus) {
			//db.close();
		}
	}

	public void onPause() {
		super.onPause();
		db.close();
	}
}
