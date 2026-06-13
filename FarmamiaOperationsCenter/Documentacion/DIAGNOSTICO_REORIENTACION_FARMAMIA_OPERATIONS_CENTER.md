# Diagnostico de reorientacion Farmamia Operations Center

## Veredicto

El proyecto no es una plataforma generica vacia: ya contiene decisiones correctas para Farmamia, como agente Windows, ruta POS, paquetes POS firmados, ventana 23:55-01:00, idempotencia de eventos, cola offline, rollback, farmacias de turno y orquestacion por oleadas.

Pero la estructura publica y una parte del lenguaje del dominio si se estan yendo a una consola generica de endpoints. El problema principal no es tecnico sino de modelado: muchas piezas internas conocen el negocio, pero la API, la UI y varias entidades aun hablan como plataforma de "devices", "packages", "deployments", "branches" y "update-events". Eso diluye el producto. Farmamia Operations Center debe hablar de farmacias, equipos POS, versiones POS, campanas POS, grupos TRX, turnos, alertas operativas y eventos del agente.

## Diagnostico critico

| Area | Esta alineada al negocio? | Problema | Cambio recomendado |
| ---- | -------------------------- | -------- | ------------------ |
| Dominio | Parcialmente | `Sucursal`, `Equipo`, `PaquetePos` y `Despliegue` representan el negocio, pero `Despliegue`, `targetGroup`, `EventoActualizacion` y `UsuarioAdministrativo` aun suenan genericos. Falta separar `VersionPos` de `PaquetePos`. | Renombrar `Despliegue` a `CampanaPos`, `Equipo` a `EquipoPos`, `Sucursal` a `Farmacia`, `targetGroup` a `grupoTrx`, y crear `VersionPos` como concepto propio. |
| Casos de uso | Parcialmente | Hay casos utiles, pero `GestionarDesplieguesCasoUso`, `OrquestarDesplieguesCasoUso`, `ConsultarCatalogoOperativoCasoUso` y `GestionarPaquetesPosCasoUso` son demasiado amplios. | Dividir por procesos: registrar equipo POS, reportar latido, aprobar version POS, crear campana piloto, crear campana TRX, consultar pendientes por farmacia, ejecutar rollback. |
| API | Baja a media | Endpoints publicos como `/api/devices`, `/api/deployments`, `/api/packages`, `/api/branches`, `/api/update-events` son genericos y en ingles. | Publicar endpoints de negocio: `/api/farmacias`, `/api/equipos-pos`, `/api/campanas-pos`, `/api/versiones-pos`, `/api/grupos-trx`, `/api/eventos-agente`. |
| Angular | Parcialmente | Tiene vista NOC y menciones de campanas, pero la navegacion prioriza paquetes, equipos y despliegues. Falta que Farmacias sea entidad principal. | Reordenar navegacion: Dashboard NOC, Farmacias, Equipos POS, Campanas POS, Versiones POS, Grupos TRX, Farmacias de turno, Alertas, Eventos, Auditoria, Usuarios, Configuracion. |
| Base de datos | Media | El modelo fisico usa nombres genericos (`branches`, `devices`, `deployments`, `deployment_targets`, `target_group`) aunque los campos contienen reglas reales. Falta tabla formal de grupos TRX. | Mantener tablas fisicas si no conviene migrar aun, pero crear capa logica: `farmacias`, `equipos_pos`, `campanas_pos`, `objetivos_campana_pos`, `grupos_trx`. Agregar tabla `operational_groups`/`grupos_trx`. |
| Agente | Alta | Esta bastante alineado: Windows, ruta POS, cierre/reapertura, rollback, firma, mutex, cola offline e idempotencia. | Fortalecer contrato del agente con nombres Farmamia: `/api/agentes-pos/...`, eventos `POS_*`, diagnostico de cola y estado local visibles en UI. |
| Alertas | Media | Alertas existen y se asocian a farmacia/equipo, pero no distinguen suficientemente farmacia de turno ni criticidad operacional por ventana/campana. | Agregar reglas explicitas: alerta critica prioritaria si farmacia de turno falla, si grupo TRX se bloquea, si cola offline crece, si equipo POS no reporta antes de apertura. |
| Despliegues | Parcialmente | Hay oleadas, pilotos, pausas y leases. Pero el limite de paralelismo normaliza hasta 500, no a 100 por grupo TRX, y `targetGroup` no modela TRX formal. | Convertir despliegues a `CampanaPos`; imponer grupo TRX maximo 100 equipos; planificar por farmacia y TRX, no solo por lista de equipos. |
| Auditoria | Media | Registra acciones, entidad e IP, pero los nombres de entidad son genericos y no siempre reflejan decision operativa. | Auditar eventos de negocio: `APROBAR_VERSION_POS`, `INICIAR_CAMPANA_TRX`, `PAUSAR_OLEADA_TRX`, `EXCEPTUAR_FARMACIA_TURNO`, `EJECUTAR_ROLLBACK_POS`. |
| Monitoreo | Media | Hay metricas Farmamia y dashboard NOC, pero falta tablero centrado en farmacias, TRX y turno. | Medir por farmacia, grupo TRX, version POS, campana y turno; agregar KPIs de latido, cola offline, pendientes antes de apertura y fallas por oleada. |

