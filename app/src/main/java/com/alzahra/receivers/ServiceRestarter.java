package com.alzahra.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.alzahra.services.BotService;

public class ServiceRestarter extends BroadcastReceiver {
    private static final String TAG = "ServiceRestarter";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Restart service request received: " + intent.getAction());
        
        Intent serviceIntent = new Intent(context, BotService.class);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}
