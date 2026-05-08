package com.alzahra;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class AlZahraApp extends Application {
    public static final String CHANNEL_ID = "alzahra_service";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "System Service",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Background operations");
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }
}
