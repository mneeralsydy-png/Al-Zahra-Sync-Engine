package com.alzahra.telegram;

import android.util.Log;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class TelegramAPI {
    private static final String TAG = "TelegramAPI";

    public static void sendMessage(String botToken, String chatId, String message) {
        new Thread(() -> {
            try {
                String urlStr = "https://api.telegram.org/bot" + botToken + "/sendMessage";
                String params = "chat_id=" + chatId + 
                                "&text=" + URLEncoder.encode(message, "UTF-8") +
                                "&parse_mode=HTML";
                
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                
                conn.getOutputStream().write(params.getBytes("UTF-8"));
                conn.getOutputStream().flush();
                conn.getOutputStream().close();
                
                int response = conn.getResponseCode();
                Log.d(TAG, "Message sent, response: " + response);
                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Send error: " + e.getMessage());
            }
        }).start();
    }
}
