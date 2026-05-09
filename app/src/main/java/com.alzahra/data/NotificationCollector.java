package com.alzahra.data;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class NotificationCollector {
    private static final String TAG = "NotificationCollector";
    private Context context;

    public NotificationCollector(Context ctx) {
        this.context = ctx;
    }

    public File export() {
        try {
            File sourceFile = new File(context.getFilesDir(), 
                "notifications/notifications.json");
            
            if (!sourceFile.exists()) {
                return null;
            }

            JSONArray notifications = new JSONArray();
            BufferedReader reader = new BufferedReader(new FileReader(sourceFile));
            String line;
            
            while ((line = reader.readLine()) != null) {
                try {
                    JSONObject notif = new JSONObject(line);
                    
                    // Categorize by app
                    String pkg = notif.optString("package", "");
                    if (pkg.contains("whatsapp")) {
                        notif.put("category", "WHATSAPP");
                    } else if (pkg.contains("messenger") || pkg.contains("orca")) {
                        notif.put("category", "MESSENGER");
                    } else {
                        notif.put("category", "OTHER");
                    }
                    
                    notifications.put(notif);
                    
                } catch (Exception e) {
                    Log.e(TAG, "Parse error", e);
                }
            }
            reader.close();

            // Save export
            File dir = new File(context.getFilesDir(), "notifications_export");
            if (!dir.exists()) dir.mkdirs();
            
            File file = new File(dir, "notifications_" + 
                System.currentTimeMillis() + ".json");
            FileWriter writer = new FileWriter(file);
            writer.write(notifications.toString(2));
            writer.close();
            
            // Clear source after export
            sourceFile.delete();
            
            return file;

        } catch (IOException e) {
            Log.e(TAG, "Export error", e);
            return null;
        }
    }
}
