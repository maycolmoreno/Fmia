param(
    [string]$ApiPort = "8081",
    [string]$PanelPort = "4200",
    [switch]$PostgresLocal,
    [string]$DbUrl = "jdbc:postgresql://localhost:5432/farmamia_ops",
    [string]$DbUser = "postgres",
    [string]$DbPassword,
    [string]$GrafanaAdminPassword,
    [switch]$NoLevantarPanel
)

$ErrorActionPreference = "Stop"

function Nueva-ContrasenaAleatoria {
    param([int]$Longitud = 24)
    $caracteres = (48..57) + (65..90) + (97..122)
    -join ((1..$Longitud) | ForEach-Object { [char]($caracteres | Get-Random) })
}

if ($PostgresLocal) {
    if (-not $DbPassword) {
        throw "Con -PostgresLocal debe indicar -DbPassword con la contrasena real de su PostgreSQL local; no se asume ningun valor por defecto."
    }
} else {
    # Se usara el Postgres/Grafana levantados por docker-compose.mvp.yml en este mismo run.
    # Si no se indica una contrasena, se genera una aleatoria (nunca un valor fijo en el repo).
    if (-not $DbPassword) {
        $DbPassword = Nueva-ContrasenaAleatoria
        Write-Host "FARMAMIA_DB_PASSWORD generada automaticamente para este run: $DbPassword"
    }
}

if (-not $GrafanaAdminPassword) {
    $GrafanaAdminPassword = Nueva-ContrasenaAleatoria
    Write-Host "FARMAMIA_GRAFANA_ADMIN_PASSWORD generada automaticamente para este run: $GrafanaAdminPassword"
}

$env:FARMAMIA_DB_PASSWORD = $DbPassword
$env:FARMAMIA_GRAFANA_ADMIN_PASSWORD = $GrafanaAdminPassword

$raiz = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$backend = Join-Path $raiz "backend"
$panel = Join-Path $raiz "frontend"
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

if ($PostgresLocal) {
    Write-Host "Usando PostgreSQL local existente en $DbUrl."
} else {
    Write-Host "Levantando PostgreSQL MVP con Docker Compose..."
    docker compose -f $compose up -d postgres
    # El compose mapea el contenedor (5432) al puerto de host 5433 (ver
    # infraestructura\local\docker-compose.mvp.yml) para no chocar con un Postgres local existente.
    $DbUrl = "jdbc:postgresql://localhost:5433/farmamia_ops"
    $DbUser = "farmamia"
    # $DbPassword ya quedo definida arriba (indicada por el usuario o generada aleatoriamente)
    # y coincide con la que docker compose acaba de usar via FARMAMIA_DB_PASSWORD.
}

$env:FARMAMIA_DB_URL = $DbUrl
$env:FARMAMIA_DB_USER = $DbUser
$env:FARMAMIA_DB_PASSWORD = $DbPassword
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
    postgresLocal = [bool]$PostgresLocal
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
