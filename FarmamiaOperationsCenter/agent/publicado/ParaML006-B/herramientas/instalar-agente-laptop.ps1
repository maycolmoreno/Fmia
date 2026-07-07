param(
    [string]$UrlApiCentral = "http://192.168.0.168:8081",
    [string]$CodigoSucursal = "FMA001",
    [string]$RutaPos = "C:\Program Files (x86)\Farmamia Cia Ltda - Elipsys\Cliente",
    [string]$RutaAgente = "C:\Program Files (x86)\Farmamia Cia Ltda - Elipsys\Agent",
    [switch]$CrearPosDemo
)

$ErrorActionPreference = "Stop"

$identidad = [Security.Principal.WindowsIdentity]::GetCurrent()
$principal = [Security.Principal.WindowsPrincipal]::new($identidad)
if (-not $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    throw "Ejecute este instalador como administrador."
}

if ($CrearPosDemo) {
    New-Item -ItemType Directory -Force -Path $RutaPos | Out-Null
    if (-not (Test-Path (Join-Path $RutaPos "Zabyca.Pos.Desktop.exe"))) {
        Set-Content -Path (Join-Path $RutaPos "Zabyca.Pos.Desktop.exe") -Value "POS demo Farmamia 1.0.0" -Encoding UTF8
    }
    if (-not (Test-Path (Join-Path $RutaPos "version.txt"))) {
        Set-Content -Path (Join-Path $RutaPos "version.txt") -Value "1.0.0" -Encoding UTF8
    }
}

& (Join-Path $PSScriptRoot "publicar-agente.ps1") `
    -Configuracion Release `
    -Runtime win-x64 `
    -RutaDestino $RutaAgente `
    -UrlApiCentral $UrlApiCentral `
    -CodigoSucursal $CodigoSucursal `
    -RutaPos $RutaPos `
    -SelfContained `
    -InstalarServicio

Write-Host ""
Write-Host "Instalacion terminada."
Write-Host "Servicio: FarmamiaOpsAgent"
Write-Host "Ruta agente: $RutaAgente"
Write-Host "API: $UrlApiCentral"
Write-Host ""
Write-Host "Validacion rapida:"
Write-Host "  Get-Service FarmamiaOpsAgent"
Write-Host "  Get-Content `"$RutaAgente\Logs\*.log`" -Tail 80"
