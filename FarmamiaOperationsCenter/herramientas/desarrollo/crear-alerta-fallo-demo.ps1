param(
    [string]$ApiBaseUrl = "http://localhost:8081",
    [string]$Version = "2026.06.2-fail",
    [string]$NombreDespliegue = "Despliegue demo POS fallido",
    [string]$CodigoSucursal = "FMA-DEMO-001",
    [string]$NombreEquipo = "POS-DEMO-001",
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

function Crear-Zip-Fallido {
    param(
        [string]$VersionPaquete
    )

    $raizTemporal = Join-Path $env:TEMP "farmamia-pos-demo-fallido"
    $carpetaPos = Join-Path $raizTemporal "pos"
    $zip = Join-Path $raizTemporal "pos-demo-$VersionPaquete.zip"

    if (Test-Path $raizTemporal) {
        Remove-Item -LiteralPath $raizTemporal -Recurse -Force
    }

    New-Item -ItemType Directory -Path $carpetaPos | Out-Null
    Set-Content -Path (Join-Path $carpetaPos "version.txt") -Value $VersionPaquete
    Set-Content -Path (Join-Path $carpetaPos "README-fallo-demo.txt") -Value "ZIP intencionalmente invalido: no contiene Zabyca.Pos.Desktop.exe."
    Compress-Archive -Path (Join-Path $carpetaPos "*") -DestinationPath $zip -Force

    $zip
}

function Validar-Zip-Agente {
    param(
        [string]$RutaZip
    )

    $archivoZip = [System.IO.Compression.ZipFile]::OpenRead($RutaZip)
    try {
        $ejecutable = $archivoZip.Entries | Where-Object {
            $_.FullName -replace "\\", "/" -eq "Zabyca.Pos.Desktop.exe"
        } | Select-Object -First 1

        if (-not $ejecutable) {
            throw "Validacion final fallida: el ZIP no contiene Zabyca.Pos.Desktop.exe."
        }
    }
    finally {
        $archivoZip.Dispose()
    }
}

$registro = @(& "$PSScriptRoot\registrar-agente-demo.ps1" `
    -ApiBaseUrl $ApiBaseUrl `
    -CodigoSucursal $CodigoSucursal `
    -NombreEquipo $NombreEquipo) | Select-Object -Last 1

$tokenAgente = [string]$registro.agentToken
$idEquipo = [string]$registro.deviceId

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

$paquetes = Invocar-Json -Metodo "GET" -Url "$ApiBaseUrl/api/packages" -Token $tokenAdmin
$paquete = $paquetes | Where-Object { $_.version -eq $Version } | Select-Object -First 1

if (-not $paquete) {
    $zipFallido = Crear-Zip-Fallido -VersionPaquete $Version
    $paquete = Enviar-MultipartPaquete `
        -Url "$ApiBaseUrl/api/packages" `
        -VersionPaquete $Version `
        -RutaArchivo $zipFallido `
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
        description = "Campana demo E2E fallida: ZIP sin ejecutable principal"
        scheduledAt = $null
        targetGroup = "DEMO-FAIL"
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

$descarga = Join-Path $env:TEMP "farmamia-pos-demo-fallido-descarga-$Version.zip"
Invoke-WebRequest `
    -Uri "$ApiBaseUrl$($instruccion.downloadUrl)" `
    -Headers @{ Authorization = "Bearer $tokenAgente" } `
    -OutFile $descarga `
    -TimeoutSec 30

$mensajeFallo = $null
try {
    Validar-Zip-Agente -RutaZip $descarga
    throw "La demo esperaba un ZIP invalido, pero la validacion final no fallo."
}
catch {
    $mensajeFallo = $_.Exception.Message
}

Invocar-Json `
    -Metodo "POST" `
    -Url "$ApiBaseUrl/api/agent/$idEquipo/events" `
    -Token $tokenAgente `
    -Cuerpo @{
        deploymentTargetId = $instruccion.deploymentTargetId
        eventType          = "VALIDATION_FAILED"
        eventMessage       = $mensajeFallo
        oldVersion         = $versionAnterior
        newVersion         = $Version
        metadata           = @{
            origin        = "crear-alerta-fallo-demo.ps1"
            failureMode   = "ZIP_WITHOUT_EXECUTABLE"
            downloadedZip = $descarga
        }
    } | Out-Null

Invocar-Json `
    -Metodo "POST" `
    -Url "$ApiBaseUrl/api/agent/$idEquipo/update-result" `
    -Token $tokenAgente `
    -Cuerpo @{
        deploymentTargetId = $instruccion.deploymentTargetId
        status             = "FAILED"
        oldVersion         = $versionAnterior
        newVersion         = $Version
        message            = $mensajeFallo
    } | Out-Null

$estadoDespliegue = Invocar-Json `
    -Metodo "GET" `
    -Url "$ApiBaseUrl/api/deployments/$($despliegue.id)/status" `
    -Token $tokenAdmin

$equipoFinal = Invocar-Json `
    -Metodo "GET" `
    -Url "$ApiBaseUrl/api/devices/$idEquipo" `
    -Token $tokenAdmin

$eventos = Invocar-Json -Metodo "GET" -Url "$ApiBaseUrl/api/update-events?limit=20" -Token $tokenAdmin
$alertas = Invocar-Json -Metodo "GET" -Url "$ApiBaseUrl/api/alerts?limit=100" -Token $tokenAdmin
$alertaGenerada = $alertas | Where-Object {
    $_.deviceId -eq $idEquipo -and $_.alertType -eq "UPDATE_FAILED" -and $_.message -eq $mensajeFallo
} | Select-Object -First 1

if (-not $alertaGenerada) {
    throw "No se encontro la alerta critica esperada en /api/alerts."
}

[PSCustomObject]@{
    deviceId           = $idEquipo
    hostname           = $equipo.hostname
    previousPosVersion = $versionAnterior
    currentPosVersion  = $equipoFinal.device.posVersion
    packageId          = $paquete.id
    packageVersion     = $paquete.version
    deploymentId       = $despliegue.id
    deploymentStatus   = $despliegue.status
    targetId           = $instruccion.deploymentTargetId
    targetsByStatus    = $estadoDespliegue.targetsByStatus
    failedEvents       = @($eventos | Where-Object { $_.deploymentTargetId -eq $instruccion.deploymentTargetId }).Count
    alertId            = $alertaGenerada.id
    alertSeverity      = $alertaGenerada.severity
    alertType          = $alertaGenerada.alertType
    alertMessage       = $alertaGenerada.message
}
