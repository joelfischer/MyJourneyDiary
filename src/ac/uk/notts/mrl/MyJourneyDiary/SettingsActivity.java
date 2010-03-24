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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

public class SettingsActivity extends Activity {
	
	private SQLiteDatabase db;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);

		//initialise spinner to pick transport mode
		Spinner transportPicker= (Spinner) findViewById(R.id.spinner2);
		ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(this, R.array.transport_array, android.R.layout.simple_spinner_item);
		adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		transportPicker.setAdapter(adapter2);
		transportPicker.setOnItemSelectedListener(new MyOnItemSelectListener2());
		
		//db to store user data
		db = (new UserDataSQLHelper(SettingsActivity.this)).getWritableDatabase();
		//insert first row, overwritten in a sec. 
		db.execSQL("INSERT INTO userdata (transport) VALUES ('default')");

		//initialise spinner to pick purpose of journey
		Spinner purposePicker = (Spinner) findViewById(R.id.spinner1);
		ArrayAdapter<CharSequence> adapter1 = ArrayAdapter.createFromResource(this, R.array.purpose_array, android.R.layout.simple_spinner_item);
		adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		purposePicker.setAdapter(adapter1);
		purposePicker.setOnItemSelectedListener(new MyOnItemSelectListener1());
		
//		TextView tv = (TextView) findViewById(R.id.comment);
//		final EditText edittext = (EditText) findViewById(R.id.entry);
//		edittext.setOnKeyListener(new OnKeyListener() {
//		    public boolean onKey(View v, int keyCode, KeyEvent event) {
//		        // If the event is a key-down event on the "enter" button
//		        if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
//		            (keyCode == KeyEvent.KEYCODE_ENTER)) {
//		          // Perform action on key press
//		          Toast.makeText(SettingsActivity.this, "Thanks for your comment", Toast.LENGTH_SHORT).show();
//		          String comment = edittext.getText().toString();
//		          db.execSQL("UPDATE settings SET comment='"+ comment + 
//					"' WHERE id=(SELECT MAX(id) FROM settings)");
//		          
//		          return true;
//		        }
//		        return false;
//		    }
//		});	
		
		Button nextButton = (Button) findViewById(R.id.next_button);
		nextButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				startActivity(new Intent(SettingsActivity.this, ChooseQ.class));		
			}
		});
		

	}
	
		
public class MyOnItemSelectListener2 implements OnItemSelectedListener {
		
		public void onItemSelected(AdapterView<?> parent, View view, int pos,
				long id) {
			Toast.makeText(parent.getContext(), "Transport set to: " + 
					parent.getItemAtPosition(pos).toString(), Toast.LENGTH_SHORT).show();
			String selection = parent.getItemAtPosition(pos).toString();
			
			//write to DB...
			db.execSQL("UPDATE userdata SET transport='"+ selection + 
					"' WHERE id=(SELECT MAX(id) FROM userdata)");
			
		}

		public void onNothingSelected(AdapterView<?> arg0) {
			// TODO Auto-generated method stub
			
		}
	}

public class MyOnItemSelectListener1 implements OnItemSelectedListener {
	
	public void onItemSelected(AdapterView<?> parent, View view, int pos,
			long id) {
		Toast.makeText(parent.getContext(), "Purpose set to: " + 
				parent.getItemAtPosition(pos).toString(), Toast.LENGTH_SHORT).show();
		String selection = parent.getItemAtPosition(pos).toString();
		
		//write to DB...
		db.execSQL("UPDATE userdata SET purpose='"+ selection + 
				"' WHERE id=(SELECT MAX(id) FROM userdata)");
		
	}

	public void onNothingSelected(AdapterView<?> arg0) {
	}
}

public void onWindowFocusChanged(boolean hasFocus) {
	super.onWindowFocusChanged(hasFocus);
	
	Log.d("SettingsActivity", "hasFocus="+hasFocus);
	if (hasFocus) {
		if (db==null || (!db.isOpen())) {
			db = (new UserDataSQLHelper(SettingsActivity.this)).getWritableDatabase();
			Log.d("Settings","just created new DB");
		}
        
	}
	else if (!hasFocus) {
		//db.close();
	}
}

public void onPause() {
	super.onPause();
	Log.d("SettingsActivity","onPause");
	db.close();
}

	
	

}
