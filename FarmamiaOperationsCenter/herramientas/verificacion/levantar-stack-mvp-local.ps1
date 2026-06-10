param(
    [string]$ApiPort = "8081",
    [string]$PanelPort = "4200",
    [switch]$NoLevantarPanel
)

$ErrorActionPreference = "Stop"

$raiz = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$backend = Join-Path $raiz "backend-api"
$panel = Join-Path $raiz "admin-panel"
$compose = Join-Path $raiz "infraestructura\local\docker-compose.mvp.yml"
$runtime = Join-Path $raiz ".runtime"
$logs = Join-Path $runtime "logs"
$pids = Join-Path $runtime "pids.json"

New-Item -ItemType Directory -Force -Path $logs | Out-Null

if (Test-Path $pids) {
    throw "Ya existe un stack MVP local registrado en $pids. Ejecute primero herramientas\verificacion\detener-stack-mvp-local.ps1."
}

function Esperar-HttpOk {
    param(
        [string]$Url,
        [int]$TimeoutSec = 90
    )

    $limite = (Get-Date).AddSeconds($TimeoutSec)
    do {
        try {
            Invoke-RestMethod -Uri $Url -TimeoutSec 5 | Out-Null
            return
        } catch {
            Start-Sleep -Seconds 2
        }
    } while ((Get-Date) -lt $limite)

    throw "Timeout esperando $Url"
}

function Iniciar-ProcesoOculto {
    param(
        [string]$Nombre,
        [string]$Directorio,
        [string]$Comando,
        [string]$Log,
        [string]$Err
    )

    Write-Host "Iniciando $Nombre..."
    Start-Process `
        -FilePath "powershell" `
        -ArgumentList @("-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", $Comando) `
        -WorkingDirectory $Directorio `
        -RedirectStandardOutput $Log `
        -RedirectStandardError $Err `
        -WindowStyle Hidden `
        -PassThru
}

Write-Host "Levantando PostgreSQL MVP con Docker Compose..."
docker compose -f $compose up -d postgres

$env:FARMAMIA_DB_URL = "jdbc:postgresql://localhost:5432/farmamia_ops"
$env:FARMAMIA_DB_USER = "farmamia"
$env:FARMAMIA_DB_PASSWORD = "farmamia"
$env:FARMAMIA_API_PORT = $ApiPort
$env:FARMAMIA_PACKAGE_STORAGE = (Join-Path $backend "data\packages")
$env:FARMAMIA_SEED_DEMO_ADMIN = "true"

$backendProc = Iniciar-ProcesoOculto `
    -Nombre "backend-api" `
    -Directorio $backend `
    -Comando ".\mvnw.cmd spring-boot:run" `
    -Log (Join-Path $logs "backend-api.log") `
    -Err (Join-Path $logs "backend-api.err.log")

Esperar-HttpOk -Url "http://localhost:$ApiPort/api/health" -TimeoutSec 120

$panelProc = $null
if (-not $NoLevantarPanel) {
    $panelProc = Iniciar-ProcesoOculto `
        -Nombre "admin-panel" `
        -Directorio $panel `
        -Comando "npm start -- --port $PanelPort" `
        -Log (Join-Path $logs "admin-panel.log") `
        -Err (Join-Path $logs "admin-panel.err.log")

    Esperar-HttpOk -Url "http://localhost:$PanelPort" -TimeoutSec 120
}

$estado = [ordered]@{
    backendPid = $backendProc.Id
    panelPid = if ($panelProc) { $panelProc.Id } else { $null }
    apiBaseUrl = "http://localhost:$ApiPort"
    panelUrl = if ($panelProc) { "http://localhost:$PanelPort" } else { $null }
    logs = $logs
    compose = $compose
}

$estado | ConvertTo-Json -Depth 5 | Set-Content -Path $pids -Encoding UTF8

Write-Host ""
Write-Host "Stack MVP local listo."
Write-Host "API:   http://localhost:$ApiPort"
if ($panelProc) {
    Write-Host "Panel: http://localhost:$PanelPort"
}
Write-Host "Logs:  $logs"
Write-Host "PIDs:  $pids"
