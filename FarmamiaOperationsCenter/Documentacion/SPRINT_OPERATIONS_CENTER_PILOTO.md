# Sprint Operations Center Piloto

## Objetivo

Convertir el MVP de actualizacion remota POS en una plataforma piloto operativamente confiable para 645 farmacias y aproximadamente 1900 equipos Windows, priorizando resiliencia, control operacional, seguridad de paquetes, paginacion y observabilidad.

## Diagnostico Actual

El sistema actual ya resuelve el ciclo base de actualizacion:

- Backend Spring Boot con separacion por dominio, aplicacion, infraestructura y presentacion.
- PostgreSQL con Flyway.
- Registro de agentes, heartbeats, tokens tecnicos, paquetes POS, despliegues, instrucciones, eventos, alertas y auditoria.
- Panel Angular funcional para operacion administrativa.
- Agente Windows con descarga, checksum, backup, rollback y reporte de resultado.

Brecha principal: el sistema aun opera como actualizador centralizado, no como plataforma NOC 24/7. Faltan orquestacion explicita de campanas, durabilidad offline en agente, firma digital, paginacion global, metricas Prometheus/Grafana y controles operativos para oleadas.

## OPS-001 - Orquestador de Despliegues

### Decision Arquitectonica

Crear un modulo dedicado `orquestacion` encima del modelo actual de `deployments` y `deployment_targets`. No conviene seguir cargando toda la logica en el CRUD de despliegues. El orquestador debe decidir cuando un equipo puede recibir instruccion y cuando una campana debe avanzar, pausarse o fallar.

### Entidades Nuevas

- `operational_groups`
  - `id`
  - `code`: `trx001`, `trx002`, `trx003`
  - `name`
  - `description`
  - `max_parallel_devices`
  - `bandwidth_limit_mbps`
  - `active`

- `branch_operational_profile`
  - `id`
  - `branch_id`
  - `group_id`
  - `is_on_duty`
  - `timezone`
  - `default_window_start`
  - `default_window_end`
  - `blackout_start`
  - `blackout_end`

- `deployment_waves`
  - `id`
  - `deployment_id`
  - `wave_number`
  - `name`
  - `group_id`
  - `planned_start_at`
  - `planned_end_at`
  - `max_parallel_devices`
  - `max_failure_percent`
  - `status`: `DRAFT`, `READY`, `RUNNING`, `PAUSED`, `COMPLETED`, `FAILED`, `CANCELLED`
  - `created_at`
  - `updated_at`

- `deployment_wave_targets`
  - `id`
  - `wave_id`
  - `deployment_target_id`
  - `priority`
  - `status`

- `deployment_control_state`
  - `deployment_id`
  - `auto_pause_enabled`
  - `paused_reason`
  - `last_evaluated_at`
  - `failure_percent`
  - `active_parallel_count`
  - `next_wave_number`

### Cambios Sobre Tablas Existentes

- `devices`
  - agregar `operational_group_id`
  - agregar `maintenance_window_start`
  - agregar `maintenance_window_end`
  - agregar `is_on_duty_branch_exempt`

- `deployment_targets`
  - agregar `wave_id`
  - agregar `next_retry_at`
  - agregar `retry_policy`
  - agregar `last_instruction_issued_at`
  - agregar `instruction_lease_until`
  - agregar `idempotency_key`

### Casos de Uso

- `CrearCampanaProgresivaCasoUso`
- `PlanificarOleadasDespliegueCasoUso`
- `EvaluarAvanceCampanaCasoUso`
- `AutorizarSiguienteLoteCasoUso`
- `PausarAutomaticamenteCampanaCasoUso`
- `ReintentarObjetivosFallidosCasoUso`
- `AplicarVentanasHorariasCasoUso`
- `ExcluirFarmaciasDeTurnoCasoUso`

### Endpoints Propuestos

