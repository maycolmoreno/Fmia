param(
    [string]$ApiBaseUrl = "http://localhost:8081",
    [string]$UsuarioAdmin = "admin",
    [string]$ContrasenaAdmin = "admin123"
)

$ErrorActionPreference = "Stop"

$raiz = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$desarrollo = Join-Path $raiz "herramientas\desarrollo"

function Esperar-Api {
    $limite = (Get-Date).AddSeconds(90)
    do {
        try {
            Invoke-RestMethod -Uri "$ApiBaseUrl/api/health" -TimeoutSec 5 | Out-Null
            return
        } catch {
            Start-Sleep -Seconds 2
        }
    } while ((Get-Date) -lt $limite)

    throw "La API no respondio en $ApiBaseUrl/api/health"
}

Esperar-Api

Write-Host "Ejecutando E2E demo exitoso..."
& (Join-Path $desarrollo "crear-actualizacion-exitosa-demo.ps1") `
    -ApiBaseUrl $ApiBaseUrl `
    -UsuarioAdmin $UsuarioAdmin `
    -ContrasenaAdmin $ContrasenaAdmin

Write-Host "Ejecutando E2E demo fallido..."
& (Join-Path $desarrollo "crear-alerta-fallo-demo.ps1") `
    -ApiBaseUrl $ApiBaseUrl `
    -UsuarioAdmin $UsuarioAdmin `
    -ContrasenaAdmin $ContrasenaAdmin

Write-Host ""
Write-Host "E2E demo local completado."
