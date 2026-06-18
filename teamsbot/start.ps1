# Monsterjagd Teams Bot - PowerShell Start Script

Write-Host ""
Write-Host "=== Monsterjagd Teams Bot ===" -ForegroundColor Cyan
Write-Host ""

# Prüfe ob Node.js installiert ist
try {
    $nodeVersion = node --version
    Write-Host "[OK] Node.js $nodeVersion gefunden" -ForegroundColor Green
} catch {
    Write-Host "[FEHLER] Node.js nicht gefunden! Installiere Node.js von https://nodejs.org" -ForegroundColor Red
    exit 1
}

# Ins Bot-Verzeichnis wechseln
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptDir

# Prüfe ob node_modules existiert
if (-not (Test-Path "node_modules")) {
    Write-Host "[INFO] Installiere Dependencies..." -ForegroundColor Yellow
    npm install
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[FEHLER] npm install fehlgeschlagen!" -ForegroundColor Red
        exit 1
    }
}

# Prüfe ob .env existiert
if (-not (Test-Path ".env")) {
    if (Test-Path ".env.example") {
        Copy-Item ".env.example" ".env"
        Write-Host "[WARNUNG] .env wurde aus .env.example erstellt - bitte ausfuellen!" -ForegroundColor Yellow
        Write-Host "          Oeffne teamsbot\.env und trage App ID + Password ein." -ForegroundColor Yellow
        notepad ".env"
        exit 0
    } else {
        Write-Host "[FEHLER] Keine .env Datei gefunden!" -ForegroundColor Red
        exit 1
    }
}

Write-Host "[INFO] Starte Bot..." -ForegroundColor Yellow
Write-Host ""

node index.js
