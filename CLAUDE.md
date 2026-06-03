# Monsterjagd Minecraft Server — Projekt-Übersicht für Claude

## Server-Zugangsdaten (AUSFÜLLEN!)
```
SSH IP:        49.13.232.29
SSH Port:      22
SSH User:      root
SSH Passwort:  VXivRwmeummh

Minecraft IP:  49.13.232.29
MC Port:       25565
MC Version:    1.21.x
```

## Discord Bot Daten (AUSFÜLLEN!)
```
DISCORD_TOKEN=           ← aus .env eintragen (im vorherigen Chat-Verlauf)
DISCORD_CLIENT_ID=1511346419439636541
DISCORD_GUILD_ID=1508068507340771408
DISCORD_LIVE_CHANNEL_ID=1511341355647893667
DISCORD_OWNER_USER_ID=1447174849628868681
MC_HOST=49.13.232.29
MC_PORT=25565
MC_USERNAME=           ← Minecraft Bot-Name (noch ausfüllen)
MC_VERSION=1.21
```

## Website Admin
```
ADMIN_PASSWORD=        ← noch ausfüllen
MC_RCON_PASSWORD=      ← noch ausfüllen (in server.properties setzen)
MC_RCON_PORT=25575
```

## Projekt-Struktur
```
/                          ← Discord Bot (Node.js)
  index.js                 ← Einstiegspunkt
  mcbot.js                 ← Minecraft Bot (mineflayer) + Watchdog
  discordbot.js            ← Discord Bot (alle /mc Befehle)
  commands.js              ← Slash-Command Definitionen
  register-commands.js     ← Einmalig ausführen zum Registrieren

opplugin/                  ← Java Paper Plugin (Maven/Gradle)
  src/main/java/at/minich/opserver/
    OpServerPlugin.java    ← Haupt-Klasse
    economy/               ← Coins, Bank (5 Slots), Zinsen-Konto, Lohn
    ranks/                 ← Kaufbare Ränge (Neuling→GOTT)
    jobs/                  ← 6 Berufe Level 1-100
    enchants/              ← Custom Enchants bis Level 200
    items/                 ← 20+ Custom Items
    mining/                ← Area Mining 1x1 bis 7x7
    markt/                 ← Spieler-Markt (/markt)
    ah/                    ← Auktionshaus (/ah)
    trophies/              ← 11 Custom Pokale
    salaryfarm/            ← Lohnfarm (3x Bonus)
    bank/                  ← Bank GUI
    salary/                ← Lohn GUI
    rewards/               ← Daily Reward
    kits/                  ← Kit System
    clan/                  ← Clan System
    trade/                 ← Trade GUI
    farmworld/             ← Farm World
    commands/              ← Alle Befehle
    listeners/             ← Alle Listener

plugin-configs/            ← Fertige Configs für externe Plugins
  PlotSquared/             ← Plot-Welt (32x32, kostet 500 Coins)
  QuickShop/               ← Kisten-Shops
  LWC/                     ← Kisten-Schutz

website/                   ← Rang-Shop Website (Node.js/Express)
  server.js                ← Express Server
  public/                  ← HTML/CSS/JS Frontend
  routes/                  ← Shop, Purchase, Admin Routes
  lib/rcon.js              ← RCON Verbindung zu Minecraft

deploy/                    ← Deployment Scripts
  install.sh               ← Alles-in-einem Setup Script
  monsterjagd-bot.service  ← systemd Service für Bot
```

## Installation auf dem Server
```bash
# SSH verbinden
ssh root@49.13.232.29

# Alles automatisch installieren
git clone https://github.com/maaszluc5-lgtm/monsterjagd.minich.at /opt/monsterjagd
bash /opt/monsterjagd/deploy/install.sh

# .env ausfüllen
nano /opt/monsterjagd/.env

# Discord Commands registrieren
cd /opt/monsterjagd && node register-commands.js

# Services starten
systemctl start monsterjagd-bot
systemctl start monsterjagd-website
```

## Plugin bauen
```bash
cd /opt/monsterjagd/opplugin
./gradlew shadowJar
# JAR liegt in: build/libs/OpServer-1.0.0.jar
# Kopieren nach: /pfad/minecraft/plugins/
```

## Externe Plugins die noch installiert werden müssen
1. **Vault** — https://www.spigotmc.org/resources/vault.34315/
2. **PlotSquared** — https://www.spigotmc.org/resources/plotsquared-for-1-21.77506/
3. **QuickShop-Hikari** — https://www.spigotmc.org/resources/quickshop-hikari.100125/
4. **LWC Extended** — https://www.spigotmc.org/resources/lwc-extended.69551/
5. **LuckPerms** — https://luckperms.net (für Rang-Vergabe via Website)

## Alle Befehle
Siehe vollständige Liste im Chat-Verlauf oder in plugin.yml

## Was noch fehlt / TODO
- [ ] MC_USERNAME in .env eintragen (Minecraft Bot-Account Name)
- [ ] RCON in server.properties aktivieren (enable-rcon=true, rcon.password=...)
- [ ] Externe Plugins herunterladen und installieren
- [ ] Discord Bot starten und testen
- [ ] Website online stellen

## Wichtige Hinweise
- Nur User-ID 1447174849628868681 kann Discord-Befehle nutzen
- GitHub Branch: claude/trusting-brahmagupta-uJfP6
- SSH von Cloud-Umgebungen ist geblockt → Desktop App verwenden
- Nach Setup SSH-Passwort ändern: passwd root
