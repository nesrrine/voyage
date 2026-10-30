# Demarre toute l'architecture microservices, chaque service dans sa propre fenetre.
# Utilisation (dans le dossier backend) :  powershell -ExecutionPolicy Bypass -File .\demarrer-microservices.ps1
# Prerequis : avoir compile une fois avec  mvn -DskipTests package

$racine = Split-Path -Parent $MyInvocation.MyCommand.Path

function Demarrer($module) {
    $jar = Join-Path $racine "$module\target\$module-1.0.0.jar"
    if (-not (Test-Path $jar)) {
        Write-Host "Jar introuvable : $jar  ->  lancez d'abord 'mvn -DskipTests package'" -ForegroundColor Red
        exit 1
    }
    Write-Host "Demarrage de $module..." -ForegroundColor Cyan
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "`$host.UI.RawUI.WindowTitle='$module'; Set-Location '$racine\$module'; java -jar '$jar'"
}

function Attendre($url, $nom) {
    Write-Host "  attente de $nom..." -NoNewline
    for ($i = 0; $i -lt 60; $i++) {
        try { Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 2 | Out-Null; Write-Host " OK" -ForegroundColor Green; return } catch { Start-Sleep -Seconds 2 }
    }
    Write-Host " delai depasse" -ForegroundColor Yellow
}

# 1. Annuaire Eureka en premier
Demarrer "discovery-server"
Attendre "http://localhost:8761" "Eureka (8761)"

# 2. Microservices metier (ils s'enregistrent aupres d'Eureka)
Demarrer "user-service"
Demarrer "voyage-service"
Demarrer "forum-service"
Demarrer "ai-service"

# 3. Point d'entree unique
Demarrer "api-gateway"
Attendre "http://localhost:8080/api/meta/domaines" "API Gateway (8080) et voyage-service"

Write-Host ""
Write-Host "Tableau de bord Eureka : http://localhost:8761" -ForegroundColor Green
Write-Host "API (via la gateway)   : http://localhost:8080/api" -ForegroundColor Green
Write-Host "Frontend Angular       : cd ..\frontend ; npm start  ->  http://localhost:4200" -ForegroundColor Green
