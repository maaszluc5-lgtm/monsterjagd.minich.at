#!/bin/bash
# Setup script for monsterjagd.minich.at
# Run this as root on the server

set -e

echo "=== Installing Node.js ==="
curl -fsSL https://deb.nodesource.com/setup_20.x | bash -
apt-get install -y nodejs

echo "=== Creating bot user ==="
useradd -m -s /bin/bash mcbot 2>/dev/null || true

echo "=== Setting up bot directory ==="
mkdir -p /opt/monsterjagd
cp -r . /opt/monsterjagd/
cd /opt/monsterjagd
npm install --production
chown -R mcbot:mcbot /opt/monsterjagd

echo "=== Installing systemd service ==="
cp deploy/monsterjagd-bot.service /etc/systemd/system/
systemctl daemon-reload
systemctl enable monsterjagd-bot

echo ""
echo "FERTIG! Jetzt:"
echo "1. Trage deine Werte in /opt/monsterjagd/.env ein"
echo "2. Fuehre aus: node /opt/monsterjagd/register-commands.js"
echo "3. Starte: systemctl start monsterjagd-bot"
echo "4. Logs: journalctl -u monsterjagd-bot -f"
