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
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();
    }

    public boolean sendMessage(String chatId, String text) {
        int attempts = 0;
        while (attempts < 3) {
            try {
                String url = API_URL + token + "/sendMessage";
                
                JSONObject json = new JSONObject();
                json.put("chat_id", chatId);
                json.put("text", text);
                json.put("parse_mode", "HTML");
                
                RequestBody body = RequestBody.create(
                    json.toString(), MediaType.parse("application/json"));
                
                Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .header("Content-Type", "application/json")
                    .build();
                
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        JSONObject resp = new JSONObject(response.body().string());
                        return resp.optBoolean("ok", false);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Send attempt " + (attempts + 1) + " failed");
            }
            
            attempts++;
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                break;
            }
        }
        return false;
    }

    public boolean sendDocument(String chatId, File document, String caption) {
        int attempts = 0;
        while (attempts < 3) {
            try {
                String url = API_URL + token + "/sendDocument";
                
                RequestBody fileBody = RequestBody.create(
                    document, MediaType.parse("application/octet-stream"));
                
                MultipartBody body = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("chat_id", chatId)
                    .addFormDataPart("caption", caption != null ? caption : "")
                    .addFormDataPart("document", document.getName(), fileBody)
                    .build();
                
                Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();
                
                try (Response response = client.newCall(request).execute()) {
                    boolean success = response.isSuccessful();
                    if (success && response.body() != null) {
                        success = new JSONObject(response.body().string()).optBoolean("ok", false);
                    }
                    return success;
                }
            } catch (Exception e) {
                Log.e(TAG, "Document send failed", e);
            }
            
            attempts++;
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                break;
            }
        }
        return false;
    }

    public void getUpdates(long offset, UpdateCallback callback) {
        new Thread(() -> {
            try {
                String url = API_URL + token + "/getUpdates?offset=" + offset + "&limit=20";
                
                Request request = new Request.Builder().url(url).build();
                try (Response response = client.newCall(request).execute()) {
                    if (response.body() != null) {
                        callback.onUpdate(new JSONObject(response.body().string()));
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Get updates failed", e);
            }
        }).start();
    }

    public interface UpdateCallback {
        void onUpdate(JSONObject updates);
    }
}
