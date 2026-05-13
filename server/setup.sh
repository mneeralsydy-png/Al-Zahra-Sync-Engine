#!/bin/bash
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
