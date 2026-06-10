param(
    [string]$ApiBaseUrl = "http://localhost:8081",
    [string]$Version = "2026.06.3-piloto",
    [ValidateSet("Valido", "SinEjecutable")]
    [string]$Modo = "Valido",
    [string]$UsuarioAdmin = "admin",
    [string]$ContrasenaAdmin = "admin123",
    [string]$SalidaZip = ""
)

. "$PSScriptRoot\E2E-Funciones.ps1"

$tokenAdmin = Obtener-TokenAdmin -ApiBaseUrl $ApiBaseUrl -UsuarioAdmin $UsuarioAdmin -ContrasenaAdmin $ContrasenaAdmin

if ([string]::IsNullOrWhiteSpace($SalidaZip)) {
    $SalidaZip = Join-Path $env:TEMP "farmamia-e2e-$Version-$Modo.zip"
}

$sinEjecutable = $Modo -eq "SinEjecutable"
$zip = Crear-ZipPosDemo -Version $Version -Salida $SalidaZip -SinEjecutable:$sinEjecutable
$paquete = Enviar-PaqueteMultipart -Url "$ApiBaseUrl/api/packages" -Version $Version -RutaArchivo $zip.FullName -Token $tokenAdmin

if ($paquete.status -ne "APPROVED") {
    $paquete = Invocar-FocJson -Metodo "POST" -Url "$ApiBaseUrl/api/packages/$($paquete.id)/approve" -Cuerpo @{} -Token $tokenAdmin
}

[PSCustomObject]@{
    packageId = $paquete.id
    version = $paquete.version
    status = $paquete.status
    mode = $Modo
    localZip = $zip.FullName
    localSha256 = (Get-FileHash -Path $zip.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
}
