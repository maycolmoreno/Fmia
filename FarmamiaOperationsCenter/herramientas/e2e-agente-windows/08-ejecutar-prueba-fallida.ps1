param(
    [string]$ApiBaseUrl = "http://localhost:8081",
    [string]$RutaAgente = "C:\Program Files (x86)\Farmamia Cia Ltda - Elipsys\Agent",
    [string]$Version = "2026.06.3-piloto-fail",
    [ValidateSet("SinEjecutable")]
    [string]$Modo = "SinEjecutable",
    [string]$UsuarioAdmin = "admin",
    [string]$ContrasenaAdmin = "admin123",
    [string]$ResultadosDir = ".\resultados"
)

. "$PSScriptRoot\E2E-Funciones.ps1"

$tokenAdmin = Obtener-TokenAdmin -ApiBaseUrl $ApiBaseUrl -UsuarioAdmin $UsuarioAdmin -ContrasenaAdmin $ContrasenaAdmin
$credenciales = Obtener-CredencialesAgenteLocal -RutaAgente $RutaAgente
$updater = Join-Path $RutaAgente "FarmamiaUpdater.exe"
if (-not (Test-Path $updater)) {
    throw "No existe FarmamiaUpdater.exe en $RutaAgente"
}

$equipoAntes = Invocar-FocJson -Metodo "GET" -Url "$ApiBaseUrl/api/devices/$($credenciales.idEquipo)" -Token $tokenAdmin
$versionAntes = $equipoAntes.device.posVersion

$paquete = & "$PSScriptRoot\05-crear-paquete-demo.ps1" -ApiBaseUrl $ApiBaseUrl -Version $Version -Modo $Modo -UsuarioAdmin $UsuarioAdmin -ContrasenaAdmin $ContrasenaAdmin
$despliegue = & "$PSScriptRoot\06-crear-despliegue-demo.ps1" -ApiBaseUrl $ApiBaseUrl -PackageId $paquete.packageId -RutaAgente $RutaAgente -NombreDespliegue "E2E fallido $Version" -TargetGroup "PILOTO-E2E-FAIL" -UsuarioAdmin $UsuarioAdmin -ContrasenaAdmin $ContrasenaAdmin

& $updater --agent-root=$RutaAgente buscar | Out-Host
try {
    & $updater --agent-root=$RutaAgente instalar-ahora | Out-Host
}
catch {
    Write-Host "El updater reporto fallo esperado: $($_.Exception.Message)"
}

$estadoDespliegue = Invocar-FocJson -Metodo "GET" -Url "$ApiBaseUrl/api/deployments/$($despliegue.deploymentId)/status" -Token $tokenAdmin
$equipoDespues = Invocar-FocJson -Metodo "GET" -Url "$ApiBaseUrl/api/devices/$($credenciales.idEquipo)" -Token $tokenAdmin
$eventos = @(Invocar-FocJson -Metodo "GET" -Url "$ApiBaseUrl/api/update-events?limit=100" -Token $tokenAdmin)
$alertas = @(Invocar-FocJson -Metodo "GET" -Url "$ApiBaseUrl/api/alerts?deviceId=$($credenciales.idEquipo)&limit=100" -Token $tokenAdmin)

$eventoValidacion = $eventos | Where-Object { $_.eventType -eq "VALIDATION_FAILED" -and $_.newVersion -eq $Version } | Select-Object -First 1
$eventoUpdateFailed = $eventos | Where-Object { $_.eventType -eq "UPDATE_FAILED" -and $_.newVersion -eq $Version } | Select-Object -First 1
$alertaCritica = $alertas | Where-Object { $_.severity -eq "CRITICAL" -and $_.status -eq "OPEN" } | Select-Object -First 1

if (-not $estadoDespliegue.targetsByStatus.FAILED -or $estadoDespliegue.targetsByStatus.FAILED -lt 1) {
    throw "El deployment target no quedo FAILED."
}
if ($equipoDespues.device.posVersion -ne $versionAntes) {
    throw "La version POS cambio en escenario fallido. Antes=$versionAntes Despues=$($equipoDespues.device.posVersion)"
}
if (-not $eventoValidacion -and -not $eventoUpdateFailed) {
    throw "No se encontro VALIDATION_FAILED ni UPDATE_FAILED."
}
if (-not $alertaCritica) {
    throw "No se encontro alerta critica abierta."
}

$resultado = [PSCustomObject]@{
    scenario = "FAILED"
    timestamp = (Get-Date).ToString("o")
    failureMode = $Modo
    deviceId = $credenciales.idEquipo
    previousPosVersion = $versionAntes
    currentPosVersion = $equipoDespues.device.posVersion
    versionAttempted = $Version
    packageId = $paquete.packageId
    deploymentId = $despliegue.deploymentId
    targetsByStatus = $estadoDespliegue.targetsByStatus
    validationFailedEventId = if ($eventoValidacion) { $eventoValidacion.id } else { $null }
    updateFailedEventId = if ($eventoUpdateFailed) { $eventoUpdateFailed.id } else { $null }
    criticalAlertId = $alertaCritica.id
    state = if (Test-Path (Join-Path $RutaAgente "State\estado-agente.json")) { Get-Content (Join-Path $RutaAgente "State\estado-agente.json") -Raw | ConvertFrom-Json } else { $null }
}

$rutaResultado = Join-Path $ResultadosDir "resultado-e2e-fallido.json"
Escribir-ResultadoJson -Resultado $resultado -Ruta $rutaResultado
$resultado
