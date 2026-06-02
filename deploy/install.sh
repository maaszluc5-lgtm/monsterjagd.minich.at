#!/bin/bash
# Monsterjagd - Vollständiges Setup Script
# Ausführen als root auf dem Server: bash <(curl -s https://raw.githubusercontent.com/maaszluc5-lgtm/monsterjagd.minich.at/claude/trusting-brahmagupta-uJfP6/deploy/install.sh)

set -e

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'
log() { echo -e "${GREEN}[SETUP]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

echo -e "${BLUE}"
echo "  __  __                 _            _                 _  "
echo " |  \/  | ___  _ __  __| |_ ___ _ __(_) __ _  __ _  __| | "
echo " | |\/| |/ _ \| '_ \/ _\` | '__| '__| |/ _\` |/ _\` |/ _\` | "
echo " | |  | | (_) | | | \__,_|_|  |_|  |_|\__,_|\__, |\__,_| "
echo " |_|  |_|\___/|_| |_\__,_|_|  |_|  |_|      |___/       "
echo -e "${NC}"
echo "════════════════════════════════════════"
echo "  Monsterjagd Server - Auto Setup"
echo "════════════════════════════════════════"
echo ""

# ── 1. System Update & Dependencies ──────────────────────────────────────────
log "System wird aktualisiert..."
apt-get update -qq

log "Node.js 20 wird installiert..."
curl -fsSL https://deb.nodesource.com/setup_20.x | bash - > /dev/null 2>&1
apt-get install -y nodejs git curl wget unzip > /dev/null 2>&1

log "Java 21 wird installiert..."
apt-get install -y openjdk-21-jdk > /dev/null 2>&1

log "Nginx wird installiert (für Website)..."
apt-get install -y nginx > /dev/null 2>&1

# ── 2. Clone/Download Projekt ─────────────────────────────────────────────────
log "Projekt wird heruntergeladen..."
rm -rf /opt/monsterjagd
git clone -b claude/trusting-brahmagupta-uJfP6 https://github.com/maaszluc5-lgtm/monsterjagd.minich.at.git /opt/monsterjagd 2>&1 || {
    warn "Git clone fehlgeschlagen - erstelle Verzeichnis manuell"
    mkdir -p /opt/monsterjagd
}

# ── 3. Discord Bot Setup ──────────────────────────────────────────────────────
log "Discord Bot wird eingerichtet..."
cd /opt/monsterjagd
npm install --production > /dev/null 2>&1

if [ ! -f /opt/monsterjagd/.env ]; then
    cp /opt/monsterjagd/.env.example /opt/monsterjagd/.env
    warn ".env Datei erstellt - MUSS noch ausgefüllt werden!"
fi

# ── 4. Systemd Service für Discord Bot ───────────────────────────────────────
log "Discord Bot Service wird eingerichtet..."
cat > /etc/systemd/system/monsterjagd-bot.service << 'EOF'
[Unit]
Description=Monsterjagd Discord+Minecraft Bot
After=network.target
Wants=network-online.target

[Service]
Type=simple
User=root
WorkingDirectory=/opt/monsterjagd
ExecStart=/usr/bin/node /opt/monsterjagd/index.js
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
EnvironmentFile=/opt/monsterjagd/.env

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable monsterjagd-bot

# ── 5. Website Setup ──────────────────────────────────────────────────────────
log "Website wird eingerichtet..."
cd /opt/monsterjagd/website
npm install --production > /dev/null 2>&1

if [ ! -f /opt/monsterjagd/website/.env ]; then
    cp /opt/monsterjagd/website/.env.example /opt/monsterjagd/website/.env
fi

cat > /etc/systemd/system/monsterjagd-website.service << 'EOF'
[Unit]
Description=Monsterjagd Rank Shop Website
After=network.target

[Service]
Type=simple
User=root
WorkingDirectory=/opt/monsterjagd/website
ExecStart=/usr/bin/node /opt/monsterjagd/website/server.js
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
EnvironmentFile=/opt/monsterjagd/website/.env

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable monsterjagd-website