- `POST /api/orchestration/deployments/{deploymentId}/plan`
- `GET /api/orchestration/deployments/{deploymentId}/plan`
- `POST /api/orchestration/deployments/{deploymentId}/evaluate`
- `POST /api/orchestration/deployments/{deploymentId}/waves/{waveId}/start`
- `POST /api/orchestration/deployments/{deploymentId}/waves/{waveId}/pause`
- `POST /api/orchestration/deployments/{deploymentId}/waves/{waveId}/resume`
- `GET /api/orchestration/deployments/{deploymentId}/runtime-status`
- `GET /api/operational-groups`
- `PUT /api/branches/{branchId}/operational-profile`

### Reglas de Negocio

- Una campana masiva nunca debe autorizar los 1900 equipos al mismo tiempo.
- Una oleada no inicia si contiene farmacias de turno salvo excepcion explicita.
- Un objetivo solo recibe instruccion si:
  - su despliegue esta `RUNNING` o `APPROVED`;
  - su oleada esta `RUNNING`;
  - esta dentro de ventana horaria;
  - no excede el maximo de paralelismo;
  - no esta bloqueado por reintento;
  - el porcentaje de fallos no supero el umbral.
- Si `failure_percent > max_failure_percent`, la campana entra en `PAUSED` automaticamente.
- Cada instruccion al agente debe tener lease/idempotencia para evitar doble ejecucion.
- El orquestador debe ser idempotente: re-evaluar no debe duplicar autorizaciones.

### Flujo

```text
Crear despliegue
  -> Planificar oleadas por grupo/sucursal/riesgo
  -> Aprobar plan
  -> Iniciar oleada piloto
  -> Agentes consumen instrucciones con lease
  -> Eventos actualizan objetivos
  -> Orquestador evalua umbral de fallos
  -> Completa piloto o pausa
  -> Avanza oleada trx001/trx002/trx003
  -> Completa campana o queda pausada para intervencion NOC
```

### Estado Implementado - Fase 1

Implementado en backend:

- Migracion `V8__orquestacion_despliegues.sql`.
- Tabla `deployment_waves`.
- Tabla `deployment_control_state`.
- Relacion `deployment_targets.wave_id`.
- Migracion `V9__leases_instrucciones_paralelismo.sql`.
- Campos `deployment_targets.last_instruction_issued_at` e `instruction_lease_until`.
- Campo `deployment_waves.max_parallel_devices`.
- Migracion `V10__reintentos_objetivos_despliegue.sql`.
- Campo `deployment_targets.next_retry_at`.
- Caso de uso `OrquestarDesplieguesCasoUso`.
- Puerto `RepositorioOrquestacionDespliegues`.
- Adaptador JPA `RepositorioOrquestacionDesplieguesJpaAdaptador`.
- Controlador `ControladorOrquestacionDespliegues`.

Endpoints disponibles:

- `POST /api/orchestration/deployments/{deploymentId}/plan`
- `GET /api/orchestration/deployments/{deploymentId}/plan`
- `POST /api/orchestration/deployments/{deploymentId}/evaluate`
- `POST /api/orchestration/deployments/{deploymentId}/waves/{waveId}/start`
- `POST /api/orchestration/deployments/{deploymentId}/waves/{waveId}/pause`
- `POST /api/orchestration/deployments/{deploymentId}/waves/{waveId}/resume`
- `GET /api/orchestration/deployments/{deploymentId}/runtime-status`

Reglas implementadas:

- Planificacion deterministica desde `deployment_targets`.
- Oleada piloto primero si existen objetivos piloto.
- Oleadas posteriores por `target_group` (`trx001`, `trx002`, `trx003` o `GENERAL`).
- Conteo por oleada: planificados, completados, fallidos, pendientes y farmacias de turno.
- Umbral de fallos por campana/oleada.
- Pausa automatica del control si una oleada `RUNNING` supera `maxFailurePercent`.
- Compuerta real de instrucciones al agente:
  - Los objetivos nuevos nacen `PENDING`.
  - Al iniciar una oleada, sus objetivos pasan a `AUTHORIZED`.
  - Si un objetivo antiguo no tiene oleada y ya estaba `AUTHORIZED`, conserva el comportamiento MVP.
  - Si un objetivo tiene oleada, solo recibe instruccion cuando la oleada esta `RUNNING`.
  - Si la oleada tiene ventana horaria, la instruccion solo se entrega dentro de esa ventana.
  - Al emitir instruccion se registra un lease de 15 minutos.
  - Mientras el lease este activo, el mismo objetivo no recibe otra instruccion.
  - La oleada no emite mas instrucciones si ya alcanzo `maxParallelDevices` leases activos.
