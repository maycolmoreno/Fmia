# Publica el ejecutable de prueba "Zabyca.Pos.Desktop.exe" (el mismo usado por las pruebas
# unitarias del agente, agent\Farmamia.Agent.Tests.PosSimulado) como un POS de laboratorio real
# y ejecutable, para poder probar el cierre/reapertura de proceso (IProcesoPos) en una maquina
# real sin necesitar el binario propietario de Zabyca.
param(
    [string]$RutaPos = "C:\FarmamiaLab\PosDemo"
)

$ErrorActionPreference = "Stop"

$raizAgente = Resolve-Path (Join-Path $PSScriptRoot "..\..\agent")
$proyectoSimulado = Join-Path $raizAgente "Farmamia.Agent.Tests.PosSimulado\Farmamia.Agent.Tests.PosSimulado.csproj"

if (-not (Test-Path $proyectoSimulado)) {
    throw "No se encontro $proyectoSimulado"
}

New-Item -ItemType Directory -Force -Path $RutaPos | Out-Null

dotnet publish $proyectoSimulado -c Release -r win-x64 --self-contained false -o $RutaPos
if ($LASTEXITCODE -ne 0) {
    throw "dotnet publish fallo con codigo $LASTEXITCODE"
}

# vidaSegundos=3600: se queda "abierto" una hora al lanzarlo normalmente, simulando un POS
# real que el cajero dejo funcionando. exitCodeSmokeTest/ignoraSmokeTest no aplican a esta
# prueba (solo se usa IProcesoPos aqui, no ActualizadorPosZip.ValidarAsync).
Set-Content -Path (Join-Path $RutaPos "simulado.config") -Value "0;false;3600" -NoNewline -Encoding UTF8
Set-Content -Path (Join-Path $RutaPos "version.txt") -Value "9.9.9-lab" -NoNewline -Encoding UTF8

Write-Host ""
Write-Host "POS de laboratorio listo en: $RutaPos"
Write-Host "Ejecutable: Zabyca.Pos.Desktop.exe (simulado; permanece abierto ~1 hora una vez lanzado)"
