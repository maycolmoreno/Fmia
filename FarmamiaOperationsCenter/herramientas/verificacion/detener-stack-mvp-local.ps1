$ErrorActionPreference = "Stop"

$raiz = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$compose = Join-Path $raiz "infraestructura\local\docker-compose.mvp.yml"
$runtime = Join-Path $raiz ".runtime"
$pids = Join-Path $runtime "pids.json"

function Detener-ArbolProceso {
    param(
        [int]$ProcessId
    )

    $hijos = Get-CimInstance Win32_Process -Filter "ParentProcessId = $ProcessId" -ErrorAction SilentlyContinue
    foreach ($hijo in $hijos) {
        Detener-ArbolProceso -ProcessId $hijo.ProcessId
    }

    $proceso = Get-Process -Id $ProcessId -ErrorAction SilentlyContinue
    if ($proceso) {
        Write-Host "Deteniendo proceso $ProcessId..."
        Stop-Process -Id $ProcessId -Force
    }
}

if (Test-Path $pids) {
    $estado = Get-Content $pids -Raw | ConvertFrom-Json
    foreach ($pidProceso in @($estado.backendPid, $estado.panelPid)) {
        if ($pidProceso) {
            Detener-ArbolProceso -ProcessId ([int]$pidProceso)
        }
    }
    Remove-Item -LiteralPath $pids -Force
}

if ($estado -and $estado.postgresLocal) {
    Write-Host "PostgreSQL local queda encendido; no se detiene desde este script."
} else {
    Write-Host "Deteniendo PostgreSQL MVP..."
    docker compose -f $compose down
}
Write-Host "Stack MVP local detenido."
