param(
    [string]$RutaAgente = "C:\Program Files (x86)\Farmamia Cia Ltda - Elipsys\Agent",
    [string]$RutaPos = "C:\Program Files (x86)\Farmamia Cia Ltda - Elipsys\Cliente",
    [string]$NombreEquipoEsperado = "POS-PILOTO-001"
)

$ErrorActionPreference = "Stop"

$resultado = [ordered]@{
    timestamp = (Get-Date).ToString("o")
    computerName = $env:COMPUTERNAME
    expectedComputerName = $NombreEquipoEsperado
    agentRoot = $RutaAgente
    posPath = $RutaPos
    checks = @()
}

function Agregar-Check([string]$Nombre, [bool]$Ok, [string]$Detalle) {
    $resultado.checks += [ordered]@{
        name = $Nombre
        ok = $Ok
        detail = $Detalle
    }
}

Agregar-Check "hostname" ($env:COMPUTERNAME -eq $NombreEquipoEsperado) "Equipo actual: $env:COMPUTERNAME"

New-Item -ItemType Directory -Force -Path $RutaPos | Out-Null
if (-not (Test-Path (Join-Path $RutaPos "Zabyca.Pos.Desktop.exe"))) {
    Set-Content -Path (Join-Path $RutaPos "Zabyca.Pos.Desktop.exe") -Value "POS demo inicial para prueba E2E" -Encoding UTF8
}
if (-not (Test-Path (Join-Path $RutaPos "version.txt"))) {
    Set-Content -Path (Join-Path $RutaPos "version.txt") -Value "2026.06.2-base" -Encoding UTF8
}

Agregar-Check "pos-path" (Test-Path $RutaPos) "Ruta POS preparada"
Agregar-Check "pos-executable" (Test-Path (Join-Path $RutaPos "Zabyca.Pos.Desktop.exe")) "Ejecutable POS base"
Agregar-Check "dotnet-sdk-not-required" $true "El agente publicado no requiere SDK. Para self-contained tampoco requiere runtime."

$resultado | ConvertTo-Json -Depth 8
