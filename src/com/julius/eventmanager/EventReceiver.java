package com.julius.eventmanager;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class EventReceiver extends BroadcastReceiver {

	private final static String TAG = "EventReceiver";

	@Override
	public void onReceive(Context context, Intent i) {
		Log.i(TAG, i.getAction());
		long interval = Long.parseLong(((EventManagerApplication) context
				.getApplicationContext()).getInterval());
		Log.i(TAG, "Interval " + interval);

		// Intent intent = new Intent(EventManagerApplication.ALARM_ACTION);
		Intent intent = new Intent(context, EventManagerService.class);
		PendingIntent pendingIntent = PendingIntent.getService(context, -1,
				intent, PendingIntent.FLAG_CANCEL_CURRENT);

		AlarmManager alarmManager = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);

		if (interval == 0) {
			alarmManager.cancel(pendingIntent);
		} else {
			alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP,
					System.currentTimeMillis(), interval, pendingIntent);
		}
	}

}
