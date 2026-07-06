param(
    [string]$NombreServicio = "FarmamiaOpsAgent"
)

$ErrorActionPreference = "Stop"

$identidad = [Security.Principal.WindowsIdentity]::GetCurrent()
$principal = [Security.Principal.WindowsPrincipal]::new($identidad)
if (-not $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    throw "Ejecute este script desde PowerShell como administrador."
}

$servicio = Get-Service -Name $NombreServicio -ErrorAction SilentlyContinue
if (-not $servicio) {
    Write-Host "El servicio $NombreServicio no existe."
    return
}

if ($servicio.Status -ne "Stopped") {
    Stop-Service -Name $NombreServicio -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 2
}

sc.exe delete $NombreServicio | Out-Null
Write-Host "Servicio $NombreServicio desinstalado."

