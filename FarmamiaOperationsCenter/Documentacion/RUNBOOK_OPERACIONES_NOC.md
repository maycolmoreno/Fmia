# Runbook Operaciones NOC

Guia operativa para administrar el piloto Farmamia Operations Center durante despliegues POS.

## Alcance

Este runbook cubre incidentes de operacion diaria:

- API o base de datos no disponible.
- Campana pausada automaticamente por fallos.
- Oleada detenida o sin avance.
- Agentes offline o sin latido.
- Cola durable del agente con eventos `DEAD_LETTER`.
- Paquetes con firma invalida.
- Crecimiento de eventos, metricas o auditoria.
- Validacion rapida de observabilidad Prometheus/Grafana.

## Consolas

- Angular NOC: `http://127.0.0.1:4200/operaciones`
- API health: `http://localhost:8081/actuator/health`
- Prometheus scrape: `http://localhost:8081/actuator/prometheus`
- API docs: `http://localhost:8081/swagger-ui.html`

## Comandos Basicos

Desde `FarmamiaOperationsCenter/backend-api`:

```powershell
.\mvnw.cmd test
```

Desde `FarmamiaOperationsCenter/admin-panel`:

```powershell
npm run build
```

Desde la raiz del repositorio:

```powershell
dotnet build FarmamiaOperationsCenter\windows-agent\Farmamia.Agent.Tests\Farmamia.Agent.Tests.csproj
```

## Indicadores Clave

Revisar en Prometheus:

- `farmamia_devices_online`
- `farmamia_devices_offline`
- `farmamia_deployments_active`
- `farmamia_alerts_critical_open`
- `farmamia_orchestration_auto_pauses_total`
- `farmamia_agent_heartbeats_total`
- `farmamia_agent_events_total`
- `farmamia_agent_update_results_total`
- `farmamia_package_downloads_total`
- `farmamia_retention_deleted_total`
- `farmamia_orchestration_scheduler_errors_total`
- `hikaricp_connections_pending`
- `http_server_requests_seconds_count`

## Incidente: API No Disponible

Sintomas:

- Angular muestra `Unexpected server error` o no carga datos.
- `GET /actuator/health` no responde.
- XHR a `localhost:8081` devuelve 500 o falla conexion.

Diagnostico:

```powershell
Invoke-WebRequest -UseBasicParsing http://localhost:8081/actuator/health
Get-NetTCPConnection -LocalPort 8081 -ErrorAction SilentlyContinue
```

Contencion:

- No iniciar nuevas oleadas.
- Pausar campanas activas desde Angular si la API responde parcialmente.
- Revisar logs de API antes de reiniciar.

Recuperacion:

- Reiniciar API.
- Confirmar `UP` en `/actuator/health`.
- Confirmar metricas en `/actuator/prometheus`.
- Revisar dashboard Angular despues del reinicio.

## Incidente: Campana Pausada Automaticamente

Sintomas:

- Vista `Operaciones NOC` muestra control `PAUSED`.
- Aparece razon de pausa en runtime de campana.
- Aumenta `farmamia_orchestration_auto_pauses_total`.

Diagnostico:

- Abrir `Operaciones NOC`.
- Revisar oleada con mayor porcentaje de fallo.
- Revisar alertas criticas asociadas.
- Revisar eventos recientes de agentes: `UPDATE_FAILED`, `VALIDATION_FAILED`, `ROLLBACK_FAILED`.

Contencion:

- Mantener la campana pausada.
- No reanudar oleadas masivas si existen fallos repetidos por firma, checksum o rollback.
- Aislar sucursales o equipos con fallos recurrentes.

Recuperacion:

- Corregir causa raiz.
- Evaluar la orquestacion desde Angular.
- Reanudar solo la oleada afectada.
- Confirmar que `failedTargets` deja de crecer.

## Incidente: Oleada Sin Avance

Sintomas:

- Oleada en `RUNNING`, pero `completedTargets` no sube.
- `pendingTargets` permanece estable.
- Agentes sin latido o enlaces degradados.

Diagnostico:

- Revisar `Agentes en riesgo` en `Operaciones NOC`.
- Revisar `farmamia_agent_heartbeats_total`.
- Filtrar equipos por `OFFLINE`, `STALE` o `ERROR`.
- Ver detalle de equipo para ultimo latido, version POS y eventos recientes.

Contencion:

- Pausar oleada si hay impacto amplio.
- Mantener campanas pequenas si solo fallan equipos aislados.

