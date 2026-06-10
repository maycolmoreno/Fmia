param(
    [string]$ApiBaseUrl = "http://localhost:8081",
    [string]$RutaAgente = "C:\Program Files (x86)\Farmamia Cia Ltda - Elipsys\Agent",
    [string]$UsuarioAdmin = "admin",
    [string]$ContrasenaAdmin = "admin123",
    [int]$TimeoutSec = 120
)

. "$PSScriptRoot\E2E-Funciones.ps1"

$tokenAdmin = Obtener-TokenAdmin -ApiBaseUrl $ApiBaseUrl -UsuarioAdmin $UsuarioAdmin -ContrasenaAdmin $ContrasenaAdmin

$credenciales = Esperar-Condicion -TimeoutSec $TimeoutSec -IntervaloSec 5 -Mensaje "El agente no persistio credenciales locales." -Condicion {
    $ruta = Join-Path $RutaAgente "State\credenciales.json"
    if (Test-Path $ruta) {
        return (Get-Content $ruta -Raw | ConvertFrom-Json)
    }
    return $null
}

$equipo = Esperar-Condicion -TimeoutSec $TimeoutSec -IntervaloSec 5 -Mensaje "No se encontro heartbeat/equipo registrado en backend." -Condicion {
    try {
        $detalle = Invocar-FocJson -Metodo "GET" -Url "$ApiBaseUrl/api/devices/$($credenciales.idEquipo)" -Token $tokenAdmin
        if ($detalle) {
            return $detalle
        }
    }
    catch {
        return $null
    }
}

[PSCustomObject]@{
    deviceId = $credenciales.idEquipo
    tokenPersisted = -not [string]::IsNullOrWhiteSpace($credenciales.tokenAgente)
    device = $equipo.device
}
