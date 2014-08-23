package com.harringa.mytime.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.harringa.mytime.model.DatabaseHelper;
import org.joda.time.DateTime;
import org.joda.time.Instant;

import java.util.ArrayList;
import java.util.List;

public class CheckInRepository {

    private static final String TAG = "CheckInRepository";
    private DatabaseHelper databaseHelper;
    private static CheckInRepository instance;

    public static CheckInRepository getInstance(Context context) {
        if (instance == null) {
            Log.d(TAG, "New instance created");
            instance = new CheckInRepository(context);
        }
        return instance;
    }

    public void saveCheckIn(DateTime checkIn) {
        Log.d(TAG, "saveCheckIn()");
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHelper.CHECKIN_DATETIME, checkIn.getMillis());
        Log.d(TAG, "millis: " + checkIn.getMillis());
        Log.d(TAG, "date: " + checkIn.getYear() + "-" + checkIn.getMonthOfYear() + "-" + checkIn.getDayOfMonth());

        getConnection().insert(DatabaseHelper.CHECKIN_TABLE, "", contentValues);
        closeConnection();

    }

    public List<Instant> getAll() {
        Log.d(TAG, "getAll()");
        Cursor cursor = getConnection().query(DatabaseHelper.CHECKIN_TABLE,
                new String[]{DatabaseHelper.CHECKIN_ID, DatabaseHelper.CHECKIN_DATETIME},
                null,
                null,
                null,
                null,
                DatabaseHelper.CHECKIN_DATETIME);

        List<Instant> instants = getCheckIns(cursor);

        closeConnection();

        return instants;
    }

    private List<Instant> getCheckIns(Cursor cursor) {
        List<Instant> instants = new ArrayList<Instant>();
        if (cursor.moveToFirst()) {

            do {
                long dateTime = cursor.getLong(cursor
                        .getColumnIndex(DatabaseHelper.CHECKIN_DATETIME));
                Log.d(TAG, "dateTime: " + dateTime);

                instants.add(new Instant(dateTime));

            } while (cursor.moveToNext());
        }

        return instants;
    }

    private CheckInRepository(Context context) {
        databaseHelper = new DatabaseHelper(context);
    }

    private SQLiteDatabase getConnection() {
        return databaseHelper.getWritableDatabase();
    }

    private void closeConnection() {
        databaseHelper.close();
    }
}
