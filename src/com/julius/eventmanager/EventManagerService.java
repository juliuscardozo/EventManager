package com.julius.eventmanager;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class EventManagerService extends IntentService {

	private final static String TAG = "EventManagerService";
	private final static String JSONTAG = "JSON";
	private final static String WEB_METHOD = "/EventDataService.asmx/GetEventsData";
	public static final String NEW_EVENT_INTENT = "com.julius.eventmanager.NEW_EVENT";
	public static final String NEW_EVENT_EXTRA_COUNT = "NEW_EVENT_EXTRA_COUNT";
	public static final String NEW_EVENT_EXTRA_ERROR = "NEW_EVENT_EXTRA_ERROR";

	private String hostName;
	private long port;

	public EventManagerService() {
		super(TAG);
		Log.i(TAG, "Constructed!");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		String data = null;

		Log.i(TAG, "Starting Service");
		Intent i = new Intent(NEW_EVENT_INTENT);

		EventManagerApplication application = ((EventManagerApplication) getApplication());
		hostName = application.getSharedPrefs().getString(
				getString(R.string.pref_hostname), "192.168.1.2");
		port = Long.parseLong(application.getSharedPrefs().getString(
				getString(R.string.prefs_port), "50305"));

		data = FetchDataFromWebService("http://" + hostName + ":" + port
				+ WEB_METHOD);

		if (data != null) {
			long oldRowCount = application.getEvenData().getRowCount();

			ProcessJsonData(data);

			long newRowCount = application.getEvenData().getRowCount();
			long newRows = newRowCount - oldRowCount;

			if (newRows > 0) {
				Log.i(TAG, "Recieved " + newRows + " new events");
			} else {
				newRows = 0;
				Log.i(TAG, "No events added!");
			}

			i.putExtra(NEW_EVENT_EXTRA_COUNT, newRows);
		}
		else {
			i.putExtra(NEW_EVENT_EXTRA_ERROR, "Error in Webservice connection!");
		}
		sendBroadcast(i, getString(R.string.RECEIVE_EVENT_PERMISSION));
	}

	private void AddCalendarEntry(JSONArray jarray) throws JSONException {

		for (int i = 0; i < jarray.length(); i++) {
			WebServiceData webData = new WebServiceData(
					jarray.getJSONObject(i),
					((EventManagerApplication) getApplication()));
			webData.setCalendarEvent();
		}
	}

	private void ProcessJsonData(String data) {
		try {
			JSONObject json = new JSONObject(data);
			JSONArray jarray = json.optJSONArray("d");

			if (jarray != null) {
				AddCalendarEntry(jarray);
			}

		} catch (JSONException e) {
			Log.e(JSONTAG, e.getMessage());
		}
	}

	private String FetchDataFromWebService(String url) {
		InputStream is = null;
		String result = null;

		try {
			HttpClient httpClient = new DefaultHttpClient();
			HttpHost httpHost = new HttpHost(hostName, (int) port);
			HttpPost httpPost = new HttpPost(url);
			httpPost.addHeader("Content-Type",
					"application/json; charset=utf-8");
			HttpResponse response = httpClient.execute(httpHost, httpPost);
			HttpEntity entity = response.getEntity();
			is = entity.getContent();
		} catch (Exception e) {
			Log.e(JSONTAG, e.getMessage());
			return result;
		}

		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					is, "iso-8859-1"), 8);
			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
			is.close();
			result = sb.toString();
			Log.i(JSONTAG, result);
		} catch (Exception e) {
			Log.e(JSONTAG, "Error converting result " + e.toString());
			result = null;
		}

		return result;
	}
}