## Nombres que deben cambiar

| Actual | Recomendado | Motivo |
| ------ | ----------- | ------ |
| `Sucursal` / `branches` / `/api/branches` | `Farmacia` / alias logico `farmacias` / `/api/farmacias` | El usuario opera farmacias, no sucursales genericas. |
| `Equipo` / `devices` / `/api/devices` | `EquipoPos` / alias logico `equipos_pos` / `/api/equipos-pos` | El objetivo son POS Windows Farmamia. |
| `Despliegue` / `deployments` / `/api/deployments` | `CampanaPos` / alias logico `campanas_pos` / `/api/campanas-pos` | La actualizacion ocurre por campanas, pilotos, oleadas y TRX. |
| `PaquetePos` / `/api/packages` | `PaqueteVersionPos` o `ArtefactoVersionPos` / `/api/versiones-pos` | El operador aprueba versiones POS, no paquetes abstractos. |
| `targetGroup` | `grupoTrx` | El grupo operativo real es `trx001`, `trx002`, `trx003`. |
| `EventoActualizacion` / `/api/update-events` | `EventoAgentePos` / `/api/eventos-agente` | Los eventos vienen del agente POS, no solo de actualizaciones. |
| `UsuarioAdministrativo` | `UsuarioSoporte` o `UsuarioOperaciones` | El actor real es soporte/NOC/operaciones. |
| `deployment_targets` | `objetivos_campana_pos` | Representa equipos POS objetivo dentro de una campana. |
| `deployment_waves` | `oleadas_campana_pos` | La oleada pertenece a una campana POS. |

## Modulos a reorganizar

1. `farmacias`: farmacias, turno, zona, enlace corporativo, estado operativo y resumen de equipos.
2. `equipos-pos`: registro, inventario, heartbeat, version POS, version agente, ruta POS, estado de conexion.
3. `versiones-pos`: version, artefacto ZIP, firma, aprobacion, retiro, checksum, evidencia.
4. `campanas-pos`: piloto, TRX, oleadas, ventana, aprobacion, estado por farmacia y por equipo.
5. `grupos-trx`: definicion `trx001`, `trx002`, `trx003`, limite 100 equipos, asignacion de equipos/farmacias.
6. `agentes-pos`: contrato del agente, instrucciones, eventos idempotentes, diagnostico de cola.
7. `alertas-operativas`: severidad, farmacia, equipo, campana, turno, SLA y cierre.
8. `auditoria-operativa`: acciones de soporte y decisiones de campana.

## Endpoints recomendados

Mantener compatibilidad temporal con los endpoints actuales, pero introducir una API v2 o aliases de negocio.

