package com.alzahra.service;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.alzahra.data.DatabaseHelper;

public class NotificationService extends NotificationListenerService {
    private static final String TAG = "NotificationService";
    private DatabaseHelper db;
    
    @Override
    public void onCreate() {
        super.onCreate();
        db = new DatabaseHelper(this);
        Log.d(TAG, "NotificationService created");
    }
    
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        try {
            String packageName = sbn.getPackageName();
            
            // تجاهل إشعارات النظام
            if (isSystemPackage(packageName)) return;
            
            // تجاهل إشعارات التطبيق نفسه
            if (packageName.equals(getPackageName())) return;
            
            String title = extractString(sbn, "android.title");
            String text = extractString(sbn, "android.text");
            
            // تصنيف الإشعار
            String category = categorizePackage(packageName);
            
            // حفظ في قاعدة البيانات
            db.addNotification(
                packageName,
                title != null ? title : "",
                text != null ? text : "",
                category,
                sbn.getPostTime()
            );
            
            Log.d(TAG, "Notification saved: " + category + " - " + title);
            
        } catch (Exception e) {
            Log.e(TAG, "Notification error", e);
        }
    }
    
    private String extractString(StatusBarNotification sbn, String key) {
        try {
            if (sbn.getNotification().extras == null) return null;
            CharSequence cs = sbn.getNotification().extras.getCharSequence(key);
            return cs != null ? cs.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    private boolean isSystemPackage(String pkg) {
        return pkg.equals("android")
            || pkg.equals("com.android.systemui")
            || pkg.equals("com.android.phone")
            || pkg.startsWith("com.android.");
    }
    
    private String categorizePackage(String pkg) {
        String lower = pkg.toLowerCase();
        if (lower.contains("whatsapp")) return "WHATSAPP";
        if (lower.contains("messenger") || lower.contains("orca")) return "MESSENGER";
        if (lower.contains("telegram")) return "TELEGRAM";
        if (lower.contains("instagram")) return "INSTAGRAM";
        if (lower.contains("snapchat")) return "SNAPCHAT";
        if (lower.contains("twitter")) return "TWITTER";
        if (lower.contains("viber")) return "VIBER";
        return "OTHER";
    }
    
    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {}
}
