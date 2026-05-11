package com.alzahra.services;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.os.Bundle;
import android.util.Log;

public class NotificationService extends NotificationListenerService {
    private static final String TAG = "NotificationService";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Notification service created");
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();
        String title = sbn.getNotification().extras != null ? 
            sbn.getNotification().extras.getString("android.title") : "Unknown";
        String text = sbn.getNotification().extras != null ?
            sbn.getNotification().extras.getCharSequence("android.text", "Empty").toString() : "Empty";

        Log.d(TAG, String.format("Package: %s, Title: %s, Text: %s", packageName, title, text));
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.d(TAG, "Notification removed: " + sbn.getPackageName());
    }
}
