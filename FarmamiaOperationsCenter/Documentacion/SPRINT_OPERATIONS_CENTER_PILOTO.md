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
  - Si un objetivo no tiene oleada, conserva el comportamiento MVP.
  - Si un objetivo tiene oleada, solo recibe instruccion cuando la oleada esta `RUNNING`.
  - Si la oleada tiene ventana horaria, la instruccion solo se entrega dentro de esa ventana.
- Bloqueo de inicio de oleada si contiene farmacias marcadas como turno.
- Auditoria administrativa para planificar, evaluar, iniciar, pausar y reanudar oleadas.

Limitacion consciente de esta fase:

- El MVP actual crea targets `AUTHORIZED`; por compatibilidad no se cambio ese flujo de creacion. La compuerta por oleada ya evita entrega si el target esta planificado y su oleada no corre. La siguiente fase debe mover la autorizacion de targets nuevos a `PENDING` y agregar lease de instruccion (`instruction_lease_until`) para limitar paralelismo y evitar doble ejecucion.

Verificacion:

- `.\mvnw.cmd test`
- 27 pruebas, 0 fallos, 0 errores, 6 omitidas por Docker/Testcontainers no disponible.

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

### Agente Windows

- Agregar SQLite local.
- Convertir eventos directos a outbox durable.
- Agregar despachador en background.
- Validar firma digital antes de aplicar ZIP.
- Proteger token con DPAPI.
- Agregar lock de actualizacion.

### Angular

- No priorizar nuevas pantallas esteticas.
- Agregar vistas operativas:
  - plan de oleadas;
  - estado runtime de campana;
  - filtros paginados;
  - salud de agentes;
  - alertas NOC.

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
