param(
    [string]$RutaAgente = "C:\Program Files (x86)\Farmamia Cia Ltda - Elipsys\Agent",
    [string]$Servicio = "FarmamiaOpsAgent"
)

$ErrorActionPreference = "Continue"

function Escribir-Seccion([string]$Mensaje) {
    Write-Host ""
    Write-Host "== $Mensaje ==" -ForegroundColor Cyan
}

Escribir-Seccion "Servicio"
$svc = Get-Service -Name $Servicio -ErrorAction SilentlyContinue
if ($svc) {
    $svc | Select-Object Name, Status, StartType
} else {
    Write-Warning "No existe el servicio $Servicio."
}

Escribir-Seccion "Configuracion"
$configPath = Join-Path $RutaAgente "config.json"
if (Test-Path $configPath) {
    $config = Get-Content $configPath -Raw | ConvertFrom-Json
    $opciones = if ($config.AgenteFarmamia) { $config.AgenteFarmamia } else { $config }
    $opciones | Format-List

    if ($opciones.UrlApiCentral) {
        Escribir-Seccion "Conectividad API"
        try {
            $uri = [Uri]$opciones.UrlApiCentral
            Test-NetConnection $uri.Host -Port $uri.Port
            Invoke-RestMethod -Uri ($opciones.UrlApiCentral.TrimEnd("/") + "/actuator/health") -TimeoutSec 10 | ConvertTo-Json -Depth 5
        } catch {
            Write-Warning "No se pudo consultar API: $($_.Exception.Message)"
        }
    }
} else {
    Write-Warning "No existe $configPath."
}

Escribir-Seccion "Credenciales"
$credPath = Join-Path $RutaAgente "State\credenciales.json"
if (Test-Path $credPath) {
    $cred = Get-Content $credPath -Raw | ConvertFrom-Json
    $cred | Select-Object IdEquipo, idEquipo, DeviceId, deviceId, CodigoSucursal, codigoSucursal, Proteccion, proteccion, CreatedAt, createdAt | Format-List
    Write-Host "Existe credenciales.json. No comparta el token completo como evidencia publica."
} else {
    Write-Warning "No existe credenciales.json. El agente probablemente no logro registrarse."
}

Escribir-Seccion "Estado local"
$estadoPath = Join-Path $RutaAgente "State\estado-agente.json"
if (Test-Path $estadoPath) {
    Get-Content $estadoPath
} else {
    Write-Warning "No existe estado-agente.json."
}

Escribir-Seccion "Logs recientes"
$logsPath = Join-Path $RutaAgente "Logs"
if (Test-Path $logsPath) {
    $logs = Get-ChildItem $logsPath -Filter "*.log" -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 2
    if ($logs) {
        foreach ($log in $logs) {
            Write-Host ""
            Write-Host "-- $($log.FullName)"
            Get-Content $log.FullName -Tail 120
        }
    } else {
        Write-Warning "No hay archivos .log en $logsPath."
    }
} else {
    Write-Warning "No existe carpeta Logs."
}

