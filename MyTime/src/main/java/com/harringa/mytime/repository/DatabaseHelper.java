package com.harringa.mytime.repository;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by justinharringa on 8/17/14.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    public static final int VERSION = 1;
    public static final String DATABASE_NAME = "myTime.db";
    public static final String CHECKIN_TABLE = "CheckIn";

    public static final String CHECKIN_ID = "id";
    public static final String CHECKIN_DATETIME = "dateTime";

    private static final String CREATE_CHECKIN = "CREATE TABLE " + CHECKIN_TABLE
            + "("
            + CHECKIN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + CHECKIN_DATETIME + " INTEGER NOT NULL "
            + ");";

    private static final String DROP_CHECKIN = "DROP TABLE IF EXISTS " + CHECKIN_TABLE;
    private static final String TAG = "DatabaseHelper";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, VERSION);
        Log.d(TAG, "DatabaseHelper created");
        context.openOrCreateDatabase(DATABASE_NAME, Context.MODE_PRIVATE, null);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "Creating database");

        db.execSQL(CREATE_CHECKIN);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "Prepping database upgrade");

    }
}