- Reintentos:
  - Un resultado `FAILED` o `ROLLBACK_FAILED` incrementa `attempt_count`.
  - El backend programa `next_retry_at` con backoff exponencial: 5, 10, 20, 40 y maximo 60 minutos.
  - `evaluate` reautoriza objetivos fallidos cuando `next_retry_at` ya vencio y no superan `retryLimit`.
  - Los fallos con reintento pendiente cuentan como pendientes operativos, no como cierre definitivo de oleada.
- Evaluacion automatica:
  - `EvaluadorOrquestacionProgramado` ejecuta `evaluate` sobre campanas con control `RUNNING`.
  - Intervalo default: 60 segundos.
  - Variables:
    - `FARMAMIA_ORCHESTRATION_SCHEDULER_ENABLED=true|false`
    - `FARMAMIA_ORCHESTRATION_SCHEDULER_DELAY_MS=60000`
- Bloqueo de inicio de oleada si contiene farmacias marcadas como turno.
- Auditoria administrativa para planificar, evaluar, iniciar, pausar y reanudar oleadas.

Estado implementado - Fase 2:

- Lease de instruccion configurable por ambiente con `FARMAMIA_ORCHESTRATION_INSTRUCTION_LEASE_MINUTES` y default de 15 minutos.
- Metricas Prometheus del scheduler:
  - `farmamia.orchestration.scheduler.cycles.total`
  - `farmamia.orchestration.scheduler.deployments.evaluated.total`
  - `farmamia.orchestration.scheduler.errors.total`
  - `farmamia.orchestration.scheduler.errors.by_deployment.total` con etiqueta `deployment_id`
- Metrica Prometheus de reintentos reautorizados:
  - `farmamia.orchestration.retries.reauthorized.total`
- Metricas Prometheus de entrega de instrucciones:
  - `farmamia.orchestration.instructions.issued.total`
  - `farmamia.orchestration.instructions.blocked.total` con etiqueta `reason`
  - `farmamia.orchestration.instruction.leases.expired`

Limitacion consciente vigente:

- No quedan brechas tecnicas conocidas para OPS-001 Fase 2 dentro del alcance piloto; resta validacion funcional con datos reales y tablero Grafana.

Verificacion:

- `.\mvnw.cmd test`
- 30 pruebas, 0 fallos, 0 errores, 6 omitidas por Docker/Testcontainers no disponible.
- `npm run build` en `admin-panel`
- Build Angular exitoso; artefacto generado en `dist/farmamia-admin-panel`.

## OPS-002 - Cola Durable del Agente

### Alternativas

| Alternativa | Ventaja | Riesgo | Decision |
| --- | --- | --- | --- |
| JSON por archivo | Simple | Corrupcion, concurrencia, reintentos dificiles | No recomendado |
| LiteDB | Embebido .NET, facil | Dependencia adicional, menor estandar operacional | Aceptable |
| SQLite | Durable, transaccional, probado | Requiere paquete nativo/gestion de archivo | Recomendado |

Decision: usar SQLite local en el agente. Para un endpoint agentico Windows, SQLite da transacciones, recuperacion, indices, bloqueo controlado y facilidad de inspeccion.

### Estructura Local

Archivo:

```text
C:\Program Files (x86)\Farmamia Cia Ltda - Elipsys\Agent\State\agent-queue.db
```

Tabla `outbox_events`:

- `id`
- `event_type`
- `payload_json`
- `idempotency_key`
- `status`: `PENDING`, `SENDING`, `SENT`, `FAILED`, `DEAD_LETTER`
- `attempt_count`
- `next_attempt_at`
- `created_at`
- `updated_at`
- `last_error`
- `checksum`

Tabla `agent_state`:

- `key`
- `value_json`
- `updated_at`

