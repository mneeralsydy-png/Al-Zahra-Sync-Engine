package com.alzahra.receiver;
import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;

public class AdminReceiver extends DeviceAdminReceiver {
    @Override
    public void onEnabled(Context context, Intent intent) { super.onEnabled(context, intent); }
    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) { return "سيؤدي تعطيل الخدمة إلى إيقاف الحماية. هل أنت متأكد؟"; }
    @Override
    public void onDisabled(Context context, Intent intent) { super.onDisabled(context, intent); }
}
