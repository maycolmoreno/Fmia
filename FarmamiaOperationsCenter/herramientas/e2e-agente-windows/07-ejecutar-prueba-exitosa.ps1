param(
    [string]$ApiBaseUrl = "http://localhost:8081",
    [string]$RutaAgente = "C:\Program Files (x86)\Farmamia Cia Ltda - Elipsys\Agent",
    [string]$Version = "2026.06.3-piloto",
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

$paquete = & "$PSScriptRoot\05-crear-paquete-demo.ps1" -ApiBaseUrl $ApiBaseUrl -Version $Version -Modo "Valido" -UsuarioAdmin $UsuarioAdmin -ContrasenaAdmin $ContrasenaAdmin
$despliegue = & "$PSScriptRoot\06-crear-despliegue-demo.ps1" -ApiBaseUrl $ApiBaseUrl -PackageId $paquete.packageId -RutaAgente $RutaAgente -NombreDespliegue "E2E exitoso $Version" -TargetGroup "PILOTO-E2E-SUCCESS" -UsuarioAdmin $UsuarioAdmin -ContrasenaAdmin $ContrasenaAdmin

& $updater --agent-root=$RutaAgente buscar | Out-Host
& $updater --agent-root=$RutaAgente instalar-ahora | Out-Host

$estadoDespliegue = Invocar-FocJson -Metodo "GET" -Url "$ApiBaseUrl/api/deployments/$($despliegue.deploymentId)/status" -Token $tokenAdmin
$equipo = Invocar-FocJson -Metodo "GET" -Url "$ApiBaseUrl/api/devices/$($credenciales.idEquipo)" -Token $tokenAdmin
$eventos = @(Invocar-FocJson -Metodo "GET" -Url "$ApiBaseUrl/api/update-events?limit=100" -Token $tokenAdmin)
$alertas = @(Invocar-FocJson -Metodo "GET" -Url "$ApiBaseUrl/api/alerts?deviceId=$($credenciales.idEquipo)&limit=100" -Token $tokenAdmin)

$eventoCompletado = $eventos | Where-Object { $_.eventType -eq "UPDATE_COMPLETED" -and $_.newVersion -eq $Version } | Select-Object -First 1

if (-not $estadoDespliegue.targetsByStatus.COMPLETED -or $estadoDespliegue.targetsByStatus.COMPLETED -lt 1) {
    throw "El deployment target no quedo COMPLETED."
}
if ($equipo.device.posVersion -ne $Version) {
    throw "La version POS no se actualizo. Esperado=$Version Actual=$($equipo.device.posVersion)"
}
if (-not $eventoCompletado) {
    throw "No se encontro evento UPDATE_COMPLETED."
}
$alertaCritica = $alertas | Where-Object { $_.severity -eq "CRITICAL" -and $_.status -eq "OPEN" } | Select-Object -First 1
if ($alertaCritica) {
    throw "Se encontro alerta critica inesperada: $($alertaCritica.id)"
}

$resultado = [PSCustomObject]@{
    scenario = "SUCCESS"
    timestamp = (Get-Date).ToString("o")
    deviceId = $credenciales.idEquipo
    version = $Version
    packageId = $paquete.packageId
    deploymentId = $despliegue.deploymentId
    targetsByStatus = $estadoDespliegue.targetsByStatus
    posVersion = $equipo.device.posVersion
    updateCompletedEventId = $eventoCompletado.id
    criticalAlertsOpen = @($alertas | Where-Object { $_.severity -eq "CRITICAL" -and $_.status -eq "OPEN" }).Count
    downloads = @(Get-ChildItem (Join-Path $RutaAgente "Downloads") -Filter "*.zip" -ErrorAction SilentlyContinue | Select-Object Name, Length, LastWriteTime)
    backups = @(Get-ChildItem (Join-Path $RutaAgente "Backups") -Directory -ErrorAction SilentlyContinue | Select-Object Name, LastWriteTime)
    state = if (Test-Path (Join-Path $RutaAgente "State\estado-agente.json")) { Get-Content (Join-Path $RutaAgente "State\estado-agente.json") -Raw | ConvertFrom-Json } else { $null }
}

$rutaResultado = Join-Path $ResultadosDir "resultado-e2e-exitoso.json"
Escribir-ResultadoJson -Resultado $resultado -Ruta $rutaResultado
$resultado