### Clases Propuestas

- `IColaEventosAgente`
- `ColaEventosAgenteSqlite`
- `DespachadorColaEventosCasoUso`
- `RecuperarColaAgenteCasoUso`
- `EventoPendienteAgente`
- `ResultadoDespachoEvento`

### Estrategia

- Todo evento operacional se escribe primero en SQLite.
- El envio al backend ocurre despues.
- Si el backend no responde, el evento queda `PENDING`.
- Backoff exponencial con jitter.
- `SENDING` viejo se recupera a `PENDING` al iniciar el servicio.
- `checksum` del payload detecta corrupcion.
- Despues de N intentos, mover a `DEAD_LETTER` y reportar alerta cuando vuelva la conectividad.

### Cambios En Backend

- Aceptar `idempotencyKey` en eventos y resultados.
- Crear indice unico parcial por `device_id + idempotency_key`.
- Responder 200/202 si el evento ya fue procesado.

### Estado Implementado

- Agente Windows usa SQLite local como outbox durable.
- Eventos y resultados se escriben primero en `outbox_events`.
- El despachador reenvia pendientes con credenciales locales.
- `SENDING` se recupera a `PENDING` al iniciar.
- Backoff exponencial con jitter.
- Checksum SHA-256 del payload local.
- Estados soportados: `PENDING`, `SENDING`, `SENT`, `FAILED`, `DEAD_LETTER`.
- Diagnostico de cola por estado disponible desde `IColaEventosAgente`.
- El despachador registra salud periodica de la cola.
- Si existen eventos `DEAD_LETTER`, el agente registra alerta operativa en log.
- Prueba agregada con backend HTTP falso para verificar encolado y despacho end-to-end.

Verificacion:

- `dotnet build FarmamiaOperationsCenter\windows-agent\Farmamia.Agent.Tests\Farmamia.Agent.Tests.csproj`
- Compilacion correcta, 0 advertencias, 0 errores.

Limitacion de ambiente:

- `dotnet test` no pudo ejecutarse en este host porque falta runtime `Microsoft.NETCore.App 8.0.0`; el host solo reporta runtime 10.0.8.

## OPS-003 - Firma Digital de Paquetes

### Decision Arquitectonica

SHA-256 solo valida integridad si el hash viene de una fuente confiable. No prueba origen. Para produccion, cada ZIP POS debe estar firmado.

Decision: firma digital detached usando certificado interno o par de claves Ed25519/RSA. Para integracion empresarial Windows, RSA/ECDSA con certificado corporativo es mas natural. Para implementacion simple multiplataforma, Ed25519 es mas sencillo. Recomendacion piloto: RSA/ECDSA con certificado publico distribuido al agente.

### Modelo

Tabla `package_signing_keys`:

- `id`
- `key_id`
- `algorithm`
- `public_key_pem`
- `status`: `ACTIVE`, `ROTATING`, `REVOKED`, `EXPIRED`
- `valid_from`
- `valid_until`
- `created_at`

Cambios `pos_packages`:

- `signature`
- `signature_algorithm`
- `signing_key_id`
- `signed_at`
- `signature_status`: `PENDING`, `VALID`, `INVALID`, `REVOKED_KEY`

### Flujo

```text
Operador sube ZIP
  -> Backend calcula SHA-256
  -> Backend firma hash o paquete
  -> Guarda firma y key_id
  -> Agente descarga ZIP + metadata
  -> Agente valida SHA-256
  -> Agente valida firma con clave publica confiable
  -> Solo entonces aplica backup/update
```

### Rotacion

- Mantener al menos dos claves publicas confiables en el agente.
- Backend firma nuevos paquetes con clave activa.
- Clave vieja pasa a `ROTATING`.
- Revocacion bloquea nuevos despliegues y agentes rechazan paquetes asociados si la politica lo exige.

### Estado Implementado - Piloto

- Migracion `V11__firma_digital_paquetes.sql`.
- `pos_packages` almacena:
  - `signature`;
  - `signature_algorithm`;
  - `signing_key_id`;
  - `signing_public_key_pem`;
  - `signed_at`;
  - `signature_status`.
