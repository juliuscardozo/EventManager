package com.julius.eventmanager;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.util.Log;

public class EventManagerApplication extends Application implements
		OnSharedPreferenceChangeListener {

	public final static String REFRESH_ACTION = "com.julius.eventmanager.REFRESH_ACTION";
	private final static String TAG = "EventManagerApplication";
	private SharedPreferences prefs;
	private EventData eventData;

	@Override
	public void onCreate() {
		super.onCreate();
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(this);
		eventData = new EventData(this);
		BroadCastRefresh();
		Log.i(TAG, "Application started");
	}

	private void BroadCastRefresh() {
		Intent i = new Intent();
		i.setAction(REFRESH_ACTION);
		sendBroadcast(i);
	}

	public SharedPreferences getSharedPrefs() {
		return prefs;
	}

	public String getInterval() {
		return prefs.getString(getString(R.string.prefs_refresh_interval),
				"86400000");
	}

	public long getSelectedCalendarID() {
		return Long.parseLong(prefs.getString(getString(R.string.prefs_calendar),
				"1"));
	}
	
	public EventData getEvenData() {
		return eventData;
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (key == getString(R.string.prefs_refresh_interval)) {
			BroadCastRefresh();
		}
	}

	@Override
	public void onTerminate() {
		Log.i(TAG, "Application terminated");
		eventData.close();
		super.onTerminate();
	}

}