Recuperacion:

- Validar conectividad de agente a API.
- Confirmar token tecnico local.
- Reiniciar servicio del agente si el equipo esta accesible.
- Evaluar orquestacion al recuperar latidos.

## Incidente: Cola Durable con Dead Letter

Sintomas:

- Log local del agente muestra `DEAD_LETTER`.
- Eventos no aparecen en API aunque el agente ejecuto acciones.

Diagnostico:

- Revisar logs en carpeta `Logs` del agente.
- Confirmar diagnostico de cola:
  - pendientes;
  - fallidos;
  - dead letter;
  - evento pendiente mas antiguo.

Contencion:

- No borrar la base SQLite local del agente.
- No reinstalar agente sin preservar carpeta `State`.

Recuperacion:

- Corregir conectividad o payload incompatible.
- Reiniciar el servicio del agente para reactivar el despachador.
- Si persiste `DEAD_LETTER`, extraer logs y SQLite para analisis.

## Incidente: Firma Digital Invalida

Sintomas:

- Evento `SIGNATURE_VALIDATION_FAILED`.
- Resultado de actualizacion `FAILED`.
- POS no se modifica.

Diagnostico:

- Confirmar que el paquete tiene firma, algoritmo y clave publica.
- Confirmar que el checksum SHA-256 del paquete coincide.
- Revisar version y `signingKeyId`.

Contencion:

- Retirar paquete afectado.
- No reintentar la oleada con el mismo paquete.

Recuperacion:

- Volver a cargar paquete firmado correctamente.
- Aprobar nuevo paquete.
- Crear o replanificar campana.

## Incidente: Token Local o Credenciales

Sintomas:

- Agente no puede enviar latidos.
- API responde 401/403.
- `credenciales.json` existe pero no puede leerse.

Diagnostico:

- Confirmar que el servicio corre con el mismo usuario Windows que genero el token protegido.
- Si el archivo tiene `proteccion: DPAPI_CURRENT_USER`, debe leerse bajo el mismo contexto de usuario.
- Si es archivo legacy sin `proteccion`, el agente lo sigue aceptando y lo migrara al guardar credenciales nuevamente.

Contencion:

- No copiar `credenciales.json` protegido entre usuarios o maquinas.
- No editar manualmente el token protegido.

Recuperacion:

- Registrar nuevamente el agente si el usuario de servicio cambio.
- Validar latido en Angular y Prometheus.

## Incidente: Crecimiento de Base de Datos

Politica por defecto:

- Eventos: 90 dias.
- Metricas: 30 dias.
- Auditoria: 365 dias.

Configuracion:

- `FARMAMIA_RETENTION_ENABLED`
- `FARMAMIA_RETENTION_CRON`
- `FARMAMIA_RETENTION_EVENTS_DAYS`
- `FARMAMIA_RETENTION_METRICS_DAYS`
- `FARMAMIA_RETENTION_AUDIT_DAYS`

Diagnostico:

- Revisar `farmamia_retention_deleted_total`.
- Revisar tamanos de tablas `update_events`, `device_metrics`, `audit_logs`.

Contencion:

- No bajar retencion sin aprobacion operativa.
- Exportar auditoria si se requiere conservar evidencia antes de purga.

## Validacion Pre-Campana

Antes de iniciar una campana:

- API health en `UP`.
- Angular NOC carga sin errores 500.
- Paquete aprobado y firmado.
- Sucursales de turno revisadas.
- Ventana de mantenimiento configurada.
- `maxFailurePercent` y `maxParallelDevices` definidos.
- Alertas criticas actuales revisadas.
- Agentes objetivo con latido reciente.

## Validacion Post-Campana

Al cerrar una campana:

- `completedTargets` coincide con alcance esperado.
- `failedTargets` explicado y documentado.
- No hay oleadas en `RUNNING` o `PAUSED` sin responsable.
- Eventos `ROLLBACK_FAILED` revisados.
- Alertas criticas reconocidas o cerradas.
- Dashboard Grafana sin degradacion sostenida.

## Criterios de Escalamiento

Escalar a soporte tecnico si:

- Hay `ROLLBACK_FAILED`.
- La API no recupera health despues de reinicio.
- Aumenta `hikaricp_connections_pending`.
- Existen `DEAD_LETTER` repetidos en varios agentes.
- Firma invalida aparece en mas de un paquete.
- Mas del 5% de equipos queda `OFFLINE` durante una campana.