- Puerto `FirmadorPaquetesPos`.
- Implementacion `FirmadorPaquetesPosRsa` con `SHA256withRSA`.
- Al cargar un paquete POS, el backend firma el checksum SHA-256 y guarda la firma.
- Las respuestas de paquetes exponen estado y metadatos de firma.
- La instruccion al agente incluye:
  - `signature`;
  - `signatureAlgorithm`;
  - `signingKeyId`;
  - `signingPublicKeyPem`.
- El agente Windows valida la firma RSA sobre el checksum antes de aplicar el ZIP.
- Si la firma existe y es invalida:
  - reporta evento `SIGNATURE_VALIDATION_FAILED`;
  - reporta resultado `FAILED`;
  - guarda estado local de fallo;
  - no aplica el paquete.
- Si la instruccion no trae firma, el agente mantiene compatibilidad con paquetes historicos y continua solo con SHA-256.

Limitacion consciente vigente:

- Si no se configura clave de firma real por ambiente, el backend usa una clave RSA temporal de desarrollo generada al arrancar. Es suficiente para piloto local, pero produccion debe inyectar una clave privada estable y definir rotacion/revocacion.

Verificacion:

- `.\mvnw.cmd test`
- 30 pruebas, 0 fallos, 0 errores, 6 omitidas por Docker/Testcontainers no disponible.
- `dotnet build FarmamiaOperationsCenter\windows-agent\Farmamia.Agent.Tests\Farmamia.Agent.Tests.csproj`
- Compilacion correcta, 0 advertencias, 0 errores.

## OPS-004 - Paginacion y Filtros Globales

### Endpoints A Corregir

Riesgo actual detectado:

- Equipos: usa listados completos.
- Sucursales: usa listados completos.
- Paquetes: usa listados completos.
- Despliegues: usa listados completos y conteos por despliegue.
- Usuarios: usa listados completos.
- Eventos, alertas y auditoria ya tienen limites o filtros parciales, pero deben estandarizarse.

### Contrato Comun

Request:

```text
page=0
size=50
sort=createdAt,desc
q=texto
status=ONLINE
branchCode=FMA001
from=2026-01-01T00:00:00
to=2026-01-31T23:59:59
```

Response:

```json
{
  "content": [],
  "page": 0,
  "size": 50,
  "totalElements": 0,
  "totalPages": 0,
  "hasNext": false
}
```

### Filtros Minimos

- Equipos: `q`, `status`, `branchCode`, `group`, `posVersion`, `agentVersion`, `lastHeartbeatFrom`, `lastHeartbeatTo`.
- Sucursales: `q`, `city`, `zone`, `group`, `onDuty`.
- Paquetes: `q`, `status`, `version`, `uploadedFrom`, `uploadedTo`.
- Despliegues: `q`, `status`, `packageVersion`, `createdFrom`, `createdTo`, `group`.
- Eventos: `deviceId`, `deploymentId`, `eventType`, `severity`, `from`, `to`.
- Alertas: ya existe base; agregar `deviceId`, `deploymentId`, `acknowledgedBy`.
- Auditoria: ya existe base; agregar paginacion real.
- Usuarios: `q`, `role`, `active`.

### Estado Implementado

- Backend expone contrato `RespuestaPagina<T>` con `content`, `page`, `size`, `totalElements`, `totalPages` y `hasNext`.
- Endpoints paginados disponibles para:
  - `/api/devices/page`
  - `/api/branches/page`
  - `/api/packages/page`
  - `/api/deployments/page`
  - `/api/update-events/page`
  - `/api/alerts/page`
  - `/api/audit-logs/page`
  - `/api/admin/users/page`
- El panel Angular consume los endpoints paginados para equipos, sucursales, paquetes, despliegues, eventos, alertas, auditoria y usuarios.
- La vista de equipos incluye filtros visibles por busqueda, estado, sucursal, version POS, tamano de pagina y navegacion anterior/siguiente.
- Sucursales soporta filtros `q`, `code`, `city`, `zone`, `onDuty`, `active`, paginacion y orden por campos publicos.

