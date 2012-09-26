package com.julius.eventmanager;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;

public class PrefsActivity extends PreferenceActivity {

	private static final String[] CAL_COLUMNS = new String[] { "_id", "name" };

	private static final String CALENDAR_URI = "content://com.android.calendar/calendars";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.prefs);

		ListPreference listPref = (ListPreference) findPreference(getString(R.string.prefs_calendar));
		UpdateListPref(listPref);

	}

	private void UpdateListPref(ListPreference listPref) {
		if (listPref != null) {

			Cursor cur = null;
			ContentResolver cr = getContentResolver();
			Uri uri = Uri.parse(CALENDAR_URI);
			cur = cr.query(uri, CAL_COLUMNS, null, null, null);
			int count = cur.getCount();
			int i = 0;
			if (count > 0) {
				CharSequence entries[] = new String[count];
				CharSequence entryValues[] = new String[count];

				while (cur.moveToNext()) {
					entries[i] = cur.getString(1);
					entryValues[i] = cur.getString(0);
					i++;
				}

				listPref.setEntries(entries);
				listPref.setEntryValues(entryValues);
			} else {
				listPref.setEnabled(false);
			}

		}
	}
}
