package ac.uk.notts.mrl.MyJourneyDiary;
/**
 * @author jef
 * This BroadcastReceiver listens to system-wide broadcasts that triggers rescheduling of reminders. 
 * The intent BOOT_COMP triggers ReminderSchedulerService after every boot up. 
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


public class StartingEvent_BroadcastReceiver extends BroadcastReceiver {

	private final static String BOOT_COMP = "android.intent.action.BOOT_COMPLETED";	
	
	//@override
	public void onReceive(Context context, Intent intent) {
		Log.d("StartingEvent_BroadcastReceiver", "intent=" + intent);
		
		if(intent!=null && intent.getAction()!=null) {
					
			if (BOOT_COMP.compareToIgnoreCase(intent.getAction())==0) {
				Log.d(getClass().getSimpleName(), "BOOT_COMPLETED");
				context.startService(new Intent(context, ReminderSchedulerService.class));
			}
		}
				
	}

}
