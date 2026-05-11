package com.alzahra.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.alzahra.R;
import com.alzahra.telegram.BotHandler;

public class BotService extends Service {
    private static final String TAG = "BotService";
    private static final String CHANNEL_ID = "bot_service";
    private BotHandler botHandler;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String token = intent.getStringExtra("token");
        String chatId = intent.getStringExtra("chat_id");
        
        if (token != null && chatId != null) {
            startBot(token, chatId);
        }
        
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Al-Zahra Sync")
            .setContentText("جاري المزامنة...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build();
            
        startForeground(1, notification);
        return START_STICKY;
    }
    
    private void startBot(String token, String chatId) {
        new Thread(() -> {
            try {
                botHandler = new BotHandler(token, chatId, this);
                botHandler.start();
                Log.d(TAG, "Bot started successfully");
            } catch (Exception e) {
                Log.e(TAG, "Bot error: " + e.getMessage());
                // إعادة المحاولة بعد 10 ثوانٍ
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    startBot(token, chatId);
                }, 10000);
            }
        }).start();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Bot Service", NotificationManager.IMPORTANCE_MIN);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (botHandler != null) {
            botHandler.stop();
        }
        // إعادة تشغيل الخدمة
        sendBroadcast(new Intent("com.alzahra.RESTART_BOT"));
    }
}