| Actual | Recomendado |
| ------ | ----------- |
| `GET /api/branches` | `GET /api/farmacias` |
| `GET /api/devices` | `GET /api/equipos-pos` |
| `GET /api/devices/{id}` | `GET /api/equipos-pos/{id}` |
| No existe directo | `GET /api/farmacias/{id}/equipos` |
| No existe directo | `GET /api/farmacias/{id}/alertas` |
| `GET/POST /api/packages` | `GET/POST /api/versiones-pos` |
| `POST /api/packages/{id}/approve` | `POST /api/versiones-pos/{id}/aprobar` |
| `GET/POST /api/deployments` | `GET/POST /api/campanas-pos` |
| `GET /api/deployments/{id}/status` | `GET /api/campanas-pos/{id}/estado-por-equipo` |
| No existe directo | `GET /api/campanas-pos/{id}/estado-por-farmacia` |
| `/api/orchestration/deployments/...` | `/api/campanas-pos/{id}/orquestacion/...` |
| `GET /api/update-events` | `GET /api/eventos-agente` |
| `POST /api/agent/heartbeat` | `POST /api/agentes-pos/latidos` |
| `GET /api/agent/{idEquipo}/instructions` | `GET /api/equipos-pos/{id}/instrucciones-agente` |
| No existe formal | `GET /api/grupos-trx` |
| No existe formal | `POST /api/campanas-pos/{id}/rollback` |

## Pantallas a redisenar

1. Dashboard NOC: debe abrir con farmacias criticas, equipos sin heartbeat, campanas activas, farmacias de turno afectadas y grupos TRX en riesgo.
2. Farmacias: debe ser vista principal, con codigo, ciudad/zona, turno, equipos POS, ultimo heartbeat agregado, version POS dominante, alertas abiertas y campanas pendientes.
3. Equipos POS: debe mostrar farmacia, hostname, IP corporativa, version POS, version agente, ruta POS, estado del proceso POS, disco, ultimo heartbeat y cola offline.
4. Campanas POS: debe hablar de pilotos, oleadas, TRX, ventana 23:55-01:00, progreso por farmacia/equipo, fallos y rollback.
5. Versiones POS: debe reemplazar "Paquetes POS" como lenguaje principal; el ZIP es un artefacto de la version.
6. Grupos TRX: pantalla propia para `trx001`, `trx002`, `trx003`, conteo de equipos, limite 100 y estado de campanas.
7. Farmacias de turno: vista operacional prioritaria, con alertas criticas, equipos pendientes y bloqueo/permiso explicito para campanas.
8. Eventos: debe llamarse Eventos del agente y filtrar por farmacia, equipo POS, campana, version y tipo.

## Entidades a fortalecer

- `Farmacia`: codigo, zona, ciudad, enlace corporativo, turno, horario operativo, prioridad, estado agregado.
- `EquipoPos`: numero/rol POS dentro de farmacia, ruta POS oficial, ejecutable esperado, ultima version POS, ultimo latido, estado de agente, diagnostico de cola.
- `AgentePos`: version, token, ultimo heartbeat, modo offline, cola pendiente, lock de actualizacion.
- `VersionPos`: version funcional, estado de aprobacion, firma, checksum, artefacto, aprobador, fecha de aprobacion.
- `CampanaPos`: tipo `PILOTO`/`TRX`/`GENERAL`, version objetivo, ventana, estado, aprobacion, politica de rollback.
- `GrupoTrx`: codigo `trx001`, maximo 100 equipos, farmacias/equipos asignados, prioridad.
- `FarmaciaTurno`: vigencia, prioridad, reglas de bloqueo y excepcion.
- `AlertaOperativa`: farmacia, equipo, campana, severidad, tipo, SLA, impacto en turno.

## Reglas reales: estado actual