Verificacion:

- `.\mvnw.cmd test`
- 30 pruebas, 0 fallos, 0 errores, 6 omitidas por Docker/Testcontainers no disponible.
- `npm run build` en `admin-panel`
- Build Angular exitoso; artefacto generado en `dist/farmamia-admin-panel`.

## OPS-005 - Observabilidad Inicial

### Arquitectura

```text
Spring Boot Actuator + Micrometer
  -> /actuator/prometheus
  -> Prometheus scrape cada 15s
  -> Grafana dashboards
  -> Alertmanager / correo / Teams interno
```

### Dependencias Backend

- `spring-boot-starter-actuator`
- `micrometer-registry-prometheus`

### Metricas Recomendadas

Tecnicas:

- `http_server_requests_seconds`
- `jvm_memory_used_bytes`
- `hikaricp_connections_active`
- `hikaricp_connections_pending`
- `process_cpu_usage`

Operativas:

- `farmamia_devices_online`
- `farmamia_agent_heartbeats_total`
- `farmamia_agent_heartbeats_per_minute`
- `farmamia_deployments_active`
- `farmamia_deployment_failures_total`
- `farmamia_deployment_rollbacks_total`
- `farmamia_alerts_critical_open`
- `farmamia_package_downloads_total`
- `farmamia_agent_instruction_issued_total`
- `farmamia_orchestration_auto_pauses_total`

### Dashboards Grafana

- NOC Overview:
  - equipos online/offline;
  - alertas criticas abiertas;
  - despliegues activos;
  - fallos por hora;
  - heartbeats por minuto.

- Deployment Command:
  - campanas activas;
  - avance por oleada;
  - fallo por grupo;
  - rollbacks;
  - equipos bloqueados por ventana.

- Agent Health:
  - agentes sin heartbeat;
  - latencia reportada;
  - perdida de paquetes;
  - versiones del agente.

- API/DB Health:
  - latencia p95/p99;
  - errores 4xx/5xx;
  - conexiones DB;
  - queries lentas.

### Alertas Minimas

- API 5xx > 2% por 5 minutos.
- Heartbeats caen mas de 30% vs baseline.
- Mas de 20 alertas criticas abiertas.
- Cualquier despliegue supera umbral de fallos.
- Rollback fallido.
- DB conexiones pendientes > 0 sostenido.

### Estado Implementado - Piloto

Base tecnica:

- `spring-boot-starter-actuator` disponible.
- `micrometer-registry-prometheus` disponible.
- `/actuator/prometheus` expuesto por configuracion.
- `/actuator/health`, `/actuator/info` y `/actuator/metrics` expuestos.

Metricas operativas implementadas:

- Gauges:
  - `farmamia.devices.total`
  - `farmamia.devices.online`
  - `farmamia.devices.offline`
  - `farmamia.deployments.active`
  - `farmamia.deployment.failures.total`
  - `farmamia.deployment.rollbacks.total`
  - `farmamia.alerts.critical.open`
  - `farmamia.orchestration.instruction.leases.expired`
- Counters:
  - `farmamia.agent.heartbeats.total`
  - `farmamia.agent.events.total` con etiqueta `event_type`
  - `farmamia.agent.update.results.total` con etiqueta `status`
  - `farmamia.package.downloads.total`
  - `farmamia.orchestration.auto.pauses.total`

Artefactos de monitoreo:

- `monitoring/prometheus.yml`
  - scrape de `/actuator/prometheus` cada 15 segundos.
- `monitoring/alert_rules.yml`
  - API 5xx > 2%;
  - mas de 20 alertas criticas abiertas;
  - autopausa de campana;
  - ausencia sostenida de heartbeats;
  - conexiones DB pendientes.
- `monitoring/grafana-noc-overview.json`
  - equipos online/offline;
  - alertas criticas abiertas;
  - despliegues activos;
  - heartbeats por minuto;
  - eventos por tipo;
  - resultados de actualizacion;
  - autopausas por hora.

Limitacion consciente vigente:

- Las reglas de alerta son baseline de piloto. En produccion deben calibrarse con trafico real, baseline horario y ventanas de mantenimiento.

