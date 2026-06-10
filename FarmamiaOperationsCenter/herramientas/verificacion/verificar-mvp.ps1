param(
    [switch]$OmitirBackend,
    [switch]$OmitirAngular,
    [switch]$OmitirAgente,
    [switch]$OmitirTestsAngular,
    [switch]$ModoRapido
)

$ErrorActionPreference = "Stop"

$raiz = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$backend = Join-Path $raiz "backend-api"
$panel = Join-Path $raiz "admin-panel"
$agente = Join-Path $raiz "windows-agent"

function Escribir-Seccion($mensaje) {
    Write-Host ""
    Write-Host "== $mensaje ==" -ForegroundColor Cyan
}

function Ejecutar($descripcion, $directorio, $comando) {
    Escribir-Seccion $descripcion
    Push-Location $directorio
    try {
        & powershell -NoProfile -ExecutionPolicy Bypass -Command $comando
    } finally {
        Pop-Location
    }
}

function Validar-Archivo($rutaRelativa) {
    $ruta = Join-Path $raiz $rutaRelativa
    if (-not (Test-Path $ruta)) {
        throw "Falta archivo requerido: $rutaRelativa"
    }
    Write-Host "OK $rutaRelativa"
}

function Validar-Directorio($rutaRelativa) {
    $ruta = Join-Path $raiz $rutaRelativa
    if (-not (Test-Path $ruta -PathType Container)) {
        throw "Falta carpeta requerida: $rutaRelativa"
    }
    Write-Host "OK $rutaRelativa"
}

Escribir-Seccion "Validacion de estructura"
@(
    "backend-api",
    "backend-api\src\main\resources\db\migration",
    "admin-panel",
    "windows-agent",
    "contracts",
    "herramientas\e2e-agente-windows",
    "herramientas\desarrollo",
    "herramientas\verificacion",
    "infraestructura\local"
) | ForEach-Object { Validar-Directorio $_ }

@(
    "backend-api\mvnw.cmd",
    "backend-api\mvnw",
    "backend-api\.mvn\wrapper\maven-wrapper.properties",
    "infraestructura\local\docker-compose.mvp.yml",
    "herramientas\verificacion\levantar-stack-mvp-local.ps1",
    "herramientas\verificacion\detener-stack-mvp-local.ps1",
    "herramientas\verificacion\ejecutar-e2e-demo-local.ps1",
    "herramientas\e2e-agente-windows\01-preparar-entorno.ps1",
    "herramientas\e2e-agente-windows\02-instalar-agente.ps1",
    "herramientas\e2e-agente-windows\03-validar-servicio.ps1",
    "herramientas\e2e-agente-windows\04-validar-heartbeat.ps1",
    "herramientas\e2e-agente-windows\05-crear-paquete-demo.ps1",
    "herramientas\e2e-agente-windows\06-crear-despliegue-demo.ps1",
    "herramientas\e2e-agente-windows\07-ejecutar-prueba-exitosa.ps1",
    "herramientas\e2e-agente-windows\08-ejecutar-prueba-fallida.ps1"
) | ForEach-Object { Validar-Archivo $_ }

if (-not $OmitirAngular) {
    Ejecutar "Build Angular" $panel "npm run build"

    $specs = Get-ChildItem (Join-Path $panel "src") -Recurse -Filter "*.spec.ts" -ErrorAction SilentlyContinue
    if ($specs -and -not $OmitirTestsAngular) {
        Ejecutar "Tests Angular" $panel "npm test -- --watch=false"
    } else {
        Escribir-Seccion "Tests Angular"
        if ($OmitirTestsAngular) {
            Write-Host "OMITIDO por parametro -OmitirTestsAngular."
        } else {
            Write-Host "OMITIDO: no se encontraron archivos *.spec.ts."
        }
    }
}

if (-not $OmitirBackend) {
    $dockerDisponible = $false
    try {
        docker info *> $null
        $dockerDisponible = $true
    } catch {
        $dockerDisponible = $false
    }

    Escribir-Seccion "Docker/Testcontainers"
    if ($dockerDisponible) {
        Write-Host "Docker disponible: las pruebas de integracion Testcontainers deben ejecutarse."
    } else {
        Write-Host "Docker no disponible: las pruebas anotadas con @Testcontainers(disabledWithoutDocker = true) seran omitidas por JUnit/Testcontainers."
    }

    Ejecutar "Tests backend con Maven Wrapper" $backend ".\mvnw.cmd test"
}

if (-not $OmitirAgente) {
    Ejecutar "Tests agente Windows .NET" $agente "`$env:DOTNET_ROLL_FORWARD='Major'; dotnet test Farmamia.Agent.Tests\Farmamia.Agent.Tests.csproj"
}

if (-not $ModoRapido) {
    Escribir-Seccion "Scripts demo/E2E disponibles"
    @(
        "herramientas\desarrollo\registrar-agente-demo.ps1",
        "herramientas\desarrollo\crear-despliegue-demo.ps1",
        "herramientas\desarrollo\crear-alerta-fallo-demo.ps1",
        "herramientas\desarrollo\crear-actualizacion-exitosa-demo.ps1"
    ) | ForEach-Object { Validar-Archivo $_ }
}

Escribir-Seccion "Resultado"
Write-Host "Linea base MVP verificada con los componentes habilitados."
