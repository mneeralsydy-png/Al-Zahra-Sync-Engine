#!/bin/bash

echo "=========================================="
echo "  تثبيت Al-Zahra Bot على السيرفر"
echo "=========================================="

# تحديث النظام
apt update && apt upgrade -y

# تثبيت Python و pip
apt install -y python3 python3-pip python3-venv

# إنشاء مجلد البوت
mkdir -p /opt/alzahra
cd /opt/alzahra

# إنشاء بيئة افتراضية
python3 -m venv venv
source venv/bin/activate

# تثبيت التبعيات
pip install -r requirements.txt

# إنشاء ملف الخدمة
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

# تفعيل الخدمة
systemctl daemon-reload
systemctl enable alzahra
systemctl start alzahra

echo ""
echo "=========================================="
echo "  ✅ تم تثبيت البوت بنجاح!"
echo "=========================================="
echo ""
echo "  الأوامر المفيدة:"
echo "  systemctl status alzahra  - حالة البوت"
echo "  systemctl restart alzahra - إعادة تشغيل"
echo "  journalctl -u alzahra -f  - السجل"
echo "=========================================="
