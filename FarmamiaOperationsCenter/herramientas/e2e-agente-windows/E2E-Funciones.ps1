$ErrorActionPreference = "Stop"

function Invocar-FocJson {
    param(
        [Parameter(Mandatory = $true)][string]$Metodo,
        [Parameter(Mandatory = $true)][string]$Url,
        [object]$Cuerpo,
        [string]$Token,
        [int]$TimeoutSec = 30
    )

    $encabezados = @{}
    if (-not [string]::IsNullOrWhiteSpace($Token)) {
        $encabezados["Authorization"] = "Bearer $Token"
    }

    $parametros = @{
        Method      = $Metodo
        Uri         = $Url
        ContentType = "application/json"
        Headers     = $encabezados
        TimeoutSec  = $TimeoutSec
    }

    if ($null -ne $Cuerpo) {
        $parametros["Body"] = ($Cuerpo | ConvertTo-Json -Depth 12)
    }

    Invoke-RestMethod @parametros
}

function Obtener-TokenAdmin {
    param(
        [Parameter(Mandatory = $true)][string]$ApiBaseUrl,
        [Parameter(Mandatory = $true)][string]$UsuarioAdmin,
        [Parameter(Mandatory = $true)][string]$ContrasenaAdmin
    )

    $login = Invocar-FocJson `
        -Metodo "POST" `
        -Url "$ApiBaseUrl/api/auth/login" `
        -Cuerpo @{
            username = $UsuarioAdmin
            password = $ContrasenaAdmin
        }

    if (-not $login.accessToken) {
        throw "Login administrativo no devolvio accessToken."
    }

    [string]$login.accessToken
}

function Obtener-CredencialesAgenteLocal {
    param(
        [Parameter(Mandatory = $true)][string]$RutaAgente
    )

    $rutaCredenciales = Join-Path $RutaAgente "State\credenciales.json"
    if (-not (Test-Path $rutaCredenciales)) {
        throw "No existen credenciales del agente en $rutaCredenciales. Verifique que el servicio haya registrado el equipo."
    }

    Get-Content $rutaCredenciales -Raw | ConvertFrom-Json
}

function Obtener-ConfigAgente {
    param(
        [Parameter(Mandatory = $true)][string]$RutaAgente
    )

    $rutaConfig = Join-Path $RutaAgente "config.json"
    if (-not (Test-Path $rutaConfig)) {
        throw "No existe config.json en $rutaConfig."
    }

    $config = Get-Content $rutaConfig -Raw | ConvertFrom-Json
    if ($config.AgenteFarmamia) {
        return $config.AgenteFarmamia
    }

    $config
}

function Esperar-Condicion {
    param(
        [Parameter(Mandatory = $true)][scriptblock]$Condicion,
        [int]$TimeoutSec = 120,
        [int]$IntervaloSec = 5,
        [string]$Mensaje = "La condicion no se cumplio dentro del tiempo esperado."
    )

    $fin = (Get-Date).AddSeconds($TimeoutSec)
    do {
        $resultado = & $Condicion
        if ($resultado) {
            return $resultado
        }

        Start-Sleep -Seconds $IntervaloSec
    } while ((Get-Date) -lt $fin)

    throw $Mensaje
}

function Enviar-PaqueteMultipart {
    param(
        [Parameter(Mandatory = $true)][string]$Url,
        [Parameter(Mandatory = $true)][string]$Version,
        [Parameter(Mandatory = $true)][string]$RutaArchivo,
        [Parameter(Mandatory = $true)][string]$Token
    )

    Add-Type -AssemblyName System.Net.Http

    $cliente = [System.Net.Http.HttpClient]::new()
    $contenido = [System.Net.Http.MultipartFormDataContent]::new()
    $bytes = [System.IO.File]::ReadAllBytes($RutaArchivo)
    $archivo = [System.Net.Http.ByteArrayContent]::new($bytes)
    $archivo.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse("application/zip")
    $contenido.Add([System.Net.Http.StringContent]::new($Version), "version")
    $contenido.Add($archivo, "file", [System.IO.Path]::GetFileName($RutaArchivo))
    $cliente.DefaultRequestHeaders.Authorization = [System.Net.Http.Headers.AuthenticationHeaderValue]::new("Bearer", $Token)

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

function Crear-ZipPosDemo {
    param(
        [Parameter(Mandatory = $true)][string]$Version,
        [Parameter(Mandatory = $true)][string]$Salida,
        [switch]$SinEjecutable
    )

    $raizTemporal = Join-Path $env:TEMP ("farmamia-e2e-pos-" + [Guid]::NewGuid().ToString("N"))
    $carpeta = Join-Path $raizTemporal "pos"
    New-Item -ItemType Directory -Force -Path $carpeta | Out-Null

    if (-not $SinEjecutable) {
        Set-Content -Path (Join-Path $carpeta "Zabyca.Pos.Desktop.exe") -Value "Ejecutable demo Farmamia POS $Version" -Encoding UTF8
    }

    Set-Content -Path (Join-Path $carpeta "version.txt") -Value $Version -Encoding UTF8
    Set-Content -Path (Join-Path $carpeta "README-e2e.txt") -Value "Paquete E2E Farmamia Operations Center $Version" -Encoding UTF8

    if (Test-Path $Salida) {
        Remove-Item -LiteralPath $Salida -Force
    }

    Compress-Archive -Path (Join-Path $carpeta "*") -DestinationPath $Salida -Force
    Remove-Item -LiteralPath $raizTemporal -Recurse -Force

    Get-Item $Salida
}

function Escribir-ResultadoJson {
    param(
        [Parameter(Mandatory = $true)][object]$Resultado,
        [Parameter(Mandatory = $true)][string]$Ruta
    )

    New-Item -ItemType Directory -Force -Path (Split-Path $Ruta -Parent) | Out-Null
    $Resultado | ConvertTo-Json -Depth 12 | Set-Content -Path $Ruta -Encoding UTF8
}
