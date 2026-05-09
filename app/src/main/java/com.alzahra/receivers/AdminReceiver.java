package com.alzahra.receivers;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class AdminReceiver extends DeviceAdminReceiver {
    
    @Override
    public void onEnabled(Context context, Intent intent) {
        super.onEnabled(context, intent);
        // Admin enabled
    }

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        return "سيؤدي تعطيل الخدمة إلى إيقاف الحماية. هل أنت متأكد؟";
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        super.onDisabled(context, intent);
        // Admin disabled
    }
}
