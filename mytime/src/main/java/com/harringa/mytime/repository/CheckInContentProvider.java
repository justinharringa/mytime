package com.harringa.mytime.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class CheckInContentProvider {

    private static final String TAG = "CheckInContentProvider";
    private DatabaseHelper databaseHelper;
    private static CheckInContentProvider instance;
    private SQLiteDatabase database;

    public static CheckInContentProvider getInstance(Context context) {
        if (instance == null) {
            Log.d(TAG, "New instance created");
            instance = new CheckInContentProvider(context);
        }
        return instance;
    }

    public boolean saveCheckIn(LocalDateTime checkIn) {
        Log.d(TAG, "saveCheckIn()");

        // Check for duplicate check-in within 1 minute
        if (isDuplicateCheckIn(checkIn)) {
            Log.d(TAG, "Duplicate check-in detected, skipping");
            return false;
        }

        ContentValues contentValues = new ContentValues();
        long millis = checkIn.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        contentValues.put(DatabaseHelper.CHECKIN_DATETIME, millis);
        Log.d(TAG, "millis: " + millis);
        Log.d(TAG, "date: " + checkIn.getYear() + "-" + checkIn.getMonthValue() + "-" + checkIn.getDayOfMonth());

        SQLiteDatabase db = getDatabase();
        db.beginTransaction();
        try {
            db.insert(DatabaseHelper.CHECKIN_TABLE, "", contentValues);
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    private boolean isDuplicateCheckIn(LocalDateTime checkIn) {
        SQLiteDatabase db = getDatabase();
        long checkInMillis = checkIn.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long oneMinuteInMillis = 60000; // 1 minute in milliseconds

        String selection = DatabaseHelper.CHECKIN_DATETIME + " BETWEEN ? AND ?";
        String[] selectionArgs = {
            String.valueOf(checkInMillis - oneMinuteInMillis),
            String.valueOf(checkInMillis + oneMinuteInMillis)
        };

        Cursor cursor = null;
        try {
            cursor = db.query(DatabaseHelper.CHECKIN_TABLE,
                    new String[]{DatabaseHelper.CHECKIN_ID},
                    selection,
                    selectionArgs,
                    null,
                    null,
                    null);

            return cursor.getCount() > 0;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public List<LocalDateTime> getAll() {
        Log.d(TAG, "getAll()");
        SQLiteDatabase db = getDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(DatabaseHelper.CHECKIN_TABLE,
                    new String[]{DatabaseHelper.CHECKIN_ID, DatabaseHelper.CHECKIN_DATETIME},
                    null,
                    null,
                    null,
                    null,
                    DatabaseHelper.CHECKIN_DATETIME + " DESC");

            return getCheckIns(cursor);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private List<LocalDateTime> getCheckIns(Cursor cursor) {
        List<LocalDateTime> dateTimes = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                int columnIndex = cursor.getColumnIndex(DatabaseHelper.CHECKIN_DATETIME);
                if (columnIndex >= 0) {
                    long dateTime = cursor.getLong(columnIndex);
                    Log.d(TAG, "dateTime: " + dateTime);

                    LocalDateTime localDateTime = Instant.ofEpochMilli(dateTime)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime();
                    dateTimes.add(localDateTime);
                } else {
                    Log.e(TAG, "Column " + DatabaseHelper.CHECKIN_DATETIME + " not found in cursor");
                }

            } while (cursor.moveToNext());
        }

        return dateTimes;
    }

    private CheckInContentProvider(Context context) {
        databaseHelper = new DatabaseHelper(context);
    }

    private SQLiteDatabase getDatabase() {
        if (database == null || !database.isOpen()) {
            database = databaseHelper.getWritableDatabase();
        }
        return database;
    }

    public void close() {
        if (database != null && database.isOpen()) {
            database.close();
            database = null;
        }
        if (databaseHelper != null) {
            databaseHelper.close();
            databaseHelper = null;
        }
    }
}
