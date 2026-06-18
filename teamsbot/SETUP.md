# Monsterjagd Teams Bot - Azure Deployment

## Voraussetzungen

- Node.js 18+
- Azure Account (https://portal.azure.com)
- Microsoft 365 Tenant (Schule/Organisation)

## 1. Azure Bot erstellen

1. Gehe zu https://portal.azure.com
2. **Ressource erstellen** → Suche nach **Azure Bot**
3. Ausfüllen:
   - **Bot-Handle**: `monsterjagd-bot`
   - **Abonnement**: Dein Azure-Abo
   - **Ressourcengruppe**: Neue erstellen → `monsterjagd-rg`
   - **Tarif**: F0 (kostenlos)
   - **App-Typ**: Single Tenant
   - **Erstellungstyp**: Neue Microsoft App-ID erstellen
4. **Erstellen** klicken
5. Unter **Konfiguration** → Notiere **Microsoft App ID** und **App Password**

## 2. Azure App Service erstellen

```powershell
# Azure CLI installieren: https://aka.ms/installazurecli

# Einloggen
az login

# Ressourcengruppe (falls noch nicht vorhanden)
az group create --name monsterjagd-rg --location westeurope

# App Service Plan (kostenlos)
az appservice plan create --name monsterjagd-plan --resource-group monsterjagd-rg --sku F1 --is-linux

# Web App erstellen
az webapp create --name monsterjagd-teamsbot --resource-group monsterjagd-rg --plan monsterjagd-plan --runtime "NODE:18-lts"

# Umgebungsvariablen setzen
az webapp config appsettings set --name monsterjagd-teamsbot --resource-group monsterjagd-rg --settings ^
  MICROSOFT_APP_ID=deine-app-id ^
  MICROSOFT_APP_PASSWORD=dein-app-password ^
  DUOLINGO_USERS=username1,username2 ^
  DUOLINGO_CRON="0 20 * * *" ^
  WEBSITE_NODE_DEFAULT_VERSION=18-lts
```

## 3. Bot Endpoint konfigurieren

1. Azure Portal → **Azure Bot** Ressource öffnen
2. **Konfiguration** → **Messaging-Endpunkt**:
   ```
   https://monsterjagd-teamsbot.azurewebsites.net/api/messages
   ```
3. **Übernehmen**

## 4. Code deployen

### Option A: Git Deploy (einfachste)

```powershell
# Deployment-User einrichten (einmalig)
az webapp deployment user set --user-name monsterjagd-deploy --password DEIN_PASSWORT

# Git Remote hinzufügen
cd teamsbot
az webapp deployment source config-local-git --name monsterjagd-teamsbot --resource-group monsterjagd-rg

# URL aus dem Output kopieren und als Remote hinzufügen
git remote add azure https://monsterjagd-deploy@monsterjagd-teamsbot.scm.azurewebsites.net/monsterjagd-teamsbot.git

# Deployen
git push azure main
```

### Option B: ZIP Deploy

```powershell
cd teamsbot
npm install --production

# Alles zippen (ohne node_modules bei Bedarf - Azure installiert sie)
Compress-Archive -Path * -DestinationPath deploy.zip -Force

az webapp deploy --name monsterjagd-teamsbot --resource-group monsterjagd-rg --src-path deploy.zip --type zip
```

### Option C: GitHub Actions (automatisch bei jedem Push)

Im Repo unter `.github/workflows/azure-deploy.yml`:
```yaml
name: Deploy Teams Bot to Azure

on:
  push:
    paths:
      - 'teamsbot/**'
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-node@v4
        with:
          node-version: 18

      - run: |
          cd teamsbot
          npm install --production

      - uses: azure/webapps-deploy@v3
        with:
          app-name: monsterjagd-teamsbot
          package: teamsbot
          publish-profile: ${{ secrets.AZURE_WEBAPP_PUBLISH_PROFILE }}
```

Publish Profile holen:
```powershell
az webapp deployment list-publishing-profiles --name monsterjagd-teamsbot --resource-group monsterjagd-rg --xml
```
→ Output als GitHub Secret `AZURE_WEBAPP_PUBLISH_PROFILE` speichern

## 5. Teams Channel aktivieren

1. Azure Portal → **Azure Bot** → **Channels**
2. **Microsoft Teams** aktivieren
3. Nutzungsbedingungen akzeptieren

## 6. Teams App Manifest hochladen

```powershell
cd teamsbot
npm run manifest
```

Die generierte `monsterjagd-bot.zip` in Teams hochladen:
1. Teams öffnen
2. Apps → **App hochladen** → **Benutzerdefinierte App hochladen**
3. ZIP auswählen
4. Bot zum gewünschten Team/Chat hinzufügen

## 7. Testen

```powershell
# Health-Check
Invoke-RestMethod https://monsterjagd-teamsbot.azurewebsites.net/api/health

# Logs anschauen
az webapp log tail --name monsterjagd-teamsbot --resource-group monsterjagd-rg
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
- **Duolingo Cron**: Tägliches Update um 20:00 (konfigurierbar)

## Kosten

- **Azure Bot Service**: Kostenlos (F0 Tier, unbegrenzte Nachrichten in Teams)
- **App Service F1**: Kostenlos (60 Min CPU/Tag, 1 GB RAM)
- Für mehr Leistung: B1 Tier (~11€/Monat)
