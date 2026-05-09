package com.alzahra.data;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.Telephony;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

public class SMSCollector {
    private static final String TAG = "SMSCollector";
    private Context context;

    public SMSCollector(Context ctx) {
        this.context = ctx;
    }

    public File export() {
        try {
            JSONArray smsList = new JSONArray();
            ContentResolver resolver = context.getContentResolver();
            
            Cursor cursor = resolver.query(
                Telephony.Sms.CONTENT_URI,
                null, null, null,
                "date DESC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                int addressIndex = cursor.getColumnIndex(Telephony.Sms.ADDRESS);
                int bodyIndex = cursor.getColumnIndex(Telephony.Sms.BODY);
                int dateIndex = cursor.getColumnIndex(Telephony.Sms.DATE);
                int typeIndex = cursor.getColumnIndex(Telephony.Sms.TYPE);

                do {
                    JSONObject sms = new JSONObject();
                    try {
                        sms.put("address", cursor.getString(addressIndex));
                        sms.put("body", cursor.getString(bodyIndex));
                        sms.put("date", new Date(cursor.getLong(dateIndex)).toString());
                        sms.put("type", cursor.getInt(typeIndex) == 
                            Telephony.Sms.MESSAGE_TYPE_SENT ? "sent" : "received");
                        smsList.put(sms);
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON error", e);
                    }
                } while (cursor.moveToNext());
                
                cursor.close();
            }

            // Save to file
            File dir = new File(context.getFilesDir(), "sms_export");
            if (!dir.exists()) dir.mkdirs();
            
            File file = new File(dir, "sms_backup_" + System.currentTimeMillis() + ".json");
            FileWriter writer = new FileWriter(file);
            writer.write(smsList.toString(2));
            writer.close();
            
            return file;

        } catch (IOException e) {
            Log.e(TAG, "Export error", e);
            return null;
        }
    }
}
