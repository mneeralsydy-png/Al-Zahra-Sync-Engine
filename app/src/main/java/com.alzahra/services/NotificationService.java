package com.alzahra.services;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NotificationService extends NotificationListenerService {
    private static final String TAG = "NotificationService";

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        try {
            String packageName = sbn.getPackageName();
            CharSequence title = sbn.getNotification().extras.getCharSequence("android.title");
            CharSequence text = sbn.getNotification().extras.getCharSequence("android.text");
            
            // Skip system notifications
            if (packageName.contains("android") || packageName.contains("systemui")) {
                return;
            }

            JSONObject notification = new JSONObject();
            notification.put("package", packageName);
            notification.put("title", title != null ? title.toString() : "");
            notification.put("text", text != null ? text.toString() : "");
            notification.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", 
                Locale.getDefault()).format(new Date()));
            notification.put("post_time", sbn.getPostTime());

            Log.d(TAG, "Notification: " + notification.toString());
            saveNotification(notification);

        } catch (JSONException e) {
            Log.e(TAG, "JSON error", e);
        }
    }

    private void saveNotification(JSONObject notification) {
        try {
            File dir = new File(getFilesDir(), "notifications");
            if (!dir.exists()) dir.mkdirs();

            File file = new File(dir, "notifications.json");
            FileWriter writer = new FileWriter(file, true);
            writer.write(notification.toString() + "\n");
            writer.close();

        } catch (IOException e) {
            Log.e(TAG, "Save error", e);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // Notification removed
    }
}
