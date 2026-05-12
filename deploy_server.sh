#!/bin/bash

SERVER_IP="216.128.156.226"
SERVER_USER="root"
SERVER_PASS="E%t7SBQUAL2SE[kc"
BOT_TOKEN="8767989892:AAFCB-gylVjbrB0X6gk95G8rCn6_ds5e9As"
CHAT_ID="7344776596"

echo "=========================================="
echo "  رفع البوت على السيرفر"
echo "=========================================="

# تثبيت sshpass إذا لم يكن موجودا
if ! command -v sshpass &> /dev/null; then
    pkg install -y sshpass 2>/dev/null || echo "يرجى تثبيت sshpass يدويا"
fi

# نسخ الملفات للسيرفر
echo "[1/3] نسخ الملفات..."
sshpass -p "$SERVER_PASS" scp -o StrictHostKeyChecking=no server/bot.py ${SERVER_USER}@${SERVER_IP}:/opt/alzahra/bot.py
sshpass -p "$SERVER_PASS" scp -o StrictHostKeyChecking=no server/requirements.txt ${SERVER_USER}@${SERVER_IP}:/opt/alzahra/requirements.txt
sshpass -p "$SERVER_PASS" scp -o StrictHostKeyChecking=no server/setup.sh ${SERVER_USER}@${SERVER_IP}:/opt/alzahra/setup.sh

# تعيين التوكن
echo "[2/3] تعيين التوكن..."
sshpass -p "$SERVER_PASS" ssh -o StrictHostKeyChecking=no ${SERVER_USER}@${SERVER_IP} "sed -i 's|PLACEHOLDER_BOT_TOKEN|${BOT_TOKEN}|g' /opt/alzahra/bot.py"
sshpass -p "$SERVER_PASS" ssh -o StrictHostKeyChecking=no ${SERVER_USER}@${SERVER_IP} "sed -i 's|PLACEHOLDER_CHAT_ID|${CHAT_ID}|g' /opt/alzahra/bot.py"

# تثبيت وتشغيل
echo "[3/3] تثبيت وتشغيل البوت..."
sshpass -p "$SERVER_PASS" ssh -o StrictHostKeyChecking=no ${SERVER_USER}@${SERVER_IP} "bash /opt/alzahra/setup.sh"

echo ""
echo "=========================================="
echo "  ✅ تم رفع البوت على السيرفر!"
echo "=========================================="
echo ""
echo "  للتحقق من الحالة:"
echo "  ssh root@${SERVER_IP} 'systemctl status alzahra'"
echo "=========================================="