Verificacion:

- `.\mvnw.cmd test`
- 30 pruebas, 0 fallos, 0 errores, 6 omitidas por Docker/Testcontainers no disponible.

## Cambios Por Capa

### Backend

- Crear paquete `orquestacion`.
- Agregar endpoints de plan, oleadas y estado runtime.
- Agregar idempotencia en eventos/resultados.
- Agregar paginacion global.
- Agregar metricas Micrometer.
- Separar consultas operativas de dashboard de consultas transaccionales.

### Base de Datos

- Agregar tablas de grupos, perfiles operativos, oleadas y estado de control.
- Agregar columnas de lease/reintento/idempotencia.
- Agregar firma de paquetes y claves.
- Agregar indices compuestos para estado/fecha/sucursal/grupo.
- Definir retencion para eventos, metricas y auditoria.

### Estado Implementado - Retencion Operativa

Implementado en backend:

- Job programado `RetencionOperativaProgramada`.
- Politica configurable por ambiente:
  - `FARMAMIA_RETENTION_ENABLED`;
  - `FARMAMIA_RETENTION_CRON`;
  - `FARMAMIA_RETENTION_EVENTS_DAYS`;
  - `FARMAMIA_RETENTION_METRICS_DAYS`;
  - `FARMAMIA_RETENTION_AUDIT_DAYS`.
- Valores por defecto del piloto:
  - eventos de actualizacion: 90 dias;
  - metricas de equipos: 30 dias;
  - auditoria administrativa: 365 dias.
- Contador Prometheus `farmamia.retention.deleted.total` con etiqueta `dataset`.
- Metodos de purga por fecha en repositorios JPA de eventos, metricas y auditoria.

Verificacion:

- `.\mvnw.cmd test` finaliza correctamente: 32 pruebas, 0 fallos, 6 saltadas por falta de Docker/Testcontainers.

### Agente Windows

- Agregar SQLite local.
- Convertir eventos directos a outbox durable.
- Agregar despachador en background.
- Validar firma digital antes de aplicar ZIP.
- Proteger token con DPAPI.
- Agregar lock de actualizacion.

### Estado Implementado - Hardening Agente

Implementado en agente Windows:

- `credenciales.json` deja de persistir el token tecnico en texto claro.
- En Windows, el token se protege con DPAPI asociado al usuario que ejecuta el agente.
- Lectura retrocompatible de credenciales legacy sin campo `proteccion`.
- Lock global `Global\FarmamiaOpsAgent.UpdateLock` para evitar dos actualizaciones POS simultaneas.
- Si el lock esta ocupado, el agente reporta `UPDATE_FAILED`, guarda estado local `FAILED` y no descarga ni modifica archivos POS.
- Migracion `V12__eventos_firma_y_lock_agente.sql` alinea la constraint de eventos con los eventos de firma del agente:
  - `SIGNATURE_VALIDATED`;
  - `SIGNATURE_VALIDATION_FAILED`.

Verificacion:

- `dotnet build FarmamiaOperationsCenter\windows-agent\Farmamia.Agent.Tests\Farmamia.Agent.Tests.csproj` finaliza con 0 errores y 0 advertencias.
- `.\mvnw.cmd test` en backend finaliza correctamente: 32 pruebas, 0 fallos, 6 saltadas por falta de Docker/Testcontainers.

Limitacion de ambiente:

- `dotnet test` no puede ejecutarse en esta maquina porque falta el runtime `Microsoft.NETCore.App 8.0.0`; solo esta instalado `10.0.8`.

### Angular

- No priorizar nuevas pantallas esteticas.
- Agregar vistas operativas:
  - plan de oleadas;
  - estado runtime de campana;
  - filtros paginados;
  - salud de agentes;
  - alertas NOC.

### Estado Implementado - Angular Operativo

Implementado en `admin-panel`:

