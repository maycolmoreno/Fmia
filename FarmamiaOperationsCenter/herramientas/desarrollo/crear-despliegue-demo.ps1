param(
    [string]$ApiBaseUrl = "http://localhost:8081",
    [string]$Version = "2026.06.1-demo",
    [string]$NombreDespliegue = "Despliegue demo POS",
    [string]$CodigoSucursal = "FMA-DEMO-001",
    [string]$NombreEquipo = "POS-DEMO-001",
    [string]$UsuarioAdmin = "admin",
    [string]$ContrasenaAdmin = "admin123"
)

$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Net.Http

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
        TimeoutSec  = 20
    }

    if ($null -ne $Cuerpo) {
        $parametros["Body"] = ($Cuerpo | ConvertTo-Json -Depth 8)
    }

    Invoke-RestMethod @parametros
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

$login = Invocar-Json `
    -Metodo "POST" `
    -Url "$ApiBaseUrl/api/auth/login" `
    -Cuerpo @{
        username = $UsuarioAdmin
        password = $ContrasenaAdmin
    }

$tokenAdmin = $login.accessToken

$equipos = @(Invocar-Json -Metodo "GET" -Url "$ApiBaseUrl/api/devices" -Token $tokenAdmin)

if ($equipos.Count -eq 0) {
    & "$PSScriptRoot\registrar-agente-demo.ps1" `
        -ApiBaseUrl $ApiBaseUrl `
        -CodigoSucursal $CodigoSucursal `
        -NombreEquipo $NombreEquipo | Out-Null

    $equipos = @(Invocar-Json -Metodo "GET" -Url "$ApiBaseUrl/api/devices" -Token $tokenAdmin)
}

$equipo = $equipos | Where-Object { $_.hostname -eq $NombreEquipo } | Select-Object -First 1
if (-not $equipo) {
    $equipo = $equipos | Select-Object -First 1
}

if (-not $equipo) {
    throw "No hay equipos disponibles para crear el despliegue demo."
}

$raizTemporal = Join-Path $env:TEMP "farmamia-pos-demo"
$carpetaPos = Join-Path $raizTemporal "pos"
$zip = Join-Path $raizTemporal "pos-demo-$Version.zip"

if (Test-Path $raizTemporal) {
    Remove-Item -LiteralPath $raizTemporal -Recurse -Force
}

New-Item -ItemType Directory -Path $carpetaPos | Out-Null
Set-Content -Path (Join-Path $carpetaPos "Zabyca.Pos.Desktop.exe") -Value "Archivo demo para validacion del agente Farmamia."
Set-Content -Path (Join-Path $carpetaPos "version.txt") -Value $Version
Compress-Archive -Path (Join-Path $carpetaPos "*") -DestinationPath $zip -Force

$paquete = Enviar-MultipartPaquete `
    -Url "$ApiBaseUrl/api/packages" `
    -VersionPaquete $Version `
    -RutaArchivo $zip `
    -Token $tokenAdmin

$paqueteAprobado = Invocar-Json `
    -Metodo "POST" `
    -Url "$ApiBaseUrl/api/packages/$($paquete.id)/approve" `
    -Cuerpo @{} `
    -Token $tokenAdmin

$despliegue = Invocar-Json `
    -Metodo "POST" `
    -Url "$ApiBaseUrl/api/deployments" `
    -Cuerpo @{
        packageId   = $paqueteAprobado.id
        name        = $NombreDespliegue
        description = "Campana demo creada desde herramientas/desarrollo"
        scheduledAt = $null
        targetGroup = "DEMO"
        pilot       = $true
        deviceIds   = @($equipo.id)
    } `
    -Token $tokenAdmin

[PSCustomObject]@{
    deviceId       = $equipo.id
    hostname       = $equipo.hostname
    packageId      = $paqueteAprobado.id
    packageVersion = $paqueteAprobado.version
    deploymentId   = $despliegue.id
    deploymentName = $despliegue.name
    deploymentStatus = $despliegue.status
}
