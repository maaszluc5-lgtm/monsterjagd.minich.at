# Monsterjagd Teams Bot - Self-Hosting Setup

## Voraussetzungen

- Node.js 18+
- Eigener Server mit öffentlicher Domain (z.B. `bot.monsterjagd.minich.at`)
- HTTPS (z.B. via nginx + Let's Encrypt)
- Microsoft 365 Tenant (Schule/Organisation)

## 1. Bot bei Microsoft registrieren (OHNE Azure Hosting)

1. Gehe zu https://dev.teams.microsoft.com/bots
2. Klick **+ New Bot**
3. Name: `Monsterjagd Bot`
4. Endpoint: `https://bot.monsterjagd.minich.at/api/messages`
5. Notiere dir die **App ID** und erstelle ein **Client Secret** (Password)

Alternative: Über https://portal.azure.com → **Bot Services** → **Azure Bot** erstellen
(nur die Bot-Registration, NICHT das Hosting - der Bot läuft auf deinem Server!)

## 2. Server einrichten

```bash
cd teamsbot
cp .env.example .env
nano .env   # App ID, Password, Hostname eintragen
npm install
```

## 3. Nginx Reverse Proxy (HTTPS)

Teams erfordert HTTPS. Beispiel nginx-Config:

```nginx
server {
    listen 443 ssl;
    server_name bot.monsterjagd.minich.at;

    ssl_certificate /etc/letsencrypt/live/bot.monsterjagd.minich.at/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/bot.monsterjagd.minich.at/privkey.pem;

    location / {
        proxy_pass http://localhost:3978;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

Let's Encrypt einrichten:
```bash
sudo certbot --nginx -d bot.monsterjagd.minich.at
```

## 4. Bot starten

```bash
# Direkt
npm start

# Oder mit pm2 (empfohlen)
npm install -g pm2
pm2 start index.js --name teamsbot
pm2 save
pm2 startup
```

## 5. Teams App Manifest erstellen & hochladen

```bash
# Icons vorbereiten (in manifest/ Ordner):
# - color.png: 192x192 px
# - outline.png: 32x32 px

node create-manifest.js
```

Die generierte `monsterjagd-bot.zip` in Teams hochladen:
1. Teams öffnen
2. Apps → **App hochladen** → **Benutzerdefinierte App hochladen**
3. ZIP auswählen
4. Bot zum gewünschten Team/Chat hinzufügen

## 6. Testen

```bash
# Health-Check
curl https://bot.monsterjagd.minich.at/api/health

# Sollte zurückgeben: {"status":"ok","bot":"MonsterjagdTeamsBot"}
```

Im Teams Chat:
- `!duolingo` - Duolingo Stats anzeigen
- `!check Dein Konto wurde gesperrt, klicke hier` - Phishing-Check
- Schreibe ein Schimpfwort → Bot warnt dich

## Befehle

| Befehl | Beschreibung |
|--------|-------------|
| `!duolingo` | Zeigt Duolingo Streak/XP aller User |
| `!check <text>` | Manuelle Phishing-Prüfung |

## Automatische Features

- **Phishing-Erkennung**: Jede Nachricht wird automatisch gescannt
- **Schimpfwort-Filter**: Unangemessene Sprache wird erkannt und gewarnt
- **Chat-Logging**: Alle Nachrichten werden in `logs/` als JSON gespeichert
- **Duolingo Cron**: Tägliches Update um 20:00 (konfigurierbar via `DUOLINGO_CRON`)
