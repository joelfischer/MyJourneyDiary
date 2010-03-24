package ac.uk.notts.mrl.MyJourneyDiary;
/**
 * @author jef@cs.nott.ac.uk 
 * This receiver catches the intents issued by an AlarmManager, which is used to fire the reminders. 
 * It triggers the @see ReminderSchedulerService, which instantiates the next AlarmManager. 
 * Also, it directly notifies the user with the reminders in showNotification(). 
 */

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;


public class Reminder_BroadcastReceiver extends BroadcastReceiver {
	
	private final static int WAKE_TIME = 5000;  //wake the device up for 5s to make sure notfication is delivered

	@Override
	public void onReceive(Context context, Intent intent) {
		
		
		PowerManager pm = (PowerManager) context.getSystemService(Service.POWER_SERVICE);
        PowerManager.WakeLock w = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Reminder_BroadcastReceiver");
        w.acquire(WAKE_TIME);
        
		Log.d("Reminder_BroadcastReceiver","fired");
		showNotification(context, intent);
		
		context.startService(new Intent (context, ReminderSchedulerService.class));
		
	}
	
	private void showNotification(Context context, Intent intent) {
		
		NotificationManager nm = (NotificationManager) context.getSystemService(Service.NOTIFICATION_SERVICE);
		CharSequence text ="";
		
		 //cancel the new activity request notification
        nm.cancel(R.string.app_reminder);
        nm.cancel(R.string.diary_reminder);
		
		String mod = intent.getStringExtra("mod");
		Log.d("Reminder_BroadcastReceiver", "mod: "+mod);
		if (mod.equals("morning")) {
			text = context.getText(R.string.app_reminder);
		}
		else if (mod.equals("evening")) {
			text = context.getText(R.string.diary_reminder);
		}
       
       
        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.reminder, text,
                System.currentTimeMillis());
        notification.defaults = Notification.DEFAULT_ALL;

        PendingIntent contentIntent;
        // The PendingIntent to launch our activity if the user selects this notification
        if (mod.equals("morning")) {
	        contentIntent = PendingIntent.getActivity(context, 0,
	                new Intent(Intent.CATEGORY_HOME), 0);
	        // Set the info for the views that show in the notification panel.
	        notification.setLatestEventInfo(context, context.getText(R.string.app_reminder_label),
                    text, contentIntent);
	     // Send the notification.
	        // We use a layout id because it is a unique number.  We use it later to cancel.
	        nm.notify(R.string.app_reminder, notification);
        }
        else if (mod.equals("evening")) {
	        contentIntent = PendingIntent.getActivity(context, 0,
	                new Intent(Intent.CATEGORY_HOME), 0);
	        		//new Intent(context, OnlineQuestionnaire.class), 0);		//change to this intent to display questionnaire on select
	        // Set the info for the views that show in the notification panel.
	        notification.setLatestEventInfo(context, context.getText(R.string.diary_reminder_label),
                    text, contentIntent);
	     // Send the notification.
	        // We use a layout id because it is a unique number.  We use it later to cancel.
	        nm.notify(R.string.diary_reminder, notification);
        }

    }
	

}