# ── 6. Nginx Reverse Proxy ────────────────────────────────────────────────────
log "Nginx wird konfiguriert..."
cat > /etc/nginx/sites-available/monsterjagd << 'EOF'
server {
    listen 80;
    server_name monsterjagd.minich.at 49.13.232.29;

    # Rank Shop Website
    location / {
        proxy_pass http://localhost:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
EOF

ln -sf /etc/nginx/sites-available/monsterjagd /etc/nginx/sites-enabled/monsterjagd
rm -f /etc/nginx/sites-enabled/default
nginx -t && systemctl restart nginx

# ── 7. OpServer Plugin bauen ──────────────────────────────────────────────────
log "Minecraft Plugin wird gebaut..."
cd /opt/monsterjagd/opplugin
chmod +x gradlew
./gradlew shadowJar --no-daemon > /tmp/gradle-build.log 2>&1 && {
    log "Plugin erfolgreich gebaut!"
    ls -la build/libs/
} || {
    warn "Plugin Build fehlgeschlagen - Logs: /tmp/gradle-build.log"
}

# ── 8. Minecraft Server Setup ─────────────────────────────────────────────────
log "Minecraft Server Verzeichnis wird erstellt..."
mkdir -p /opt/minecraft/plugins

if [ -f /opt/monsterjagd/opplugin/build/libs/OpServer-1.0.0.jar ]; then
    cp /opt/monsterjagd/opplugin/build/libs/OpServer-1.0.0.jar /opt/minecraft/plugins/
    log "OpServer Plugin kopiert nach /opt/minecraft/plugins/"
fi

# Vault Plugin herunterladen
log "Vault Plugin wird heruntergeladen..."
wget -q "https://github.com/milkbowl/Vault/releases/download/1.7.3/Vault.jar" \
    -O /opt/minecraft/plugins/Vault.jar 2>/dev/null && \
    log "Vault heruntergeladen" || \
    warn "Vault konnte nicht heruntergeladen werden - manuell installieren"

# PlotSquared Config kopieren
if [ -d /opt/monsterjagd/plugin-configs/PlotSquared ]; then
    mkdir -p /opt/minecraft/plugins/PlotSquared
    cp -r /opt/monsterjagd/plugin-configs/PlotSquared/* /opt/minecraft/plugins/PlotSquared/
    log "PlotSquared Config kopiert"
fi

# QuickShop Config kopieren
if [ -d /opt/monsterjagd/plugin-configs/QuickShop ]; then
    mkdir -p /opt/minecraft/plugins/QuickShop-Hikari
    cp -r /opt/monsterjagd/plugin-configs/QuickShop/* /opt/minecraft/plugins/QuickShop-Hikari/
    log "QuickShop Config kopiert"
fi

# ── 9. Firewall ───────────────────────────────────────────────────────────────
log "Firewall wird konfiguriert..."
if command -v ufw &> /dev/null; then
    ufw allow 22/tcp > /dev/null 2>&1
    ufw allow 80/tcp > /dev/null 2>&1
    ufw allow 443/tcp > /dev/null 2>&1
    ufw allow 25565/tcp > /dev/null 2>&1
    ufw allow 25575/tcp > /dev/null 2>&1   # RCON
    ufw --force enable > /dev/null 2>&1
fi

# ── Fertig! ───────────────────────────────────────────────────────────────────
echo ""
echo -e "${GREEN}════════════════════════════════════════${NC}"
echo -e "${GREEN}  SETUP ABGESCHLOSSEN!${NC}"
echo -e "${GREEN}════════════════════════════════════════${NC}"
echo ""
echo -e "${YELLOW}WICHTIG: Fülle jetzt die .env Dateien aus!${NC}"
echo ""
echo "1. Discord Bot konfigurieren:"
echo "   nano /opt/monsterjagd/.env"
echo ""
echo "2. Website konfigurieren:"
echo "   nano /opt/monsterjagd/website/.env"
echo ""
echo "3. Discord Commands registrieren:"
echo "   cd /opt/monsterjagd && node register-commands.js"
echo ""
echo "4. Services starten:"
echo "   systemctl start monsterjagd-bot"
echo "   systemctl start monsterjagd-website"
echo ""
echo "5. Plugin in Minecraft einfügen:"
echo "   /opt/minecraft/plugins/OpServer-1.0.0.jar"
echo "   (Außerdem noch PlotSquared + QuickShop von SpigotMC laden)"
echo ""
echo -e "Website erreichbar unter: ${BLUE}http://monsterjagd.minich.at${NC}"
echo -e "Bot Logs: ${BLUE}journalctl -u monsterjagd-bot -f${NC}"
echo ""
echo -e "${RED}Sicherheit: Ändere dein Root-Passwort: passwd root${NC}"
