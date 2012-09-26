package com.julius.eventmanager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class EventData {

	private static final String TAG = "EventData";
	private static final int VERSION = 1;
	private static final String DATABASE = "events.db";
	private static final String TABLE = "events";

	public final static String C_ID = "_id";
	public final static String C_TITLE = "title";
	public final static String C_CAL_EVENTID = "cal_eventid";
	public final static String C_DATE = "start_date";

	private static final String[] COUNT_ID = { "count(" + C_ID + ") " };

	private static final String[] DB_ID_COLUMN = { C_ID };

	private static final String[] DB_ALL_COLUMNS = { C_CAL_EVENTID, C_TITLE, C_DATE };

	final DataHelper dbHelper;

	public EventData(Context context) {
		this.dbHelper = new DataHelper(context);
		Log.i(TAG, "Initialized data");
	}

	public void close() {
		this.dbHelper.close();
	}

	public long getRowCount() {
		long count = 0;
		SQLiteDatabase db = this.dbHelper.getReadableDatabase();
		try {
			Cursor cursor = db.query(TABLE, COUNT_ID, null, null, null, null,
					null);
			try {
				if (cursor.moveToNext())
					count = cursor.getLong(0);
			} finally {
				cursor.close();
			}
		} finally {
			db.close();
		}
		return count;
	}

	public Cursor getAllEvents() {
		SQLiteDatabase db = this.dbHelper.getReadableDatabase();
		Cursor cursor = db.query(TABLE, DB_ALL_COLUMNS, null,
				null, null, null, null);
		return cursor;
	}

	public boolean isEventIdPresent(long eventId) {
		SQLiteDatabase db = this.dbHelper.getReadableDatabase();
		Cursor cursor = db.query(TABLE, DB_ID_COLUMN, C_ID + "=" + eventId,
				null, null, null, null);
		return !cursor.moveToNext();
	}

	public void insertOrIgnore(ContentValues values) {
		Log.d(TAG, "insertOrIgnore on " + values);
		SQLiteDatabase db = this.dbHelper.getWritableDatabase();
		try {
			db.insertWithOnConflict(TABLE, null, values,
					SQLiteDatabase.CONFLICT_IGNORE);
		} finally {
			db.close();
		}
	}

	public void delete() {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		db.delete(TABLE, null, null);
		db.close();
	}

	class DataHelper extends SQLiteOpenHelper {

		public DataHelper(Context context) {
			super(context, DATABASE, null, VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			Log.i(TAG, "Creating database...");
			String strQuery = "create table " + TABLE + " (" + C_ID
					+ " integer primary key, " + C_TITLE + " text, "
					+ C_CAL_EVENTID + " text, " + C_DATE + " integer)";

			db.execSQL(strQuery);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("drop table " + TABLE);
			this.onCreate(db);
		}

	}
}
