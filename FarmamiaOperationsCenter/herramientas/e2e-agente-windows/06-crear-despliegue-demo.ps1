param(
    [string]$ApiBaseUrl = "http://localhost:8081",
    [Parameter(Mandatory = $true)][string]$PackageId,
    [string]$RutaAgente = "C:\Program Files (x86)\Farmamia Cia Ltda - Elipsys\Agent",
    [string]$NombreDespliegue = "E2E piloto POS",
    [string]$TargetGroup = "PILOTO-E2E",
    [string]$UsuarioAdmin = "admin",
    [string]$ContrasenaAdmin = "admin123"
)

. "$PSScriptRoot\E2E-Funciones.ps1"

$tokenAdmin = Obtener-TokenAdmin -ApiBaseUrl $ApiBaseUrl -UsuarioAdmin $UsuarioAdmin -ContrasenaAdmin $ContrasenaAdmin
$credenciales = Obtener-CredencialesAgenteLocal -RutaAgente $RutaAgente

$idsEquipos = [System.Collections.Generic.List[string]]::new()
$idsEquipos.Add([string]$credenciales.idEquipo)

$despliegue = Invocar-FocJson `
    -Metodo "POST" `
    -Url "$ApiBaseUrl/api/deployments" `
    -Token $tokenAdmin `
    -Cuerpo @{
        packageId = $PackageId
        name = $NombreDespliegue
        description = "Despliegue E2E real del agente Windows"
        scheduledAt = $null
        targetGroup = $TargetGroup
        pilot = $true
        deviceIds = $idsEquipos
    }

[PSCustomObject]@{
    deploymentId = $despliegue.id
    deploymentName = $despliegue.name
    deploymentStatus = $despliegue.status
    deviceId = $credenciales.idEquipo
    packageId = $PackageId
}
