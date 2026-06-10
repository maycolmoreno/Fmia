param(
    [string]$ApiBaseUrl = "http://localhost:8081",
    [string]$Version = "2026.06.2-success",
    [string]$NombreDespliegue = "Despliegue demo POS exitoso",
    [string]$CodigoSucursal = "FMA-DEMO-001",
    [string]$NombreEquipo = "POS-DEMO-SUCCESS-001",
    [string]$UsuarioAdmin = "admin",
    [string]$ContrasenaAdmin = "admin123"
)

$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Net.Http
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem

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
        TimeoutSec  = 25
    }

    $json = $null
    if ($null -ne $Cuerpo) {
        $json = ($Cuerpo | ConvertTo-Json -Depth 10)
        $parametros["Body"] = $json
    }

    try {
        Invoke-RestMethod @parametros
    }
    catch {
        if ($json) {
            Write-Host "Error invocando $Metodo $Url"
            Write-Host $json
        }
        throw
    }
}

function Enviar-MultipartPaquete {
    param(
        [string]$Url,
        [string]$VersionPaquete,
        [string]$RutaArchivo,
        [string]$Token
    )

    $cliente = [System.Net.Http.HttpClient]::new()
    $contenido = [System.Net.Http.MultipartFormDataContent]::new()
    $bytes = [System.IO.File]::ReadAllBytes($RutaArchivo)
    $archivo = [System.Net.Http.ByteArrayContent]::new($bytes)
    $archivo.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse("application/zip")

    $contenido.Add([System.Net.Http.StringContent]::new($VersionPaquete), "version")
    $contenido.Add($archivo, "file", [System.IO.Path]::GetFileName($RutaArchivo))
    if ($Token) {
        $cliente.DefaultRequestHeaders.Authorization = [System.Net.Http.Headers.AuthenticationHeaderValue]::new("Bearer", $Token)
    }

    try {
        $respuesta = $cliente.PostAsync($Url, $contenido).GetAwaiter().GetResult()
        $texto = $respuesta.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        if (-not $respuesta.IsSuccessStatusCode) {
            throw "No se pudo cargar paquete. HTTP $([int]$respuesta.StatusCode): $texto"
        }

        $texto | ConvertFrom-Json
    }
    finally {
        $archivo.Dispose()
        $contenido.Dispose()
        $cliente.Dispose()
    }
}

function Crear-Zip-Valido {
    param(
        [string]$VersionPaquete
    )

    $raizTemporal = Join-Path $env:TEMP "farmamia-pos-demo-exitoso"
    $carpetaPos = Join-Path $raizTemporal "pos"
    $zip = Join-Path $raizTemporal "pos-demo-$VersionPaquete.zip"

    if (Test-Path $raizTemporal) {
        Remove-Item -LiteralPath $raizTemporal -Recurse -Force
    }

    New-Item -ItemType Directory -Path $carpetaPos | Out-Null
    Set-Content -Path (Join-Path $carpetaPos "Zabyca.Pos.Desktop.exe") -Value "Ejecutable demo valido para Farmamia POS $VersionPaquete."
    Set-Content -Path (Join-Path $carpetaPos "version.txt") -Value $VersionPaquete
    Set-Content -Path (Join-Path $carpetaPos "README-success-demo.txt") -Value "ZIP valido para escenario E2E exitoso."
    Compress-Archive -Path (Join-Path $carpetaPos "*") -DestinationPath $zip -Force

    $zip
}

function Validar-Ejecutable {
    param(
        [string]$RutaPos
    )

    $ejecutable = Join-Path $RutaPos "Zabyca.Pos.Desktop.exe"
    if (-not (Test-Path $ejecutable)) {
        throw "Validacion final fallida: no existe Zabyca.Pos.Desktop.exe en $RutaPos."
    }
}

function Reportar-Evento {
    param(
        [string]$Tipo,
        [string]$Mensaje,
        [object]$Metadatos
    )

    Invocar-Json `
        -Metodo "POST" `
        -Url "$ApiBaseUrl/api/agent/$idEquipo/events" `
        -Token $tokenAgente `
        -Cuerpo @{
            deploymentTargetId = $instruccion.deploymentTargetId
            eventType          = $Tipo
            eventMessage       = $Mensaje
            oldVersion         = $versionAnterior
            newVersion         = $Version
            metadata           = $Metadatos
        } | Out-Null
}

