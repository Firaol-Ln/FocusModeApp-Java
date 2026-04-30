package com.example.focusmodejv.data;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "FocusApp.db";
    private static final int DATABASE_VERSION = 2;

    private static final String TABLE_SESSIONS = "sessions";
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_DURATION_MS = "duration_ms";
    private static final String COLUMN_START_TIME = "start_time";
    private static final String COLUMN_END_TIME = "end_time";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_SESSIONS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_START_TIME + " INTEGER, " +
                COLUMN_END_TIME + " INTEGER, " +
                COLUMN_DURATION_MS + " INTEGER" +
                ")";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SESSIONS);
        onCreate(db);
    }

    // Insert a new completed session
    public void addSession(long startTimeMs, long endTimeMs, long durationMs) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_START_TIME, startTimeMs);
        values.put(COLUMN_END_TIME, endTimeMs);
        values.put(COLUMN_DURATION_MS, durationMs);
        db.insert(TABLE_SESSIONS, null, values);
        db.close();
    }

    // Get total duration for a given time range
    public long getTotalDurationInRange(long startTimestampMs, long endTimestampMs) {
        SQLiteDatabase db = this.getReadableDatabase();
        long totalDuration = 0;

        String query = "SELECT SUM(" + COLUMN_DURATION_MS + ") FROM " + TABLE_SESSIONS +
                " WHERE " + COLUMN_END_TIME + " >= ? AND " + COLUMN_END_TIME + " <= ?";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(startTimestampMs), String.valueOf(endTimestampMs)});

        if (cursor.moveToFirst()) {
            totalDuration = cursor.getLong(0);
        }
        cursor.close();
        db.close();

        return totalDuration;
    }

    // Get all sessions within a time range
    @SuppressLint("Range")
    public List<Session> getSessionsInRange(long startTimestampMs, long endTimestampMs) {
        List<Session> sessions = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT * FROM " + TABLE_SESSIONS +
                " WHERE " + COLUMN_END_TIME + " >= ? AND " + COLUMN_END_TIME + " <= ? ORDER BY " + COLUMN_END_TIME + " ASC";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(startTimestampMs), String.valueOf(endTimestampMs)});

        if (cursor.moveToFirst()) {
            do {
                Session session = new Session(
                        cursor.getInt(cursor.getColumnIndex(COLUMN_ID)),
                        cursor.getLong(cursor.getColumnIndex(COLUMN_START_TIME)),
                        cursor.getLong(cursor.getColumnIndex(COLUMN_END_TIME)),
                        cursor.getLong(cursor.getColumnIndex(COLUMN_DURATION_MS))
                );
                sessions.add(session);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();

        return sessions;
    }

    // Clear all sessions
    public void clearAllSessions() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SESSIONS, null, null);
        db.close();
    }

    // Session Data Model
    public static class Session {
        public int id;
        public long startTimeMs;
        public long endTimeMs;
        public long durationMs;

        public Session(int id, long startTimeMs, long endTimeMs, long durationMs) {
            this.id = id;
            this.startTimeMs = startTimeMs;
            this.endTimeMs = endTimeMs;
            this.durationMs = durationMs;
        }
    }
}