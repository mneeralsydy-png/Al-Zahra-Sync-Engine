package com.alzahra.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.alzahra.services.TelegramSyncService;

public class NetworkReceiver extends BroadcastReceiver {
    
    private static boolean wasConnected = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnected();
        
        if (isConnected && !wasConnected) {
            // Network became available - trigger auto sync
            Intent syncIntent = new Intent(context, TelegramSyncService.class);
            syncIntent.setAction(TelegramSyncService.ACTION_AUTO_SYNC);
            context.startService(syncIntent);
        }
        
        wasConnected = isConnected;
    }
}