$registro = Invocar-Json `
    -Metodo "POST" `
    -Url "$ApiBaseUrl/api/agent/register" `
    -Cuerpo @{
        branchCode     = $CodigoSucursal
        hostname       = $NombreEquipo
        ipAddress      = "192.168.10.26"
        macAddress     = "00-11-22-33-44-66"
        windowsVersion = "Windows 11 Pro 23H2"
        agentVersion   = "0.1.0-demo"
        posVersion     = "2026.06.1"
        posPath        = "C:\Farmamia\POS"
    }

$tokenAgente = [string]$registro.agentToken
$idEquipo = [string]$registro.deviceId

Invocar-Json `
    -Metodo "POST" `
    -Url "$ApiBaseUrl/api/agent/heartbeat" `
    -Token $tokenAgente `
    -Cuerpo @{
        deviceId          = $idEquipo
        posVersion        = "2026.06.1"
        diskFreeMb        = 80320
        diskTotalMb       = 128000
        posProcessRunning = $false
        latencyMs         = 16
        packetLossPercent = 0.0
    } | Out-Null

$login = Invocar-Json `
    -Metodo "POST" `
    -Url "$ApiBaseUrl/api/auth/login" `
    -Cuerpo @{
        username = $UsuarioAdmin
        password = $ContrasenaAdmin
    }

$tokenAdmin = $login.accessToken
$equipos = Invocar-Json -Metodo "GET" -Url "$ApiBaseUrl/api/devices" -Token $tokenAdmin
$equipo = $equipos | Where-Object { $_.id -eq $idEquipo } | Select-Object -First 1
$versionAnterior = $equipo.posVersion

$alertasAntes = @(Invocar-Json -Metodo "GET" -Url "$ApiBaseUrl/api/alerts?limit=100" -Token $tokenAdmin | Where-Object { $_.deviceId -eq $idEquipo })

$paquetes = Invocar-Json -Metodo "GET" -Url "$ApiBaseUrl/api/packages" -Token $tokenAdmin
$paquete = $paquetes | Where-Object { $_.version -eq $Version } | Select-Object -First 1

if (-not $paquete) {
    $zipValido = Crear-Zip-Valido -VersionPaquete $Version
    $paquete = Enviar-MultipartPaquete `
        -Url "$ApiBaseUrl/api/packages" `
        -VersionPaquete $Version `
        -RutaArchivo $zipValido `
        -Token $tokenAdmin
}

if ($paquete.status -ne "APPROVED") {
    $paquete = Invocar-Json `
        -Metodo "POST" `
        -Url "$ApiBaseUrl/api/packages/$($paquete.id)/approve" `
        -Cuerpo @{} `
        -Token $tokenAdmin
}

$idsEquipos = [System.Collections.Generic.List[string]]::new()
$idsEquipos.Add($idEquipo)

$despliegue = Invocar-Json `
    -Metodo "POST" `
    -Url "$ApiBaseUrl/api/deployments" `
    -Cuerpo @{
        packageId   = [string]$paquete.id
        name        = $NombreDespliegue
        description = "Campana demo E2E exitosa: ZIP valido con ejecutable principal"
        scheduledAt = $null
        targetGroup = "DEMO-SUCCESS"
        pilot       = $true
        deviceIds   = $idsEquipos
    } `
    -Token $tokenAdmin

