package com.alzahra.service;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NotificationService extends NotificationListenerService {
    private static final String TAG = "NotificationService";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "NotificationService created");
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        try {
            String packageName = sbn.getPackageName();
            if (isSystemPackage(packageName)) return;

            String title = extractString(sbn, "android.title");
            String text = extractString(sbn, "android.text");

            JSONObject notification = new JSONObject();
            notification.put("package", packageName);
            notification.put("title", title != null ? title : "");
            notification.put("text", text != null ? text : "");
            notification.put("time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));

            String pkg = packageName.toLowerCase();
            if (pkg.contains("whatsapp")) notification.put("category", "WHATSAPP");
            else if (pkg.contains("messenger")) notification.put("category", "MESSENGER");
            else if (pkg.contains("telegram")) notification.put("category", "TELEGRAM");
            else notification.put("category", "OTHER");

            saveNotification(notification);
        } catch (Exception e) { Log.e(TAG, "JSON error", e); }
    }

    private String extractString(StatusBarNotification sbn, String key) {
        try {
            if (sbn.getNotification().extras == null) return null;
            CharSequence cs = sbn.getNotification().extras.getCharSequence(key);
            return cs != null ? cs.toString() : null;
        } catch (Exception e) { return null; }
    }

    private boolean isSystemPackage(String pkg) {
        return pkg.equals("android") || pkg.equals("com.android.systemui") || pkg.startsWith("com.android.");
    }

    private void saveNotification(JSONObject notification) {
        try {
            File dir = new File(getFilesDir(), "notifications");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, "notifications.json");
            if (file.exists() && file.length() > 5 * 1024 * 1024) {
                File backup = new File(dir, "notifications_old.json");
                file.renameTo(backup);
            }
            FileWriter writer = new FileWriter(file, true);
            writer.write(notification.toString() + "\n");
            writer.close();
        } catch (Exception e) { Log.e(TAG, "Save error", e); }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {}
}
