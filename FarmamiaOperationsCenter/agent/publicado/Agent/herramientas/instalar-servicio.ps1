param(
    [string]$RutaAgente = "C:\Program Files (x86)\Farmamia Cia Ltda - Elipsys\Agent",
    [string]$NombreServicio = "FarmamiaOpsAgent",
    [string]$NombreVisible = "Farmamia Operations Center Agent"
)

$ErrorActionPreference = "Stop"

function Confirmar-Administrador {
    $identidad = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = [Security.Principal.WindowsPrincipal]::new($identidad)
    if (-not $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
        throw "Ejecute este script desde PowerShell como administrador."
    }
}

Confirmar-Administrador

$exe = Join-Path $RutaAgente "Farmamia.Agent.Service.exe"
if (-not (Test-Path $exe)) {
    throw "No se encontro $exe. Publique o copie primero los binarios del agente."
}

foreach ($carpeta in @("Downloads", "Backups", "Logs", "Temp", "State")) {
    New-Item -ItemType Directory -Force -Path (Join-Path $RutaAgente $carpeta) | Out-Null
}

$servicio = Get-Service -Name $NombreServicio -ErrorAction SilentlyContinue
if ($servicio) {
    if ($servicio.Status -ne "Stopped") {
        Stop-Service -Name $NombreServicio -Force -ErrorAction SilentlyContinue
        Start-Sleep -Seconds 2
    }

    sc.exe delete $NombreServicio | Out-Null
    Start-Sleep -Seconds 2
}

$binario = "`"$exe`" --contentRoot `"$RutaAgente`""
New-Service `
    -Name $NombreServicio `
    -BinaryPathName $binario `
    -DisplayName $NombreVisible `
    -Description "Agente Windows para Farmamia Operations Center." `
    -StartupType Automatic | Out-Null

sc.exe failure $NombreServicio reset= 86400 actions= restart/60000/restart/60000/restart/300000 | Out-Null

Start-Service -Name $NombreServicio
Start-Sleep -Seconds 3
Get-Service -Name $NombreServicio | Select-Object Name, Status, StartType

