@echo off
echo.
echo === Monsterjagd Teams Bot ===
echo.

where node >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [FEHLER] Node.js nicht gefunden! Installiere Node.js von https://nodejs.org
    pause
    exit /b 1
)

cd /d "%~dp0"

if not exist "node_modules" (
    echo [INFO] Installiere Dependencies...
    npm install
)

if not exist ".env" (
    if exist ".env.example" (
        copy ".env.example" ".env"
        echo [WARNUNG] .env wurde erstellt - bitte ausfuellen!
        notepad ".env"
        pause
        exit /b 0
    )
)

echo [INFO] Starte Bot...
echo.
node index.js
pause
