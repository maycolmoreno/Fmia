param(
    [string]$RutaAgente = "C:\Program Files (x86)\Farmamia Cia Ltda - Elipsys\Agent"
)

$ErrorActionPreference = "Stop"

$servicio = Get-Service -Name "FarmamiaOpsAgent" -ErrorAction Stop
$carpetas = "Downloads", "Backups", "Logs", "Temp", "State"
$checks = foreach ($carpeta in $carpetas) {
    [PSCustomObject]@{
        name = $carpeta
        exists = Test-Path (Join-Path $RutaAgente $carpeta)
    }
}

[PSCustomObject]@{
    serviceName = $servicio.Name
    serviceStatus = $servicio.Status.ToString()
    startType = $servicio.StartType.ToString()
    agentRootExists = Test-Path $RutaAgente
    serviceExeExists = Test-Path (Join-Path $RutaAgente "Farmamia.Agent.Service.exe")
    updaterExeExists = Test-Path (Join-Path $RutaAgente "FarmamiaUpdater.exe")
    configExists = Test-Path (Join-Path $RutaAgente "config.json")
    folders = $checks
}
