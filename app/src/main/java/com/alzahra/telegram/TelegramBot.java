package com.alzahra.telegram;

import android.util.Log;
import org.json.JSONObject;
import java.io.File;
import java.util.concurrent.TimeUnit;
import okhttp3.*;

public class TelegramBot {
    private static final String TAG = "TelegramBot";
    private static final String API_URL = "https://api.telegram.org/bot";
    private final String token;
    private final OkHttpClient client;

    public TelegramBot(String token) {
        this.token = token;
        this.client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();
    }

    public boolean sendMessage(String chatId, String text) {
        for (int i = 0; i < 3; i++) {
            try {
                JSONObject json = new JSONObject();
                json.put("chat_id", chatId);
                json.put("text", text);
                json.put("parse_mode", "HTML");

                RequestBody body = RequestBody.create(
                    json.toString(), MediaType.parse("application/json"));

                Request request = new Request.Builder()
                    .url(API_URL + token + "/sendMessage")
                    .post(body)
                    .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        return new JSONObject(response.body().string()).optBoolean("ok", false);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Send failed: " + e.getMessage());
            }
            try { Thread.sleep(2000); } catch (InterruptedException e) {}
        }
        return false;
    }

    public boolean sendDocument(String chatId, File file, String caption) {
        try {
            MultipartBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", chatId)
                .addFormDataPart("caption", caption)
                .addFormDataPart("document", file.getName(),
                    RequestBody.create(file, MediaType.parse("application/octet-stream")))
                .build();

            Request request = new Request.Builder()
                .url(API_URL + token + "/sendDocument")
                .post(body)
                .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    return new JSONObject(response.body().string()).optBoolean("ok", false);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Send doc failed: " + e.getMessage());
        }
        return false;
    }

    public void getUpdates(long offset, UpdateCallback callback) {
        new Thread(() -> {
            try {
                Request request = new Request.Builder()
                    .url(API_URL + token + "/getUpdates?offset=" + offset + "&limit=20")
                    .build();
                try (Response response = client.newCall(request).execute()) {
                    if (response.body() != null) {
                        callback.onUpdate(new JSONObject(response.body().string()));
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Get updates failed: " + e.getMessage());
            }
        }).start();
    }

    public void sendAlert(String chatId, String title, String message) {
        sendMessage(chatId, "🔔 <b>" + title + "</b>\n\n" + message);
    }

    public interface UpdateCallback {
        void onUpdate(JSONObject updates);
    }
}
