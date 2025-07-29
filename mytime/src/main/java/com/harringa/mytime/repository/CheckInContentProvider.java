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

    public static CheckInContentProvider getInstance(Context context) {
        if (instance == null) {
            Log.d(TAG, "New instance created");
            instance = new CheckInContentProvider(context);
        }
        return instance;
    }

    public void saveCheckIn(LocalDateTime checkIn) {
        Log.d(TAG, "saveCheckIn()");
        ContentValues contentValues = new ContentValues();
        long millis = checkIn.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        contentValues.put(DatabaseHelper.CHECKIN_DATETIME, millis);
        Log.d(TAG, "millis: " + millis);
        Log.d(TAG, "date: " + checkIn.getYear() + "-" + checkIn.getMonthValue() + "-" + checkIn.getDayOfMonth());

        getConnection().insert(DatabaseHelper.CHECKIN_TABLE, "", contentValues);
        closeConnection();

    }

    public List<LocalDateTime> getAll() {
        Log.d(TAG, "getAll()");
        Cursor cursor = getConnection().query(DatabaseHelper.CHECKIN_TABLE,
                new String[]{DatabaseHelper.CHECKIN_ID, DatabaseHelper.CHECKIN_DATETIME},
                null,
                null,
                null,
                null,
                DatabaseHelper.CHECKIN_DATETIME + " DESC");

        List<LocalDateTime> dateTimes = getCheckIns(cursor);

        closeConnection();

        return dateTimes;
    }

    private List<LocalDateTime> getCheckIns(Cursor cursor) {
        List<LocalDateTime> dateTimes = new ArrayList<>();
        if (cursor.moveToFirst()) {

            do {
                long dateTime = cursor.getLong(cursor
                        .getColumnIndex(DatabaseHelper.CHECKIN_DATETIME));
                Log.d(TAG, "dateTime: " + dateTime);

                LocalDateTime localDateTime = Instant.ofEpochMilli(dateTime)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();
                dateTimes.add(localDateTime);

            } while (cursor.moveToNext());
        }

        return dateTimes;
    }

    private CheckInContentProvider(Context context) {
        databaseHelper = new DatabaseHelper(context);
    }

    private SQLiteDatabase getConnection() {
        return databaseHelper.getWritableDatabase();
    }

    private void closeConnection() {
        databaseHelper.close();
    }
}
