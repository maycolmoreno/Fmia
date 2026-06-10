param(
    [string]$RutaAgente = "C:\Program Files (x86)\Farmamia Cia Ltda - Elipsys\Agent"
)

$ErrorActionPreference = "Stop"

$servicio = "FarmamiaOpsAgent"
$exe = Join-Path $RutaAgente "Farmamia.Agent.Service.exe"

if (-not (Test-Path $exe)) {
    throw "No se encontro $exe. Publique/copielo antes de instalar el servicio."
}

New-Item -ItemType Directory -Force -Path $RutaAgente | Out-Null
foreach ($carpeta in @("Downloads", "Backups", "Logs", "Temp", "State")) {
    New-Item -ItemType Directory -Force -Path (Join-Path $RutaAgente $carpeta) | Out-Null
}

$existente = Get-Service -Name $servicio -ErrorAction SilentlyContinue
if ($existente) {
    sc.exe stop $servicio | Out-Null
    sc.exe delete $servicio | Out-Null
    Start-Sleep -Seconds 2
}

sc.exe create $servicio binPath= "`"$exe`"" start= auto DisplayName= "Farmamia Operations Agent" | Out-Null
sc.exe failure $servicio reset= 86400 actions= restart/60000/restart/60000/restart/60000 | Out-Null
sc.exe description $servicio "Agente Farmamia Operations Center para inventario, heartbeat y actualizacion POS." | Out-Null
sc.exe start $servicio | Out-Null

Write-Host "Servicio $servicio instalado e iniciado."
