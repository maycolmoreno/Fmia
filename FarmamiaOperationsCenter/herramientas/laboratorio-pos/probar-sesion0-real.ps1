# Reproduce en una maquina real el unico riesgo de la Fase 2 que ningun test unitario puede
# confirmar: si el agente, corriendo con el mismo aislamiento de Sesion 0 que el Windows Service
# real (cuenta SYSTEM, sesion no interactiva), puede (a) mostrar un aviso visible al usuario via
# WTSSendMessage y (b) cerrar/reabrir el proceso POS en el escritorio interactivo del usuario.
#
# No depende del backend ni de una campana real: lanza el comando de diagnostico
# "FarmamiaUpdater.exe probar-sesion" dentro de una tarea programada como SYSTEM, que es la forma
# estandar de ejecutar codigo con el mismo aislamiento de sesion que un Windows Service, sin tener
# que tocar el servicio real en produccion.
#
# Requisitos previos:
#   1. Publicar el agente (herramientas\publicar-agente.ps1 o instalar-agente-laptop.ps1)
#      para tener FarmamiaUpdater.exe en $RutaAgente.
#   2. Ejecutar herramientas\laboratorio-pos\preparar-pos-demo.ps1 para tener un POS de
#      laboratorio real y ejecutable en $RutaPos.
param(
    [string]$RutaAgente = "C:\Program Files (x86)\Farmamia Cia Ltda - Elipsys\Agent",
    [string]$RutaPos = "C:\FarmamiaLab\PosDemo"
)

$ErrorActionPreference = "Stop"

function Confirmar-Administrador {
    $identidad = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = [Security.Principal.WindowsPrincipal]::new($identidad)
    if (-not $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
        throw "Ejecute este script como administrador (se necesita para registrar una tarea programada como SYSTEM)."
    }
}

function Esperar-TareaTerminada {
    param([string]$NombreTarea, [int]$TimeoutSegundos = 60)

    $limite = (Get-Date).AddSeconds($TimeoutSegundos)
    do {
        $info = Get-ScheduledTaskInfo -TaskName $NombreTarea
        if ($info.LastTaskResult -ne 267009) {
            # 267009 = SCHED_S_TASK_RUNNING
            return
        }
        Start-Sleep -Seconds 1
    } while ((Get-Date) -lt $limite)

    Write-Warning "La tarea seguia en ejecucion despues de $TimeoutSegundos s; se continua de todas formas."
}

Confirmar-Administrador

$updater = Join-Path $RutaAgente "FarmamiaUpdater.exe"
if (-not (Test-Path $updater)) {
    throw "No se encontro $updater. Publique el agente primero (herramientas\publicar-agente.ps1 o instalar-agente-laptop.ps1)."
}

$posDemo = Join-Path $RutaPos "Zabyca.Pos.Desktop.exe"
if (-not (Test-Path $posDemo)) {
    throw "No se encontro $posDemo. Ejecute primero herramientas\laboratorio-pos\preparar-pos-demo.ps1 -RutaPos `"$RutaPos`""
}

Write-Host "=== Paso 1: lanzando el POS de laboratorio en su sesion interactiva actual ==="
Start-Process -FilePath $posDemo -WorkingDirectory $RutaPos
Start-Sleep -Seconds 2
$procesoAntes = Get-Process -Name "Zabyca.Pos.Desktop" -ErrorAction SilentlyContinue
if (-not $procesoAntes) {
    throw "El POS de laboratorio no aparece en ejecucion. Revise preparar-pos-demo.ps1."
}
Write-Host "POS de laboratorio corriendo (PID $($procesoAntes.Id))."
Write-Host ""
Read-Host "Confirme que ve su ventana/consola en el escritorio y presione Enter para continuar"

$logDiagnostico = Join-Path $env:TEMP "farmamia-probar-sesion0.log"
Remove-Item -Path $logDiagnostico -ErrorAction SilentlyContinue

$nombreTarea = "FarmamiaProbarSesion0"
Write-Host ""
Write-Host "=== Paso 2: ejecutando el diagnostico como SYSTEM (misma Sesion 0 que el servicio real) ==="
Unregister-ScheduledTask -TaskName $nombreTarea -Confirm:$false -ErrorAction SilentlyContinue

$accion = New-ScheduledTaskAction `
    -Execute "cmd.exe" `
    -Argument "/c `"`"$updater`" --agent-root=`"$RutaAgente`" probar-sesion > `"$logDiagnostico`" 2>&1`""
$principalTarea = New-ScheduledTaskPrincipal -UserId "SYSTEM" -LogonType ServiceAccount -RunLevel Highest
Register-ScheduledTask -TaskName $nombreTarea -Action $accion -Principal $principalTarea -Force | Out-Null

Start-ScheduledTask -TaskName $nombreTarea
Start-Sleep -Seconds 2
Esperar-TareaTerminada -NombreTarea $nombreTarea -TimeoutSegundos 60
Unregister-ScheduledTask -TaskName $nombreTarea -Confirm:$false -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "=== Log del diagnostico (ejecutado como SYSTEM) ==="
if (Test-Path $logDiagnostico) {
    Get-Content $logDiagnostico
} else {
    Write-Warning "No se genero $logDiagnostico. La tarea programada pudo no haberse ejecutado."
}

Write-Host ""
Write-Host "=== Resultado automatico (cierre/reapertura de proceso) ==="
$procesoDespues = Get-Process -Name "Zabyca.Pos.Desktop" -ErrorAction SilentlyContinue
if ($procesoDespues -and $procesoDespues.Id -ne $procesoAntes.Id) {
    Write-Host "OK: PID cambio ($($procesoAntes.Id) -> $($procesoDespues.Id)) => IProcesoPos.Cerrar+Iniciar funciono desde Sesion 0." -ForegroundColor Green
} elseif ($procesoDespues -and $procesoDespues.Id -eq $procesoAntes.Id) {
    Write-Warning "El PID no cambio ($($procesoDespues.Id)) => el cierre/reapertura probablemente NO funciono desde Sesion 0."
} else {
    Write-Warning "No se encontro el proceso POS de laboratorio despues de la prueba. Revise el log de arriba."
}

Write-Host ""
Write-Host "=== Verificacion manual pendiente ===" -ForegroundColor Yellow
Write-Host "¿Aparecio en su escritorio un mensaje de Windows con el texto 'PRUEBA DE DIAGNOSTICO...'?"
Write-Host "  Si -> WTSSendMessage funciona desde Sesion 0, el aviso al operador es confiable."
Write-Host "  No -> hay que investigar una alternativa (tarea programada en la sesion del usuario, por ejemplo)."
