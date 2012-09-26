package com.julius.eventmanager;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class EventListActivity extends Activity {

	private static final String TAG = "EventListActivity";
	static final String[] CAL_COLUMNS = { "title", "dtstart", "description" };
	static final int[] TO = { R.id.title, R.id.startTime, R.id.detail };

	ArrayList<HashMap<String, String>> listItems = new ArrayList<HashMap<String, String>>();
	SimpleAdapter adapter;
	NewEventReceiver newEventReceiver;
	IntentFilter filter;

	ListView listView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list);

		adapter = new SimpleAdapter(this, listItems, R.layout.row, CAL_COLUMNS,
				TO);
		setTitle(getString(R.string.title));

		listView = (ListView) findViewById(R.id.list);
		listView.setAdapter(adapter);
		listView.setScrollbarFadingEnabled(true);
		listView.setEmptyView(findViewById(R.id.empty));

		newEventReceiver = new NewEventReceiver();
		filter = new IntentFilter(EventManagerService.NEW_EVENT_INTENT);
	}

	@Override
	protected void onResume() {
		super.onResume();
		ProcessEventData();
		super.registerReceiver(newEventReceiver, filter,
				getString(R.string.SEND_EVENT_PERMISSION), null);
	}

	@Override
	protected void onPause() {
		super.onPause();
		super.unregisterReceiver(newEventReceiver);
		listItems.clear();
	}

	synchronized private void ProcessEventData() {
		EventData events = ((EventManagerApplication) getApplication())
				.getEvenData();

		Cursor cursor = events.getAllEvents();
		Calendar cal = Calendar.getInstance();
		DateFormat dateFormat = new SimpleDateFormat("MMMddyy");
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
		dateFormat.setCalendar(cal);
		Date now = dateFormat.getCalendar().getTime();

		while (cursor.moveToNext()) {
			long id = cursor.getLong(0);

			Date eventDate = new Date(cursor.getLong(2));

			Log.d(TAG,
					dateFormat.format(now) + "=="
							+ dateFormat.format(eventDate));

			if (dateFormat.format(now).equals(dateFormat.format(eventDate))) {
				ReadTodaysEventsFromCalendar(id);
			}
		}
	}

	private void ReadTodaysEventsFromCalendar(long id) {

		long calendarId = ((EventManagerApplication) getApplication())
				.getSelectedCalendarID();
		Uri uri = Uri.parse("content://com.android.calendar/events/" + id);

		Cursor eventCursor = getContentResolver().query(uri, CAL_COLUMNS,
				"calendar_id = " + calendarId, null, null);

		if (eventCursor != null) {
			while (eventCursor.moveToNext()) {
				HashMap<String, String> map = new HashMap<String, String>();
				map.put(CAL_COLUMNS[0], eventCursor.getString(0));
				Calendar cal = Calendar.getInstance();
				cal.setTimeInMillis(eventCursor.getLong(1));
				DateFormat dateFormat = new SimpleDateFormat("hh:mm a");
				map.put(CAL_COLUMNS[1],
						"At " + dateFormat.format(cal.getTime()));
				map.put(CAL_COLUMNS[2], eventCursor.getString(2));
				listItems.add(map);
				adapter.notifyDataSetChanged();
			}
		} else {
			Log.e(TAG, "Invalid Cursor");
		}

	}

	private void LaunchCalendar() {
		Uri uriCalendar = Uri.parse("content://com.android.calendar/time/"
				+ String.valueOf(System.currentTimeMillis()));
		Intent calendarIntent = new Intent(Intent.ACTION_VIEW, uriCalendar);

		try {
			startActivity(calendarIntent);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(this, "Google Calendar Widget not Found!",
					Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_event_list, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		boolean connected = false;
		switch (item.getItemId()) {
		case R.id.menu_calendar:
			LaunchCalendar();
			Log.i(TAG, "Launched Calendar!");
			return true;
		case R.id.menu_refresh:
			Log.i(TAG, "Refreshing data from webservice!");
			ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
			NetworkInfo network = connManager.getActiveNetworkInfo();
			if (network != null)
				connected = network.isConnected();

			if (connected) {
				Intent serviceIntent = new Intent(this,
						EventManagerService.class);
				startService(serviceIntent);
			} else {
				Log.w(TAG, getString(R.string.no_network_msg));
				// Toast.makeText(this, getString(R.string.no_network_msg),
				// Toast.LENGTH_SHORT).show();
				showNetworkErrorMsg();
			}
			return true;
		case R.id.menu_settings:
			Intent prefsIntent = new Intent(this, PrefsActivity.class);
			startActivity(prefsIntent);
			return true;
		}

		return false;
	}

	private void showNetworkErrorMsg() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setMessage(R.string.no_network_msg);
		alert.setTitle("No Network");
		alert.setIcon(android.R.drawable.ic_dialog_alert);
		alert.setNeutralButton(R.string.error_ok, new OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();

			}
		});
		alert.show();
	}

	class NewEventReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {

			String msg = intent
					.getStringExtra(EventManagerService.NEW_EVENT_EXTRA_ERROR);
			if (msg != null) {
				Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
			} else {

				long count = intent.getLongExtra(
						EventManagerService.NEW_EVENT_EXTRA_COUNT, 0);
				if (count > 0) {
					Toast.makeText(
							context,
							"Received " + count + " event"
									+ (count > 1 ? "s!" : "!"),
							Toast.LENGTH_SHORT).show();
					listItems.clear();
					ProcessEventData();
				} else {
					Toast.makeText(context, "No new Events!",
							Toast.LENGTH_SHORT).show();
				}
			}
		}

	}
}
