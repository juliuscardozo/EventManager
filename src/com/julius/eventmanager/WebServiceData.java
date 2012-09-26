package com.julius.eventmanager;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

public class WebServiceData {

	private final static String TAG = "WebServiceData";
	private final static String EVENT_URL = "content://com.android.calendar/events";
	private final static String REMINDER_URL = "content://com.android.calendar/reminders";
	private final static long MINUTE_IN_MILLISEC = 60 * 1000;
	private long startTime;
	private long eventId;
	private long eventDuration;
	private String eventTitle;
	private String eventDesc;
	private String eventLocation;
	private DateFormat dateFormat = new SimpleDateFormat("MMM dd yyyy hh:mma");
	private boolean validEvent = false;

	private EventManagerApplication application;
	private JSONObject jsonObject;

	public WebServiceData(JSONObject obj, EventManagerApplication app)
			throws JSONException {
		jsonObject = obj;
		application = app;
		EventData eventData = application.getEvenData();

		String strDate = jsonObject.getString("EventStart");
		try {
			dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
			Date d = dateFormat.parse(strDate);
			startTime = d.getTime();

			eventId = jsonObject.getLong("EventId");

			if (eventData.isEventIdPresent(eventId)
					&& d.after(Calendar.getInstance().getTime())) {
				eventTitle = jsonObject.getString("EventTitle");
				eventDesc = jsonObject.getString("EventDescription");
				eventLocation = jsonObject.getString("EventLocation");
				eventDuration = jsonObject.getLong("EventDuration");
				validEvent = true;
			} else {
				Log.w(TAG,
						"Event with id " + eventId + " and start time "
								+ d.toString() + "ignored");
			}

		} catch (ParseException e) {
			Log.e(TAG, e.getMessage());
		}
	}

	public void setCalendarEvent() {
		long calEventID = 0;
		long calendarId = ((EventManagerApplication) application
				.getApplicationContext()).getSelectedCalendarID();

		if (!validEvent)
			return;

		Uri EVENTS_URI = Uri.parse(EVENT_URL);
		ContentResolver cr = application.getContentResolver();

		// event insert
		ContentValues values = new ContentValues();

		values.put("calendar_id", calendarId);
		values.put("title", eventTitle);
		values.put("allDay", 0);
		values.put("eventLocation", eventLocation);
		values.put("dtstart", startTime);
		values.put("dtend", (startTime + (eventDuration * MINUTE_IN_MILLISEC)));
		values.put("description", eventDesc);
		values.put("eventTimezone", "GMT+0:00");
		values.put("selfAttendeeStatus", 1);
		values.put("hasAttendeeData", 1);
		values.put("hasAlarm", 1);
		try {
			Uri event = cr.insert(EVENTS_URI, values);
			calEventID = Long.parseLong(event.getLastPathSegment());
			insertReminders(calEventID);
		} catch (Exception e) {
			Log.e(TAG, "Could not insert Calendar Event!");
			e.printStackTrace();
		}

	}

	public void SendNotification(long calEventID) {
		NotificationManager nm = (NotificationManager) application
				.getApplicationContext().getSystemService(
						Context.NOTIFICATION_SERVICE);
		int icon = R.drawable.ic_launcher;
		CharSequence tickerText = "New Event: " + eventTitle + "!";
		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, tickerText, when);

		Context context = application.getApplicationContext();
		CharSequence contentTitle = eventTitle;
		CharSequence contentText = eventDesc;
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(EVENT_URL + "/" + String.valueOf(calEventID)));
		// Android 2.1 and below.
		// intent.setData(Uri.parse("content://calendar/events/" +
		// String.valueOf(calendarEventID)));
		intent.putExtra("beginTime", startTime);
		intent.putExtra("endTime", startTime
				+ (eventDuration * MINUTE_IN_MILLISEC));
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_SINGLE_TOP
				| Intent.FLAG_ACTIVITY_CLEAR_TOP
				| Intent.FLAG_ACTIVITY_NO_HISTORY
				| Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		PendingIntent contentIntent = PendingIntent.getActivity(application, 0,
				intent, 0);

		notification.setLatestEventInfo(context, contentTitle, contentText,
				contentIntent);
		notification.flags |= Notification.FLAG_AUTO_CANCEL;

		nm.notify((int) calEventID, notification);
	}

	private void insertReminders(long calEventID) {
		Uri REMINDERS_URI = Uri.parse(REMINDER_URL);
		ContentValues values = new ContentValues();
		ContentResolver cr = application.getContentResolver();

		values = new ContentValues();

		try {
			values.put("event_id", calEventID);
			values.put("method", 1);
			values.put("minutes", 1440); // one day prior
			cr.insert(REMINDERS_URI, values);

			values.put("event_id", calEventID);
			values.put("method", 1);
			values.put("minutes", 10080); // 7 days prior
			cr.insert(REMINDERS_URI, values);

			values.put("event_id", calEventID);
			values.put("method", 1);
			values.put("minutes", 21600); // 15 days prior
			cr.insert(REMINDERS_URI, values);

			insertEventDb(calEventID);
			SendNotification(calEventID);

		} catch (Exception e) {
			Log.e(TAG, "Could not insert reminders!");
			e.printStackTrace();
		}

	}

	private void insertEventDb(long calEventID) {
		EventData eventData = application.getEvenData();
		ContentValues values = new ContentValues();
		values.put(EventData.C_ID, eventId);
		values.put(EventData.C_TITLE, eventTitle);
		values.put(EventData.C_DATE, startTime);
		values.put(EventData.C_CAL_EVENTID, calEventID);

		eventData.insertOrIgnore(values);
	}
}