| Regla | Estado observado | Recomendacion |
| ----- | --------------- | ------------- |
| Maximo 1 descarga por farmacia | No esta modelada claramente. Hay leases por equipo y paralelismo por oleada, pero no bloqueo por farmacia. | Agregar `download_lock` por farmacia/campana o contador de leases activos por `branch_id`. |
| Grupos TRX maximo 100 equipos | No esta garantizado. `maxParallelDevices` permite hasta 500. | Crear `GrupoTrx` formal y validar maximo 100 equipos por grupo. |
| Farmacias de turno primero | Esta detectado, pero la regla actual bloquea iniciar oleadas con farmacias de turno sin excepcion. | Decidir regla real: si son primero, crear oleada prioritaria de turno; si se protegen, pedir excepcion explicita. Hoy hay ambiguedad. |
| Ventana 23:55 a 01:00 | Existe en `deployments` y validacion de agente. | Llevarla a politica de campana y mostrarla fuerte en UI. |
| Cierre y reapertura del POS | Implementado en agente. | Exponer resultado en eventos y detalle de equipo. |
| Rollback | Implementado en agente y eventos. | Agregar caso de uso/API administrativo para rollback manual por equipo/campana. |
| Actualizar antes de abrir POS si estuvo apagado | Parcial. Si el agente consulta fuera de ventana puede quedar bloqueado segun campana/oleada. | Agregar regla de catch-up antes de apertura: equipo apagado recibe instruccion pendiente antes de iniciar POS. |
| Alertas criticas por farmacia de turno | Parcial. Alertas criticas por fallo existen, pero no se eleva por turno. | Si `farmacia.deTurno=true`, elevar severidad/prioridad y mostrar en tablero NOC. |
| Eventos idempotentes del agente | Implementado. | Mantener. Es una abstraccion correcta. |
| Cola offline del agente | Implementada en agente. | Mostrar diagnostico en backend/UI y alertar por `DEAD_LETTER` o cola envejecida. |

## Cosas que no conviene abstraer todavia

- No abstraer a "endpoint", "asset", "software package" o "generic deployment".
- No generalizar el agente para Linux/macOS.
- No modelar multiples aplicaciones si el objetivo actual es `Zabyca.Pos.Desktop.exe`.
- No ocultar la ruta POS oficial tras configuraciones genericas sin valor operativo.
- No crear un motor universal de workflows; las reglas Farmamia deben estar explicitas.
- No diluir TRX como un simple string libre.

## Cosas que si pueden mantenerse genericas

- Paginacion, filtros, ordenamiento y DTO de pagina.
- Auditoria tecnica como mecanismo, aunque las acciones deben ser de negocio.
- Autenticacion, roles y politicas de seguridad.
- Almacenamiento de artefactos, checksum y firma.
- Idempotencia de eventos.
- Retencion operativa.
- Metricas Micrometer/Prometheus, siempre que las etiquetas hablen de Farmamia.

## Backlog inmediato

1. Crear lenguaje canonico del dominio: `Farmacia`, `EquipoPos`, `CampanaPos`, `VersionPos`, `GrupoTrx`, `EventoAgentePos`.
2. Introducir endpoints de negocio como aliases sin romper los actuales.
3. Redisenar navegacion Angular para que Farmacias y Campanas POS sean centrales.
4. Crear tabla/modelo `grupos_trx` y reemplazar `target_group` libre por FK o validacion fuerte.
5. Validar maximo 100 equipos por grupo TRX.
6. Implementar bloqueo de maximo 1 descarga activa por farmacia.
7. Resolver regla de farmacias de turno: priorizar primero o bloquear salvo excepcion, pero no ambas.
8. Crear estado por farmacia para campanas POS.
9. Agregar detalle de cola offline del agente en heartbeat o endpoint diagnostico.
10. Elevar alertas criticas cuando la farmacia afectada este de turno.
11. Agregar rollback manual POS como caso de uso administrativo auditado.
12. Actualizar README y contrato API para que el producto deje de presentarse como MVP generico de actualizacion.

## Prioridad recomendada

Primero cambiar lenguaje y bordes publicos: API, Angular y documentacion. Despues reforzar modelo relacional con `grupos_trx`, estado por farmacia y bloqueo de descarga por farmacia. La razon es practica: el sistema ya tiene reglas tecnicas utiles, pero mientras el operador vea "devices/deployments/packages", el producto seguira pareciendo una consola generica aunque internamente haga cosas correctas.
