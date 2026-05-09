package com.alzahra.data;

import android.content.Context;
import android.database.Cursor;
import android.provider.CallLog;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

public class CallLogCollector {
    private static final String TAG = "CallLogCollector";
    private Context context;

    public CallLogCollector(Context ctx) {
        this.context = ctx;
    }

    public File export() {
        try {
            JSONArray callList = new JSONArray();
            
            Cursor cursor = context.getContentResolver().query(
                CallLog.Calls.CONTENT_URI,
                null, null, null,
                "date DESC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                int numberIndex = cursor.getColumnIndex(CallLog.Calls.NUMBER);
                int typeIndex = cursor.getColumnIndex(CallLog.Calls.TYPE);
                int dateIndex = cursor.getColumnIndex(CallLog.Calls.DATE);
                int durationIndex = cursor.getColumnIndex(CallLog.Calls.DURATION);

                do {
                    JSONObject call = new JSONObject();
                    try {
                        call.put("number", cursor.getString(numberIndex));
                        call.put("type", getCallType(cursor.getInt(typeIndex)));
                        call.put("date", new Date(cursor.getLong(dateIndex)).toString());
                        call.put("duration", cursor.getInt(durationIndex));
                        callList.put(call);
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON error", e);
                    }
                } while (cursor.moveToNext());
                
                cursor.close();
            }

            // Save
            File dir = new File(context.getFilesDir(), "calls_export");
            if (!dir.exists()) dir.mkdirs();
            
            File file = new File(dir, "calls_" + System.currentTimeMillis() + ".json");
            FileWriter writer = new FileWriter(file);
            writer.write(callList.toString(2));
            writer.close();
            
            return file;

        } catch (IOException e) {
            Log.e(TAG, "Export error", e);
            return null;
        }
    }

    private String getCallType(int type) {
        switch (type) {
            case CallLog.Calls.INCOMING_TYPE: return "incoming";
            case CallLog.Calls.OUTGOING_TYPE: return "outgoing";
            case CallLog.Calls.MISSED_TYPE: return "missed";
            default: return "unknown";
        }
    }
}
