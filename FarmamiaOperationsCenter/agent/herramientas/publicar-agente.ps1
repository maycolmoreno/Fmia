param(
    [string]$Configuracion = "Release",
    [string]$Runtime = "win-x64",
    [string]$Salida = ".\publicado\Agent",
    [string]$RutaDestino = "",
    [string]$UrlApiCentral = "http://192.168.0.168:8081",
    [string]$CodigoSucursal = "FMA001",
    [string]$RutaPos = "C:\Program Files (x86)\Farmamia Cia Ltda - Elipsys\Cliente",
    [switch]$SelfContained,
    [switch]$OmitirUpdater,
    [switch]$InstalarServicio
)

$ErrorActionPreference = "Stop"

$raiz = Resolve-Path (Join-Path $PSScriptRoot "..")
$salidaAbsoluta = Join-Path $raiz $Salida
$servicioProyecto = Join-Path $raiz "Farmamia.Agent\Farmamia.Agent.csproj"
$updaterProyecto = Join-Path $raiz "FarmamiaUpdater\FarmamiaUpdater.csproj"
$publicadoServicio = Join-Path $raiz "publicado\servicio"
$publicadoUpdater = Join-Path $raiz "publicado\updater"

function Limpiar-Carpeta([string]$Ruta) {
    if (Test-Path $Ruta) {
        Remove-Item -LiteralPath $Ruta -Recurse -Force
    }

    New-Item -ItemType Directory -Force -Path $Ruta | Out-Null
}

function Ejecutar-DotnetPublish([string[]]$Argumentos) {
    & dotnet publish @Argumentos
    if ($LASTEXITCODE -ne 0) {
        throw "dotnet publish fallo con codigo $LASTEXITCODE."
    }
}

Limpiar-Carpeta $publicadoServicio
if (-not $OmitirUpdater) {
    Limpiar-Carpeta $publicadoUpdater
}
Limpiar-Carpeta $salidaAbsoluta

$selfContainedValue = if ($SelfContained) { "true" } else { "false" }

Ejecutar-DotnetPublish @($servicioProyecto, "-c", $Configuracion, "-r", $Runtime, "--self-contained", $selfContainedValue, "-o", $publicadoServicio)
if (-not $OmitirUpdater) {
    Ejecutar-DotnetPublish @($updaterProyecto, "-c", $Configuracion, "-r", $Runtime, "--self-contained", $selfContainedValue, "-o", $publicadoUpdater)
}

Copy-Item -Path (Join-Path $publicadoServicio "*") -Destination $salidaAbsoluta -Recurse -Force
if (-not $OmitirUpdater) {
    Copy-Item -Path (Join-Path $publicadoUpdater "FarmamiaUpdater*") -Destination $salidaAbsoluta -Force -ErrorAction SilentlyContinue
}
Copy-Item -Path $PSScriptRoot -Destination (Join-Path $salidaAbsoluta "herramientas") -Recurse -Force

foreach ($carpeta in @("Downloads", "Backups", "Logs", "Temp", "State")) {
    New-Item -ItemType Directory -Force -Path (Join-Path $salidaAbsoluta $carpeta) | Out-Null
}

$rutaAgenteConfig = if ([string]::IsNullOrWhiteSpace($RutaDestino)) {
    "C:\Program Files (x86)\Farmamia Cia Ltda - Elipsys\Agent"
} else {
    $RutaDestino
}

$config = [ordered]@{
    AgenteFarmamia = [ordered]@{
        UrlApiCentral = $UrlApiCentral
        CodigoSucursal = $CodigoSucursal
        VersionAgente = "1.0.0"
        RutaPos = $RutaPos
        RutaAgente = $rutaAgenteConfig
        IntervaloHeartbeatSegundos = 60
        TimeoutSegundos = 30
        MaxIntentosDescarga = 3
        BackoffInicialSegundos = 5
        BackoffMaximoSegundos = 300
    }
}

$config | ConvertTo-Json -Depth 5 | Set-Content -Path (Join-Path $salidaAbsoluta "config.json") -Encoding UTF8

if (-not [string]::IsNullOrWhiteSpace($RutaDestino)) {
    New-Item -ItemType Directory -Force -Path $RutaDestino | Out-Null
    Copy-Item -Path (Join-Path $salidaAbsoluta "*") -Destination $RutaDestino -Recurse -Force
    Write-Host "Agente copiado en $RutaDestino"
}

if ($InstalarServicio) {
    if ([string]::IsNullOrWhiteSpace($RutaDestino)) {
        throw "Para -InstalarServicio debe indicar -RutaDestino."
    }

    & (Join-Path $PSScriptRoot "instalar-servicio.ps1") -RutaAgente $RutaDestino
}

Write-Host "Publicacion lista en $salidaAbsoluta"
