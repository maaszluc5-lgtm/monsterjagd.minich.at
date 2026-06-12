# Monsterjagd Teams Bot - Self-Hosting auf Windows Server

## Voraussetzungen

- Windows Server / Windows 10/11
- Node.js 18+ (https://nodejs.org)
- PowerShell 5.1+
- Eigener Server mit öffentlicher Domain (z.B. `bot.monsterjagd.minich.at`)
- Microsoft 365 Tenant (Schule/Organisation)

## 1. Bot bei Microsoft registrieren (OHNE Azure Hosting)

1. Gehe zu https://dev.teams.microsoft.com/bots
2. Klick **+ New Bot**
3. Name: `Monsterjagd Bot`
4. Endpoint: `https://bot.monsterjagd.minich.at/api/messages`
5. Notiere dir die **App ID** und erstelle ein **Client Secret** (Password)

## 2. Bot einrichten

### PowerShell:
```powershell
cd teamsbot
Copy-Item .env.example .env
notepad .env   # App ID, Password, Hostname eintragen
npm install
```

### Oder einfach:
Doppelklick auf `start.bat` - das Script macht alles automatisch.

## 3. HTTPS einrichten

Teams akzeptiert NUR HTTPS. Optionen auf Windows:

### Option A: IIS Reverse Proxy (empfohlen auf Windows Server)

1. IIS installieren (Server Manager → Rollen hinzufügen → Web Server IIS)
2. **URL Rewrite** installieren: https://www.iis.net/downloads/microsoft/url-rewrite
3. **ARR (Application Request Routing)** installieren: https://www.iis.net/downloads/microsoft/application-request-routing

IIS Reverse Proxy Konfiguration (`web.config`):
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <system.webServer>
        <rewrite>
            <rules>
                <rule name="TeamsBot" stopProcessing="true">
                    <match url="(.*)" />
                    <action type="Rewrite" url="http://localhost:3978/{R:1}" />
                </rule>
            </rules>
        </rewrite>
    </system.webServer>
</configuration>
```

SSL-Zertifikat in IIS:
```powershell
# Let's Encrypt mit win-acme
# Download: https://www.win-acme.com/
winacme.exe --target iis --siteid 1 --installation iis
```

### Option B: Caddy (einfachste Lösung - automatisches HTTPS)

```powershell
# Caddy installieren: https://caddyserver.com/download
# Caddyfile erstellen:
```

Caddyfile:
```
bot.monsterjagd.minich.at {
    reverse_proxy localhost:3978
}
```

```powershell
caddy run
```

Caddy holt sich automatisch ein SSL-Zertifikat!

### Option C: nginx für Windows

```nginx
server {
    listen 443 ssl;
    server_name bot.monsterjagd.minich.at;

    ssl_certificate     certs/fullchain.pem;
    ssl_certificate_key certs/privkey.pem;

    location / {
        proxy_pass http://localhost:3978;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

## 4. Bot starten

### Einfach (PowerShell):
```powershell
.\start.ps1
```

### Oder Doppelklick:
`start.bat`

### Als Windows-Dienst (läuft im Hintergrund, startet automatisch):
```powershell
# NSSM installieren: https://nssm.cc/download
# nssm.exe nach C:\Windows\System32\ kopieren

# Dienst installieren
.\install-service.ps1 -Action install

# Dienst starten
.\install-service.ps1 -Action start

# Status prüfen
.\install-service.ps1 -Action status

# Dienst stoppen
.\install-service.ps1 -Action stop

# Dienst entfernen
.\install-service.ps1 -Action remove
```

## 5. Teams App Manifest erstellen & hochladen

```powershell
# Icons vorbereiten (in manifest\ Ordner):
# - color.png: 192x192 px
# - outline.png: 32x32 px

npm run manifest
```

Die generierte `monsterjagd-bot.zip` in Teams hochladen:
1. Teams öffnen
2. Apps → **App hochladen** → **Benutzerdefinierte App hochladen**
3. ZIP auswählen
4. Bot zum gewünschten Team/Chat hinzufügen

## 6. Testen

```powershell
# Health-Check
Invoke-RestMethod https://bot.monsterjagd.minich.at/api/health

# Sollte zurückgeben: status: ok, bot: MonsterjagdTeamsBot
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

## Firewall

Port 3978 muss intern erreichbar sein (nur für den Reverse Proxy, nicht von außen):
```powershell
New-NetFirewallRule -DisplayName "Teams Bot" -Direction Inbound -LocalPort 3978 -Protocol TCP -Action Allow
```

Port 443 muss von außen erreichbar sein (für Teams):
```powershell
New-NetFirewallRule -DisplayName "HTTPS" -Direction Inbound -LocalPort 443 -Protocol TCP -Action Allow
```
