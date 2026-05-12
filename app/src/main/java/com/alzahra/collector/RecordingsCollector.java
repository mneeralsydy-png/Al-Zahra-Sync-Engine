package com.alzahra.collector;

import android.content.Context;
import android.util.Log;

import java.io.File;

public class RecordingsCollector {
    private static final String TAG = "RecordingsCollector";
    private final Context context;

    public RecordingsCollector(Context context) {
        this.context = context;
    }

    public File[] getRecordings() {
        try {
            File dir = new File(context.getFilesDir(), "recordings");
            if (dir.exists() && dir.isDirectory()) {
                return dir.listFiles();
            }
        } catch (Exception e) {
            Log.e(TAG, "Get recordings error", e);
        }
        return null;
    }
}