- Nueva vista `Operaciones NOC` en la navegacion principal.
- Resumen operativo de campanas activas, estado de control, oleadas activas, agentes en riesgo y alertas abiertas.
- Selector de campanas operativas con carga del plan de orquestacion existente.
- Panel runtime de campana con planificacion, evaluacion, progreso total y control de oleadas.
- Acciones de oleada conectadas a API: iniciar, pausar y reanudar.
- Bloque de salud de agentes fuera de linea o degradados.
- Bloque de alertas NOC con acceso directo a la bandeja de alertas.

Verificacion:

- `npm run build` en `admin-panel` finaliza correctamente.
- La API local expone metricas Prometheus de OPS-005 en `/actuator/prometheus`.

## Riesgos Tecnicos

1. Orquestador mal disenado puede duplicar instrucciones.
2. Sin idempotencia, los reintentos del agente generan estados inconsistentes.
3. SQLite mal usado puede bloquear el servicio si se accede concurrentemente sin cuidado.
4. Firma digital incompleta puede dar falsa sensacion de seguridad.
5. Paginacion tardia obliga a redisenar front y API bajo presion.
6. Observabilidad sin alertas accionables se convierte en decoracion.
7. No modelar farmacias de turno puede causar impactos operativos reales.
8. No controlar ancho de banda puede saturar enlaces privados.
9. No separar dashboard ejecutivo de consola NOC degrada la operacion.
10. Retencion no definida puede inflar PostgreSQL rapidamente.

## Orden Recomendado

1. OPS-004 Paginacion y filtros globales.
2. OPS-002 Cola durable del agente.
3. Idempotencia backend para eventos/resultados.
4. OPS-003 Firma digital de paquetes.
5. OPS-001 Orquestador fase 1: grupos, oleadas, umbral de fallos y pausa automatica.
6. OPS-005 Observabilidad inicial.
7. OPS-001 fase 2: farmacias de turno, ventanas y ancho de banda.
8. Angular operativo para oleadas y monitoreo.

## Estimacion

| Modulo | Esfuerzo | Riesgo |
| --- | --- | --- |
| OPS-004 | 4-6 dias | Medio |
| OPS-002 | 5-8 dias | Alto |
| Idempotencia backend | 2-3 dias | Medio |
| OPS-003 | 5-8 dias | Alto |
| OPS-001 fase 1 | 8-12 dias | Alto |
| OPS-005 | 3-5 dias | Medio |
| Angular operativo | 5-8 dias | Medio |

Total realista: 5 a 7 semanas de trabajo enfocado para un piloto confiable. Para produccion 24/7, sumar hardening, pruebas de carga, runbooks y monitoreo operacional.

### Estado Implementado - Runbook NOC

Implementado en documentacion:

- `RUNBOOK_OPERACIONES_NOC.md` con procedimientos para:
  - API no disponible;
  - campana pausada automaticamente;
  - oleada sin avance;
  - agentes offline;
  - cola durable con `DEAD_LETTER`;
  - firma digital invalida;
  - credenciales locales protegidas;
  - crecimiento de base de datos;
  - validacion pre-campana y post-campana.
- Incluye indicadores Prometheus clave y criterios de escalamiento.

Limitacion consciente vigente:

- El runbook queda como version piloto; debe validarse con soporte/NOC durante una simulacion completa de campana.

## Impacto Esperado

### Escalabilidad

- Menos carga por listados completos.
- Mejor control de campanas masivas.
- Menos inconsistencias por reintentos.
- Base preparada para 1900 equipos sin saturar UI/API.

### Operacion Real

- El NOC podra pausar campanas antes de afectar toda la cadena.
- Los agentes podran sobrevivir cortes de red sin perder eventos.
- Los paquetes tendran origen verificable.
- Los operadores tendran senales tempranas de fallo.
- La plataforma pasara de "actualizador remoto" a "operacion controlada por campanas".

## Definicion de Hecho del Sprint

- Endpoints principales paginados.
- Agente persiste eventos offline y los reenvia tras reinicio.
- Backend procesa eventos idempotentes.
- Paquetes firmados y agente valida firma.
- Existen grupos operativos y oleadas.
- Una campana se pausa automaticamente al superar umbral de fallos.
- Prometheus puede scrapear metricas operativas.
- Grafana tiene dashboard NOC inicial.
