param(
    [string]$ApiBaseUrl = "http://localhost:8081",
    [string]$CodigoSucursal = "FMA-DEMO-001",
    [string]$NombreEquipo = "POS-DEMO-001"
)

$ErrorActionPreference = "Stop"

function Invocar-Json {
    param(
        [string]$Metodo,
        [string]$Url,
        [object]$Cuerpo,
        [string]$Token
    )

    $encabezados = @{}
    if ($Token) {
        $encabezados["Authorization"] = "Bearer $Token"
    }

    $parametros = @{
        Method      = $Metodo
        Uri         = $Url
        ContentType = "application/json"
        Headers     = $encabezados
        TimeoutSec  = 15
    }

    if ($null -ne $Cuerpo) {
        $parametros["Body"] = ($Cuerpo | ConvertTo-Json -Depth 8)
    }

    Invoke-RestMethod @parametros
}

$registro = Invocar-Json `
    -Metodo "POST" `
    -Url "$ApiBaseUrl/api/agent/register" `
    -Cuerpo @{
        branchCode     = $CodigoSucursal
        hostname       = $NombreEquipo
        ipAddress      = "192.168.10.25"
        macAddress     = "00-11-22-33-44-55"
        windowsVersion = "Windows 11 Pro 23H2"
        agentVersion   = "0.1.0-demo"
        posVersion     = "2026.06.0"
        posPath        = "C:\Farmamia\POS"
    }

Invocar-Json `
    -Metodo "POST" `
    -Url "$ApiBaseUrl/api/agent/heartbeat" `
    -Token $registro.agentToken `
    -Cuerpo @{
        deviceId            = $registro.deviceId
        posVersion          = "2026.06.0"
        diskFreeMb          = 82340
        diskTotalMb         = 128000
        posProcessRunning   = $true
        latencyMs           = 18
        packetLossPercent   = 0.0
    } | Out-Null

Invocar-Json `
    -Metodo "POST" `
    -Url "$ApiBaseUrl/api/agent/$($registro.deviceId)/events" `
    -Token $registro.agentToken `
    -Cuerpo @{
        eventType       = "DOWNLOAD_STARTED"
        eventMessage    = "Evento demo generado desde script de desarrollo"
        oldVersion      = "2026.06.0"
        newVersion      = "2026.06.1"
        metadata        = @{
            origin = "registrar-agente-demo.ps1"
            mode   = "demo"
        }
    } | Out-Null

[PSCustomObject]@{
    deviceId   = $registro.deviceId
    agentToken = $registro.agentToken
    branchCode = $CodigoSucursal
    hostname   = $NombreEquipo
    apiBaseUrl  = $ApiBaseUrl
}
