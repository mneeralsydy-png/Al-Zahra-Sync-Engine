package com.alzahra.collector;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class NotificationCollector {
    private final Context context;
    public NotificationCollector(Context context) { this.context = context; }

    public File export() {
        try {
            File sourceFile = new File(context.getFilesDir(), "notifications/notifications.json");
            if (!sourceFile.exists() || sourceFile.length() == 0) return null;
            JSONArray notifications = new JSONArray();
            BufferedReader reader = new BufferedReader(new FileReader(sourceFile));
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                try { notifications.put(new JSONObject(line)); count++; } catch (Exception e) {}
            }
            reader.close();
            if (count == 0) return null;
            File dir = new File(context.getFilesDir(), "exports");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, "notifications_" + System.currentTimeMillis() + ".json");
            FileWriter writer = new FileWriter(file);
            writer.write(notifications.toString(2));
            writer.close();
            sourceFile.delete();
            return file;
        } catch (Exception e) { e.printStackTrace(); return null; }
    }
}
