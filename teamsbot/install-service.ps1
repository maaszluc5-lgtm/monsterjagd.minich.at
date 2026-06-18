# Monsterjagd Teams Bot als Windows-Dienst installieren (mit NSSM)
# NSSM Download: https://nssm.cc/download

param(
    [string]$Action = "install"
)

$serviceName = "MonsterjagdTeamsBot"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$nodePath = (Get-Command node).Source

switch ($Action) {
    "install" {
        # Prüfe ob NSSM vorhanden
        $nssm = Get-Command nssm -ErrorAction SilentlyContinue
        if (-not $nssm) {
            Write-Host "[FEHLER] NSSM nicht gefunden!" -ForegroundColor Red
            Write-Host "Download: https://nssm.cc/download" -ForegroundColor Yellow
            Write-Host "Entpacke nssm.exe nach C:\Windows\System32\" -ForegroundColor Yellow
            exit 1
        }

        Write-Host "Installiere $serviceName als Windows-Dienst..." -ForegroundColor Cyan

        nssm install $serviceName $nodePath "$scriptDir\index.js"
        nssm set $serviceName AppDirectory $scriptDir
        nssm set $serviceName DisplayName "Monsterjagd Teams Bot"
        nssm set $serviceName Description "Teams Bot - Duolingo, Phishing-Schutz, Chat-Logging"
        nssm set $serviceName Start SERVICE_AUTO_START
        nssm set $serviceName AppStdout "$scriptDir\logs\service-stdout.log"
        nssm set $serviceName AppStderr "$scriptDir\logs\service-stderr.log"
        nssm set $serviceName AppRotateFiles 1
        nssm set $serviceName AppRotateBytes 1048576

        Write-Host "[OK] Dienst installiert!" -ForegroundColor Green
        Write-Host "Starten mit: nssm start $serviceName" -ForegroundColor Yellow
    }
    "start" {
        nssm start $serviceName
        Write-Host "[OK] Bot gestartet" -ForegroundColor Green
    }
    "stop" {
        nssm stop $serviceName
        Write-Host "[OK] Bot gestoppt" -ForegroundColor Green
    }
    "remove" {
        nssm stop $serviceName 2>$null
        nssm remove $serviceName confirm
        Write-Host "[OK] Dienst entfernt" -ForegroundColor Green
    }
    "status" {
        nssm status $serviceName
    }
    default {
        Write-Host "Verwendung: .\install-service.ps1 -Action [install|start|stop|remove|status]"
    }
}