$instruccion = Invocar-Json `
    -Metodo "GET" `
    -Url "$ApiBaseUrl/api/agent/$idEquipo/instructions" `
    -Token $tokenAgente

if (-not $instruccion.hasInstruction) {
    throw "El agente demo no recibio instruccion UPDATE_POS."
}

$descarga = Join-Path $env:TEMP "farmamia-pos-demo-exitoso-descarga-$Version.zip"
Invoke-WebRequest `
    -Uri "$ApiBaseUrl$($instruccion.downloadUrl)" `
    -Headers @{ Authorization = "Bearer $tokenAgente" } `
    -OutFile $descarga `
    -TimeoutSec 30

Reportar-Evento -Tipo "DOWNLOAD_COMPLETED" -Mensaje "Descarga de paquete completada" -Metadatos @{
    origin = "crear-actualizacion-exitosa-demo.ps1"
    file   = $descarga
}

$hashDescarga = (Get-FileHash -Path $descarga -Algorithm SHA256).Hash.ToLowerInvariant()
if ($hashDescarga -ne ([string]$instruccion.sha256Checksum).ToLowerInvariant()) {
    throw "Checksum invalido. Esperado $($instruccion.sha256Checksum), obtenido $hashDescarga."
}

Reportar-Evento -Tipo "CHECKSUM_VALIDATED" -Mensaje "Checksum SHA-256 validado" -Metadatos @{
    origin = "crear-actualizacion-exitosa-demo.ps1"
    sha256 = $hashDescarga
}

$raizRuntime = Join-Path $env:TEMP "farmamia-pos-runtime-success"
$rutaPos = Join-Path $raizRuntime "POS"
$rutaBackup = Join-Path $raizRuntime ("Backups\POS-" + (Get-Date -Format "yyyyMMddHHmmss"))
$rutaExtraccion = Join-Path $raizRuntime "Extracted"

if (Test-Path $rutaExtraccion) {
    Remove-Item -LiteralPath $rutaExtraccion -Recurse -Force
}
if (-not (Test-Path $rutaPos)) {
    New-Item -ItemType Directory -Path $rutaPos | Out-Null
    Set-Content -Path (Join-Path $rutaPos "Zabyca.Pos.Desktop.exe") -Value "Ejecutable demo anterior."
    Set-Content -Path (Join-Path $rutaPos "version.txt") -Value $versionAnterior
}

New-Item -ItemType Directory -Path $rutaBackup | Out-Null
Copy-Item -Path (Join-Path $rutaPos "*") -Destination $rutaBackup -Recurse -Force

Reportar-Evento -Tipo "BACKUP_CREATED" -Mensaje "Respaldo POS creado" -Metadatos @{
    origin = "crear-actualizacion-exitosa-demo.ps1"
    backup = $rutaBackup
}

Reportar-Evento -Tipo "UPDATE_STARTED" -Mensaje "Actualizacion POS iniciada" -Metadatos @{
    origin = "crear-actualizacion-exitosa-demo.ps1"
}

Expand-Archive -Path $descarga -DestinationPath $rutaExtraccion -Force
Copy-Item -Path (Join-Path $rutaExtraccion "*") -Destination $rutaPos -Recurse -Force
Validar-Ejecutable -RutaPos $rutaPos

Reportar-Evento -Tipo "VALIDATION_OK" -Mensaje "Validacion POS correcta" -Metadatos @{
    origin = "crear-actualizacion-exitosa-demo.ps1"
    posPath = $rutaPos
}

Invocar-Json `
    -Metodo "POST" `
    -Url "$ApiBaseUrl/api/agent/$idEquipo/update-result" `
    -Token $tokenAgente `
    -Cuerpo @{
        deploymentTargetId = $instruccion.deploymentTargetId
        status             = "COMPLETED"
        oldVersion         = $versionAnterior
        newVersion         = $Version
        message            = "Actualizacion demo completada correctamente."
    } | Out-Null

$estadoDespliegue = Invocar-Json `
    -Metodo "GET" `
    -Url "$ApiBaseUrl/api/deployments/$($despliegue.id)/status" `
    -Token $tokenAdmin

$equipoFinal = Invocar-Json `
    -Metodo "GET" `
    -Url "$ApiBaseUrl/api/devices/$idEquipo" `
    -Token $tokenAdmin

$eventos = Invocar-Json -Metodo "GET" -Url "$ApiBaseUrl/api/update-events?limit=50" -Token $tokenAdmin
$eventosTarget = @($eventos | Where-Object { $_.deploymentTargetId -eq $instruccion.deploymentTargetId })
$eventoCompletado = $eventosTarget | Where-Object { $_.eventType -eq "UPDATE_COMPLETED" } | Select-Object -First 1
$alertasDespues = @(Invocar-Json -Metodo "GET" -Url "$ApiBaseUrl/api/alerts?limit=100" -Token $tokenAdmin | Where-Object { $_.deviceId -eq $idEquipo })

if (-not $estadoDespliegue.targetsByStatus.COMPLETED -or $estadoDespliegue.targetsByStatus.COMPLETED -lt 1) {
    throw "El target no quedo en estado COMPLETED."
}

if ($equipoFinal.device.posVersion -ne $Version) {
    throw "La version POS esperada era $Version, pero el equipo quedo en $($equipoFinal.device.posVersion)."
}

if (-not $eventoCompletado) {
    throw "No se encontro evento UPDATE_COMPLETED para el target."
}

if ($alertasDespues.Count -ne $alertasAntes.Count) {
    throw "Se genero una alerta inesperada para el equipo exitoso."
}

[PSCustomObject]@{
    deviceId           = $idEquipo
    hostname           = $equipoFinal.device.hostname
    previousPosVersion = $versionAnterior
    currentPosVersion  = $equipoFinal.device.posVersion
    packageId          = $paquete.id
    packageVersion     = $paquete.version
    deploymentId       = $despliegue.id
    deploymentStatus   = $despliegue.status
    targetId           = $instruccion.deploymentTargetId
    targetsByStatus    = $estadoDespliegue.targetsByStatus
    updateEvents       = $eventosTarget.Count
    completedEventId   = $eventoCompletado.id
    alertsBefore       = $alertasAntes.Count
    alertsAfter        = $alertasDespues.Count
    localPosPath       = $rutaPos
    backupPath         = $rutaBackup
}
