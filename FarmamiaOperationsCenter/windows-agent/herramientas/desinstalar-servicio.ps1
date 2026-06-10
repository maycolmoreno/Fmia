$ErrorActionPreference = "Stop"

$servicio = "FarmamiaOpsAgent"
$existente = Get-Service -Name $servicio -ErrorAction SilentlyContinue
if (-not $existente) {
    Write-Host "Servicio $servicio no esta instalado."
    exit 0
}

sc.exe stop $servicio | Out-Null
Start-Sleep -Seconds 2
sc.exe delete $servicio | Out-Null

Write-Host "Servicio $servicio desinstalado."
