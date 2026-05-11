package com.alzahra.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.alzahra.services.BotService;
import com.alzahra.services.CoreService;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "=== Boot Completed - Starting Services ===");
            
            // بدء الخدمة الأساسية
            context.startService(new Intent(context, CoreService.class));
            
            // بدء خدمة البوت
            Intent botIntent = new Intent(context, BotService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(botIntent);
            } else {
                context.startService(botIntent);
            }
        }
    }
}
