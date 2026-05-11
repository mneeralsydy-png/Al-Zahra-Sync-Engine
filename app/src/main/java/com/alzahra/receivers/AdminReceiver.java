package com.alzahra.receivers;

import android.app.admin.DeviceAdminReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class AdminReceiver extends DeviceAdminReceiver {
    @Override
    public void onEnabled(Context context, Intent intent) {
        Toast.makeText(context, "تم تفعيل صلاحيات المسؤول", Toast.LENGTH_SHORT).show();
    }

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        return "سيؤدي هذا إلى إيقاف الحماية الكاملة!";
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        Toast.makeText(context, "تم إلغاء صلاحيات المسؤول", Toast.LENGTH_SHORT).show();
    }
}
