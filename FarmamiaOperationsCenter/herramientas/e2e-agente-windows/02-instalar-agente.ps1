param(
    [Parameter(Mandatory = $true)][string]$PaqueteAgente,
    [string]$RutaAgente = "C:\Program Files (x86)\Farmamia Cia Ltda - Elipsys\Agent",
    [string]$ApiBaseUrl = "http://localhost:8081",
    [string]$CodigoSucursal = "FMA-PILOTO-001",
    [string]$RutaPos = "C:\Program Files (x86)\Farmamia Cia Ltda - Elipsys\Cliente",
    [switch]$IniciarServicio
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $PaqueteAgente)) {
    throw "No existe el paquete/carpeta del agente: $PaqueteAgente"
}

New-Item -ItemType Directory -Force -Path $RutaAgente | Out-Null

if ((Get-Item $PaqueteAgente).PSIsContainer) {
    Copy-Item -Path (Join-Path $PaqueteAgente "*") -Destination $RutaAgente -Recurse -Force
}
else {
    $temporal = Join-Path $env:TEMP ("farmamia-agent-pkg-" + [Guid]::NewGuid().ToString("N"))
    New-Item -ItemType Directory -Force -Path $temporal | Out-Null
    Expand-Archive -Path $PaqueteAgente -DestinationPath $temporal -Force
    Copy-Item -Path (Join-Path $temporal "*") -Destination $RutaAgente -Recurse -Force
    Remove-Item -LiteralPath $temporal -Recurse -Force
}

foreach ($carpeta in @("Downloads", "Backups", "Logs", "Temp", "State")) {
    New-Item -ItemType Directory -Force -Path (Join-Path $RutaAgente $carpeta) | Out-Null
}

$config = [ordered]@{
    AgenteFarmamia = [ordered]@{
        UrlApiCentral = $ApiBaseUrl
        CodigoSucursal = $CodigoSucursal
        VersionAgente = "1.0.0"
        RutaPos = $RutaPos
        RutaAgente = $RutaAgente
        IntervaloHeartbeatSegundos = 30
        TimeoutSegundos = 30
        MaxIntentosDescarga = 3
        BackoffInicialSegundos = 5
        BackoffMaximoSegundos = 300
    }
}
$config | ConvertTo-Json -Depth 5 | Set-Content -Path (Join-Path $RutaAgente "config.json") -Encoding UTF8

$servicio = "FarmamiaOpsAgent"
$exe = Join-Path $RutaAgente "Farmamia.Agent.Service.exe"
if (-not (Test-Path $exe)) {
    throw "No se encontro $exe despues de copiar el paquete del agente."
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

if ($IniciarServicio) {
    Start-Service -Name "FarmamiaOpsAgent"
} else {
    sc.exe start $servicio | Out-Null
}

[PSCustomObject]@{
    agentRoot = $RutaAgente
    serviceName = "FarmamiaOpsAgent"
    apiBaseUrl = $ApiBaseUrl
    branchCode = $CodigoSucursal
    posPath = $RutaPos
}
