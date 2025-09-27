package com.tricenc.aftershift;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log; // NEW

import java.util.ArrayList;
import java.util.List;

public class ReportDatabase extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "shop_reports.db";
    private static final int DATABASE_VERSION = 3; // NEW: Increment database version

    private static final String TABLE_REPORTS = "reports";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_DATE = "date"; // Last Edited Date
    private static final String COLUMN_PREVIEW = "preview";
    private static final String COLUMN_DATA = "data";
    private static final String COLUMN_TEMPLATE_ID = "template_id";
    private static final String COLUMN_CREATED_DATE = "created_date"; // NEW

    public ReportDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_REPORTS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_TITLE + " TEXT NOT NULL, " +
                COLUMN_DATE + " TEXT NOT NULL, " +
                COLUMN_PREVIEW + " TEXT, " +
                COLUMN_DATA + " TEXT NOT NULL, " +
                COLUMN_TEMPLATE_ID + " TEXT DEFAULT '" + TemplateManager.DEFAULT_TEMPLATE_ID + "', " +
                COLUMN_CREATED_DATE + " TEXT NOT NULL DEFAULT '01/01/70 00:00')"; // NEW: Added created_date with default
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            // Simple upgrade strategy: drop and recreate.
            // For production, you'd want to use ALTER TABLE to preserve data.
            Log.w("ReportDatabase", "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_REPORTS);
            onCreate(db);
        }
    }

    // NEW: Overload saveReport to include templateId and set createdDate
    public long saveReport(String title, String date, String preview, String data, String templateId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, title);
        values.put(COLUMN_DATE, date); // Last Edited Date
        values.put(COLUMN_PREVIEW, preview);
        values.put(COLUMN_DATA, data);
        values.put(COLUMN_TEMPLATE_ID, templateId);
        values.put(COLUMN_CREATED_DATE, date); // NEW: Set created date on save

        long id = db.insert(TABLE_REPORTS, null, values);
        db.close();
        return id;
    }

    // Existing saveReport (might be called from older code, but new code should use the overloaded one)
    public long saveReport(String title, String date, String preview, String data) {
        return saveReport(title, date, preview, data, TemplateManager.DEFAULT_TEMPLATE_ID); // Default template for compatibility
    }

    // NEW: Overload updateReport to include templateId
    public void updateReport(long id, String title, String date, String preview, String data, String templateId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, title);
        values.put(COLUMN_DATE, date); // Last Edited Date
        values.put(COLUMN_PREVIEW, preview);
        values.put(COLUMN_DATA, data);
        values.put(COLUMN_TEMPLATE_ID, templateId);

        // NOTE: COLUMN_CREATED_DATE is NOT updated here.

        db.update(TABLE_REPORTS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    // Existing updateReport
    public void updateReport(long id, String title, String date, String preview, String data) {
        // Retrieve existing report to get its templateId, or default if it's an old report.
        MainActivity.SavedReport existing = getReport(id);
        String templateId = (existing != null) ? existing.getTemplateId() : TemplateManager.DEFAULT_TEMPLATE_ID;
        updateReport(id, title, date, preview, data, templateId);
    }


    public void deleteReport(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_REPORTS, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public MainActivity.SavedReport getReport(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_REPORTS, null, COLUMN_ID + " = ?",
                new String[]{String.valueOf(id)}, null, null, null);

        if (cursor.moveToFirst()) {
            String templateId;
            int templateIdIndex = cursor.getColumnIndex(COLUMN_TEMPLATE_ID);
            if (templateIdIndex != -1) {
                templateId = cursor.getString(templateIdIndex);
            } else {
                // Handle old reports that don't have a template_id column yet
                templateId = TemplateManager.DEFAULT_TEMPLATE_ID;
            }

            String createdDate;
            int createdDateIndex = cursor.getColumnIndex(COLUMN_CREATED_DATE);
            if (createdDateIndex != -1) {
                createdDate = cursor.getString(createdDateIndex);
            } else {
                // Handle old reports that don't have a created_date column yet, use COLUMN_DATE as fallback
                createdDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE));
            }

            MainActivity.SavedReport report = new MainActivity.SavedReport(
                    cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE)), // Last Edited Date
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PREVIEW)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATA)),
                    templateId,
                    createdDate // NEW
            );
            cursor.close();
            db.close();
            return report;
        }

        cursor.close();
        db.close();
        return null;
    }

    public List<MainActivity.SavedReport> getAllReports() {
        List<MainActivity.SavedReport> reports = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_REPORTS, null, null, null, null, null, COLUMN_DATE + " DESC");

        if (cursor.moveToFirst()) {
            do {
                String templateId;
                int templateIdIndex = cursor.getColumnIndex(COLUMN_TEMPLATE_ID);
                if (templateIdIndex != -1) {
                    templateId = cursor.getString(templateIdIndex);
                } else {
                    // Handle old reports that don't have a template_id column yet
                    templateId = TemplateManager.DEFAULT_TEMPLATE_ID;
                }

                String createdDate;
                int createdDateIndex = cursor.getColumnIndex(COLUMN_CREATED_DATE);
                if (createdDateIndex != -1) {
                    createdDate = cursor.getString(createdDateIndex);
                } else {
                    // Handle old reports that don't have a created_date column yet, use COLUMN_DATE as fallback
                    createdDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE));
                }

                MainActivity.SavedReport report = new MainActivity.SavedReport(
                        cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE)), // Last Edited Date
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PREVIEW)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATA)),
                        templateId,
                        createdDate // NEW
                );
                reports.add(report);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return reports;
    }

    // NEW: Check if any reports are using a specific template
    public boolean hasReportsUsingTemplate(String templateId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        boolean hasReports = false;
        try {
            cursor = db.query(TABLE_REPORTS,
                    new String[]{COLUMN_ID},
                    COLUMN_TEMPLATE_ID + " = ?",
                    new String[]{templateId},
                    null, null, null);
            if (cursor != null && cursor.getCount() > 0) {
                hasReports = true;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return hasReports;
    }
}