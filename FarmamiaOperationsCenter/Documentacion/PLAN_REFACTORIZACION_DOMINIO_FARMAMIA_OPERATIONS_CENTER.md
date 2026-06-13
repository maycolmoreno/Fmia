# Plan de refactorizacion de dominio - Farmamia Operations Center

## Objetivo arquitectonico

Reorientar Farmamia Operations Center desde una consola generica de `devices`, `deployments`, `packages`, `branches` y `update-events` hacia una plataforma operacional para una cadena de farmacias.

Este plan no agrega funcionalidades nuevas. Ordena lenguaje, bordes publicos, navegacion, contratos y modelo operativo para que las capacidades existentes se entiendan como operacion Farmamia:

- Farmacia.
- Equipo POS.
- Version POS.
- Campana POS.
- Grupo TRX.
- Farmacia de turno.
- Agente POS.
- Evento del agente.
- Alerta operativa.
- Usuario de operaciones.

La prioridad es alineacion al negocio. La elegancia tecnica queda en segundo plano si introduce abstracciones que no ayudan al entorno Farmamia.

## Principios de la reorientacion

1. Mantener compatibilidad temporal. No eliminar aun `/api/devices`, `/api/branches`, `/api/packages` ni `/api/deployments`.
2. Introducir aliases de negocio en API, Angular y documentacion.
3. Renombrar primero lenguaje externo y modelos de aplicacion; aplazar renombres fisicos de tablas salvo que sean necesarios.
4. No crear nuevos modulos funcionales antes de estabilizar el vocabulario.
5. Evitar abstracciones genericas: el producto opera POS Farmamia, no endpoints arbitrarios.

## Estado de implementacion - Etapa 1

La etapa 1 queda implementada con aliases API de negocio y renombre visual Angular, sin modificar agente Windows, base de datos ni modelo relacional.

Aliases activos:

```http
/api/farmacias
/api/equipos-pos
/api/versiones-pos
/api/campanas-pos
/api/eventos-agente
```

Rutas legacy mantenidas:

```http
/api/branches
/api/devices
/api/packages
/api/deployments
/api/update-events
```

El panel Angular ya consume los aliases de negocio desde `OperacionesApiService` y mantiene wrappers internos legacy para reducir riesgo mientras el componente monolitico se divide en etapas posteriores.

## Fase 1 - Lenguaje canonico del dominio

### Tabla de nombres canonicos

| Nombre actual | Nombre nuevo | Motivo |
| ------------- | ------------ | ------ |
| `Sucursal` | `Farmacia` | El negocio opera farmacias. "Sucursal" es aceptable internamente, pero no debe dominar la experiencia del producto. |
| `branches` | Alias logico `farmacias` | Mantener tabla por compatibilidad, pero exponer dominio Farmamia. |
| `ControladorSucursales` | `ControladorFarmacias` | La API publica debe hablar de farmacias. |
| `RepositorioSucursales` | `RepositorioFarmacias` | Puerto de dominio alineado al negocio. |
| `RespuestaSucursal` | `RespuestaFarmacia` | DTO visible al cliente debe usar lenguaje del producto. |
| `FiltroSucursales` | `FiltroFarmacias` | Evita que el lenguaje viejo siga filtrandose a nuevas capas. |
| `/api/branches` | `/api/farmacias` | Alias de negocio. Mantener `/api/branches` como legacy temporal. |
| `Equipo` | `EquipoPos` | No son equipos genericos; son equipos Windows que ejecutan POS. |
| `devices` | Alias logico `equipos_pos` | El modelo fisico puede esperar, el contrato no. |
| `ControladorEquipos` | `ControladorEquiposPos` | Claridad operacional. |
| `RepositorioEquipos` | `RepositorioEquiposPos` | El puerto debe expresar que administra POS. |
| `RespuestaEquipo` | `RespuestaEquipoPos` | Evita ambiguedad con otros activos TI. |
| `DetalleEquipo` | `DetalleEquipoPos` | El detalle debe enfatizar version POS, agente y farmacia. |
| `MetricaEquipo` | `MetricaEquipoPos` | Las metricas son del equipo POS y agente. |
| `/api/devices` | `/api/equipos-pos` | Alias de negocio para inventario POS. |
| `PaquetePos` | `ArtefactoVersionPos` | El ZIP es artefacto. Lo que aprueba operaciones es una version POS. |
| `packages` / `pos_packages` | Alias logico `versiones_pos` + artefacto | La UI debe priorizar version; paquete es detalle tecnico. |
| `ControladorPaquetes` | `ControladorVersionesPos` | El operador no piensa en "packages"; piensa en versiones POS aprobadas. |
| `GestionarPaquetesPosCasoUso` | `GestionarVersionesPosCasoUso` | Caso de uso alineado a carga, validacion, aprobacion y retiro de versiones. |
| `RespuestaPaquetePos` | `RespuestaVersionPos` | DTO publico orientado a negocio. |
| `/api/packages` | `/api/versiones-pos` | Alias de negocio. Descarga puede conservar ruta legacy hasta migrar agente. |
| `Despliegue` | `CampanaPos` | El proceso real se ejecuta como campana, piloto, oleada, TRX o general. |
| `deployments` | Alias logico `campanas_pos` | Mantener tabla por ahora; exponer campanas. |
| `ControladorDespliegues` | `ControladorCampanasPos` | API orientada a operacion. |
| `GestionarDesplieguesCasoUso` | `GestionarCampanasPosCasoUso` | Mejor reflejo de aprobacion, programacion, pausa, reanudacion y cancelacion. |
| `OrquestarDesplieguesCasoUso` | `OrquestarCampanasPosCasoUso` | La orquestacion opera campanas POS. |
| `EstadoDespliegue` | `EstadoCampanaPos` | Estado de la campana, no de un despliegue generico. |
| `ObjetivoDespliegueEquipo` | `ObjetivoCampanaEquipoPos` | Objetivo de campana por equipo POS. |
| `PlanOrquestacionDespliegue` | `PlanOrquestacionCampanaPos` | Evita lenguaje genericista. |
| `OleadaDespliegueEntidad` | `OleadaCampanaPosEntidad` | La oleada pertenece a una campana POS. |
| `deployment_targets` | Alias logico `objetivos_campana_pos` | Representa equipos POS objetivo dentro de una campana. |
| `deployment_waves` | Alias logico `oleadas_campana_pos` | Representa oleadas de una campana POS. |
| `/api/deployments` | `/api/campanas-pos` | Alias de negocio. |
| `/api/orchestration/deployments` | `/api/campanas-pos/{id}/orquestacion` | El operador no debe navegar por "orchestration/deployments". |
| `targetGroup` / `grupoObjetivo` | `grupoTrx` | El grupo operativo real es `trx001`, `trx002`, `trx003`. |
| `GENERAL`, `IT`, `AUDIT`, strings libres | `trx001`, `trx002`, `trx003` o `GENERAL` controlado | El grupo no debe ser texto libre si gobierna oleadas reales. |
| `EventoActualizacion` | `EventoAgentePos` | Los eventos vienen del agente POS; no todos son solo actualizacion. |
| `update_events` | Alias logico `eventos_agente_pos` | La tabla puede esperar; el lenguaje no. |
| `ControladorEventosActualizacion` | `ControladorEventosAgente` | API y UI deben filtrar por evento del agente. |
| `/api/update-events` | `/api/eventos-agente` | Alias de negocio. |
| `ControladorAgente` | `ControladorAgentePos` | El agente instalado en cada POS no es un agente generico. |
| `/api/agent` | `/api/agentes-pos` | Alias de negocio para registro, latido, instrucciones y resultados. |
| `UsuarioAdministrativo` | `UsuarioOperaciones` | El usuario representa soporte/NOC/operaciones, no solo administracion. |
| `app_users` | Alias logico `usuarios_operaciones` | Mantener tabla por ahora; ajustar DTO y UI. |
| `ControladorUsuariosAdministrativos` | `ControladorUsuariosOperaciones` | Alineacion a roles de operacion. |
| `AuditoriaAdministrativa` | `AuditoriaOperativa` | Debe registrar decisiones operativas, no solo administracion. |
| `/api/audit-logs` | `/api/auditoria-operativa` | Alias recomendado; mantener legacy. |
| `AlertaEquipo` / `AlertaRegistrada` | `AlertaOperativa` | La alerta puede impactar farmacia, equipo, campana o turno. |
| `/api/alerts` | `/api/alertas-operativas` | Alias de negocio. |

### Aplicacion por capa

| Capa | Cambio controlado | Regla de compatibilidad |
| ---- | ----------------- | ----------------------- |
| Entidades de dominio Java | Crear nombres nuevos como wrappers/records canonicos o renombrar gradualmente clase por clase. | No cambiar simultaneamente persistencia, API y UI. |
| Entidades JPA | Mantener nombres fisicos inicialmente. Introducir comentarios, aliases y repositorios canonicos. | Evitar migraciones masivas de tablas en Fase 1. |
| DTOs | Crear DTOs nuevos `RespuestaFarmacia`, `RespuestaEquipoPos`, `RespuestaCampanaPos`, `RespuestaVersionPos`. | Mantener DTOs legacy mientras Angular migra. |
| Casos de uso | Renombrar hacia procesos Farmamia. | Mantener clases adaptadoras/delegadas si los controladores legacy las usan. |
| Controladores | Crear controladores alias de negocio que deleguen al mismo caso de uso. | No eliminar controladores actuales hasta cambiar agente y Angular. |
| Endpoints | Publicar aliases en espanol de negocio. | Responder mismo payload al inicio para reducir riesgo. |
| Angular | Renombrar tipos y metodos del servicio; despues apuntar a aliases. | Mantener llamadas legacy hasta que backend tenga aliases probados. |
| Navegacion | Cambiar etiquetas visibles antes de dividir componentes. | No crear pantallas nuevas todavia. |
| Documentacion | Declarar vocabulario canonico y marcar nombres legacy. | Cada README nuevo debe usar lenguaje canonico. |

## Fase 2 - Reestructuracion de navegacion Angular

### Estado actual observado

El `admin-panel` es una aplicacion Angular standalone con un `AppComponent` monolitico. Las rutas reales son solo `login` y `operaciones`; las vistas internas se controlan con `vistaActiva`.

Vistas actuales:

| Vista actual | Estado | Problema |
| ------------ | ------ | -------- |
| `dashboard` | Existe en `app.component.html` | Mide API, equipos, paquetes, despliegues, eventos y alertas; todavia suena generico. |
| `operaciones` | Existe como Operaciones NOC | Es la vista mas cercana al negocio, pero aparece despues de Paquetes, Equipos y Despliegues. |
| `equipos` | Mezcla sucursales, resumen y equipos | Debe separarse conceptualmente en Farmacias y Equipos POS, aunque siga en el mismo componente inicialmente. |
| `paquetes` | Existe | Debe convertirse visualmente en Versiones POS; paquete/ZIP debe ser subdetalle. |
| `despliegues` | Existe | Debe convertirse en Campanas POS. |
| `eventos` | Existe | Debe llamarse Eventos del Agente. |
| `alertas` | Existe | Debe llamarse Alertas Operativas. |
| `auditoria` | Existe | Debe llamarse Auditoria Operativa. |
| `usuarios` | Existe | Debe llamarse Usuarios de Operaciones. |
| `seguridad` | Existe | Debe integrarse bajo Configuracion. |

### Navegacion objetivo sin agregar funcionalidad

La navegacion visual debe evolucionar a:

```txt
Dashboard NOC
Farmacias
Versiones POS
Campanas POS
Grupos TRX
Farmacias de Turno
Alertas Operativas
Eventos del Agente
Auditoria
Usuarios
Configuracion
```

Los subitems solicitados deben ser estructura conceptual y etiquetas internas, no nuevas pantallas en esta fase:

```txt
Farmacias
 - Resumen
 - Equipos POS
 - Alertas
 - Estado Operativo

Versiones POS
 - Versiones
 - Artefactos
 - Aprobaciones

Campanas POS
 - Pilotos
 - TRX
 - Generales
 - Rollbacks
```

### Componentes a mover o dividir

| Bloque actual | Ubicacion actual | Destino conceptual | Accion recomendada |
| ------------- | ---------------- | ------------------ | ------------------ |
| Metricas generales | `vistaActiva === 'dashboard'` | Dashboard NOC | Renombrar KPIs: farmacias criticas, equipos POS offline, campanas activas, alertas operativas. |
| Equipos recientes | Dashboard | Dashboard NOC / Equipos POS | Mantener, pero mostrar farmacia, version POS y ultimo latido. |
| Ultimos paquetes | Dashboard | Versiones POS | Renombrar a versiones POS recientes. |
| Ultimos despliegues | Dashboard | Campanas POS | Renombrar a campanas POS recientes. |
| Ultimos eventos | Dashboard | Eventos del Agente | Renombrar y filtrar por agente POS. |
| Alertas recientes | Dashboard | Alertas Operativas | Mantener con prioridad de farmacia de turno cuando exista. |
| Operaciones NOC | `vistaActiva === 'operaciones'` | Dashboard NOC o Campanas POS | Debe subir de prioridad; sus widgets son el nucleo NOC. |
| Sucursales | Dentro de `equipos` | Farmacias > Resumen | Cambiar etiqueta y tipo Angular de `Sucursal` a `Farmacia`. |
| Equipos inventariados | Dentro de `equipos` | Farmacias > Equipos POS / Equipos POS | Cambiar etiqueta a Equipos POS. |
| Detalle de equipo | Dentro de `equipos` | Equipo POS detalle | Renombrar detalle y campos con lenguaje POS/agente. |
| Paquetes disponibles | `paquetes` | Versiones POS > Versiones/Artefactos | Cambiar titulo y formulario: cargar artefacto de version POS. |
| Crear despliegue | `despliegues` | Campanas POS | Cambiar copy: crear campana POS. `targetGroup` debe mostrarse como Grupo TRX. |
| Orquestacion | `operaciones` y `despliegues` | Campanas POS > TRX/Pilotos | Unificar lenguaje en campana, oleada y grupo TRX. |
| Eventos de actualizacion | `eventos` | Eventos del Agente | Cambiar etiqueta y filtros futuros. |
| Filtros de alertas | `alertas` | Alertas Operativas | Mantener estructura; cambiar textos y prioridad de turno. |
| Auditoria administrativa | `auditoria` | Auditoria Operativa | Cambiar nombres de accion/entidad hacia negocio. |
| Credenciales administrativas | `seguridad` | Configuracion | Renombrar seccion. |
| Usuarios administrativos | `usuarios` | Usuarios de Operaciones | Ajustar copy y roles. |

### Orden de refactor Angular

1. Renombrar `type Vista`: `dashboard`, `farmacias`, `versionesPos`, `campanasPos`, `gruposTrx`, `farmaciasTurno`, `alertasOperativas`, `eventosAgente`, `auditoria`, `usuarios`, `configuracion`.
2. Mantener los mismos datos y metodos, pero crear aliases de nombres en TypeScript: `sucursales` -> `farmacias`, `paquetes` -> `versionesPos`, `despliegues` -> `campanasPos`.
3. Cambiar copy visible y titulos. Esto da valor inmediato sin tocar backend.
4. Renombrar interfaces Angular: `Sucursal` -> `Farmacia`, `Equipo` -> `EquipoPos`, `PaquetePos` -> `VersionPos`, `Despliegue` -> `CampanaPos`, `EventoActualizacion` -> `EventoAgente`.
5. Extraer componentes despues de estabilizar lenguaje:
   - `DashboardNocComponent`.
   - `FarmaciasComponent`.
   - `VersionesPosComponent`.
   - `CampanasPosComponent`.
   - `AlertasOperativasComponent`.
   - `EventosAgenteComponent`.
   - `AuditoriaOperativaComponent`.
   - `UsuariosOperacionesComponent`.

No conviene dividir componentes antes del renombre conceptual: solo moveria deuda con nombres viejos.

## Fase 3 - API de negocio

### Aliases requeridos

| Endpoint legacy | Alias de negocio | Observacion |
| --------------- | ---------------- | ----------- |
| `GET /api/branches` | `GET /api/farmacias` | Misma consulta inicial. |
| `GET /api/branches/page` | `GET /api/farmacias/page` | Necesario porque Angular usa paginacion. |
| No existe directo | `GET /api/farmacias/{id}/equipos` | Puede delegar a filtro de equipos por farmacia. |
| No existe directo | `GET /api/farmacias/{id}/alertas` | Puede delegar a filtro de alertas por farmacia. |
| `GET /api/devices` | `GET /api/equipos-pos` | Inventario POS. |
| `GET /api/devices/page` | `GET /api/equipos-pos/page` | Alias prioritario para Angular. |
| `GET /api/devices/{id}` | `GET /api/equipos-pos/{id}` | Detalle POS. |
| `GET /api/packages` | `GET /api/versiones-pos` | Versiones y artefactos. |
| `GET /api/packages/page` | `GET /api/versiones-pos/page` | Alias prioritario. |
| `POST /api/packages` | `POST /api/versiones-pos` | Cargar artefacto de version POS. |
| `POST /api/packages/{id}/approve` | `POST /api/versiones-pos/{id}/aprobar` | Aprobacion de version POS. |
| `POST /api/packages/{id}/retire` | `POST /api/versiones-pos/{id}/retirar` | Retiro de version POS. |
| `GET /api/packages/{id}/download` | `GET /api/versiones-pos/{id}/descargar` | Mantener download legacy para agente hasta migrarlo. |
| `GET /api/deployments` | `GET /api/campanas-pos` | Campanas POS. |
| `GET /api/deployments/page` | `GET /api/campanas-pos/page` | Alias prioritario. |
| `POST /api/deployments` | `POST /api/campanas-pos` | Crear campana POS. |
| `GET /api/deployments/{id}/status` | `GET /api/campanas-pos/{id}/estado-por-equipo` | Estado actual por objetivo/equipo. |
| No existe directo | `GET /api/campanas-pos/{id}/estado-por-farmacia` | Alias conceptual futuro; puede agregarse cuando exista agregacion. |
| `POST /api/deployments/{id}/pause` | `POST /api/campanas-pos/{id}/pausar` | Alias directo. |
| `POST /api/deployments/{id}/resume` | `POST /api/campanas-pos/{id}/reanudar` | Alias directo. |
| `POST /api/deployments/{id}/cancel` | `POST /api/campanas-pos/{id}/cancelar` | Alias directo. |
| `/api/orchestration/deployments/{id}/...` | `/api/campanas-pos/{id}/orquestacion/...` | Mover orquestacion bajo campana. |
| `GET /api/update-events` | `GET /api/eventos-agente` | Eventos del agente POS. |
| `GET /api/update-events/page` | `GET /api/eventos-agente/page` | Alias prioritario. |
| `POST /api/agent/register` | `POST /api/agentes-pos/registro` | Registro agente POS. |
| `POST /api/agent/heartbeat` | `POST /api/agentes-pos/latidos` | Latido agente POS. |
| `GET /api/agent/{idEquipo}/instructions` | `GET /api/equipos-pos/{idEquipo}/instrucciones-agente` | Instrucciones para equipo POS. |
| `POST /api/agent/{idEquipo}/events` | `POST /api/equipos-pos/{idEquipo}/eventos-agente` | Eventos idempotentes. |
| `POST /api/agent/{idEquipo}/update-result` | `POST /api/equipos-pos/{idEquipo}/resultado-actualizacion` | Resultado de actualizacion POS. |
| No existe formal | `GET /api/grupos-trx` | Debe esperar modelo `GrupoTrx`; no inventar datos fake. |
| `GET /api/alerts/page` | `GET /api/alertas-operativas/page` | Alias directo. |
| `POST /api/alerts/{id}/acknowledge` | `POST /api/alertas-operativas/{id}/reconocer` | Alias directo. |
| `POST /api/alerts/{id}/close` | `POST /api/alertas-operativas/{id}/cerrar` | Alias directo. |

### Estrategia de migracion gradual

1. Backend publica aliases nuevos delegando a los mismos casos de uso.
2. Contrato API documenta legacy como "compatibilidad temporal".
3. Angular cambia `OperacionesApiService` para consumir aliases nuevos.
4. Agente Windows queda en endpoints legacy hasta tener version de agente compatible.
5. Se agregan headers de deprecacion a endpoints legacy administrativos, no al agente todavia.
6. Cuando Angular y agente esten migrados, se define fecha de retiro de legacy.

### Versionado recomendado

No crear `/api/v2` todavia. Para este proyecto, el problema no es version tecnica sino lenguaje. Usar aliases de negocio en `/api/...` y conservar legacy hasta que el ecosistema migre.

## Fase 4 - Modelo operativo

| Capacidad | Existe hoy | Falta | Cambio recomendado |
| --------- | ---------- | ----- | ------------------ |
| `trx001`, `trx002`, `trx003` | Parcial. Existe `target_group` como string libre. | Catalogo formal, validacion, conteo y relacion con equipos/farmacias. | Crear concepto `GrupoTrx`; en DB puede iniciar como tabla `operational_groups` o `grupos_trx`. |
| Maximo 100 equipos por grupo | No. `maxParallelDevices` permite hasta 500 y `target_group` no controla membresia. | Regla de limite por grupo TRX. | Validar al crear/asignar grupo TRX y al crear campana TRX. |
| 1 descarga simultanea por farmacia | No claramente. Hay lease por objetivo/equipo y paralelismo por oleada. | Lock o contador por farmacia/campana. | Agregar regla de entrega de instrucciones: no emitir descarga si ya hay equipo de la misma farmacia descargando. |
| Farmacias de turno | Parcial. `branches.is_on_duty` existe y la orquestacion bloquea oleadas con farmacias de turno. | Definir si se priorizan o se protegen. | Resolver politica: "turno primero" o "turno requiere excepcion"; hoy el sistema mezcla ambas intenciones. |
| Campanas piloto | Si. `pilot_required`, `is_pilot` y oleadas piloto existen. | Lenguaje de UI/API. | Renombrar y exponer como Campanas POS > Pilotos. |
| Campanas generales | Si parcialmente. Hay despliegues con objetivos y grupo `GENERAL`. | Diferenciar campana general de campana TRX. | Introducir `tipoCampana`: `PILOTO`, `TRX`, `GENERAL` cuando se toque modelo. |
| Rollback por campana | Parcial. El agente hace rollback por equipo al fallar; no hay accion operativa por campana. | Caso de uso administrativo auditable. | No implementarlo aun, pero reservar en modelo de dominio como proceso futuro. |
| Rollback por equipo | Parcial. Existe rollback tecnico automatico en agente. | Endpoint/accion manual y visibilidad clara. | Renombrar eventos y estado para distinguir rollback automatico vs solicitado. |
| Heartbeat | Si. | Enriquecer con cola offline/diagnostico cuando toque. | Mantener generico en mecanismo, pero exponer como latido de agente POS. |
| Cola offline | Si en agente. | Visibilidad en backend/UI. | No crear pantalla; preparar contrato de diagnostico cuando se aborde monitoreo. |
| Idempotencia | Si. | Mantener. | Es una abstraccion tecnica correcta. |

## Fase 5 - Dashboard NOC

El Dashboard NOC debe responder preguntas operativas, no listar entidades genericas.

### Preguntas que debe contestar

| Pregunta | Fuente actual | Brecha |
| -------- | ------------- | ------ |
| Que farmacias estan criticas? | Alertas + farmacias | Falta agregado por farmacia y prioridad de turno. |
| Que equipos POS estan offline? | Equipos + heartbeat | Existe, pero debe verse como riesgo de farmacia/POS. |
| Que campana esta activa? | Despliegues/campanas | Existe, pero lenguaje y agrupacion deben cambiar. |
| Que version POS esta desplegandose? | Paquetes + despliegues | Existe como `packageVersion`, debe verse como `versionPos`. |
| Que grupo TRX tiene problemas? | `targetGroup` | Falta modelo formal y KPI. |
| Que farmacias de turno tienen riesgo? | `is_on_duty` + alertas/equipos | Falta KPI especifico. |
| Que alertas requieren atencion? | Alertas | Existe, falta priorizacion por impacto operativo. |

### Widgets propuestos

| Widget | KPI principal | Por que importa |
| ------ | ------------- | --------------- |
| Farmacias criticas | Numero de farmacias con alerta critica abierta | El NOC opera continuidad por farmacia. |
| Farmacias de turno en riesgo | Turno con equipo offline, alerta critica o campana pendiente | Prioridad operacional maxima. |
| Equipos POS offline | Equipos sin latido o fuera de linea | Detecta caida de agente/enlace/POS. |
| Campanas POS activas | Campanas en piloto, TRX o general | Control de actualizaciones. |
| Version POS en despliegue | Version objetivo de campanas activas | Visibilidad de release operacional. |
| Grupo TRX con fallos | Fallos/pendientes por `grupoTrx` | Control de oleadas reales. |
| Pendientes antes de apertura | Equipos con campana pendiente y ultimo latido antiguo | Riesgo de update atrasado. |
| Alertas operativas abiertas | Criticas/altas sin reconocer | Cola de atencion NOC. |
| Rollbacks recientes | Rollbacks completados/fallidos | Indica salud de version/campana. |
| Cola offline del agente | Eventos pendientes/dead-letter por equipo | Indica desconexion o falla de despacho. |

### KPIs que deben reemplazar a los genericos

| KPI actual | KPI recomendado |
| ---------- | --------------- |
| `Equipos` | Equipos POS online/offline |
| `Paquetes` | Versiones POS aprobadas |
| `Despliegues` | Campanas POS activas |
| `Eventos` | Eventos del agente criticos |
| `Alertas` | Alertas operativas abiertas |
| `API` | Salud plataforma central |

## Fase 6 - Backlog de reorientacion

| ID | Nombre | Descripcion | Impacto | Complejidad | Prioridad |
| -- | ------ | ----------- | ------- | ----------- | --------- |
| P0-01 | Vocabulario canonico | Documentar y adoptar nombres oficiales: Farmacia, Equipo POS, Version POS, Campana POS, Grupo TRX, Agente POS, Evento del Agente. | Alto | Baja | P0 |
| P0-02 | Aliases API de lectura | Crear aliases `GET /api/farmacias`, `/api/equipos-pos`, `/api/versiones-pos`, `/api/campanas-pos`, `/api/eventos-agente`. | Alto | Media | P0 |
| P0-03 | Renombre Angular visible | Cambiar etiquetas: Paquetes -> Versiones POS, Despliegues -> Campanas POS, Equipos -> Equipos POS, Eventos -> Eventos del Agente. | Alto | Baja | P0 |
| P0-04 | Renombre modelos Angular | Cambiar interfaces `Sucursal`, `Equipo`, `PaquetePos`, `Despliegue`, `EventoActualizacion` hacia nombres canonicos. | Alto | Media | P0 |
| P0-05 | Servicio Angular con aliases | Agregar metodos `listarFarmacias`, `listarEquiposPos`, `listarVersionesPos`, `listarCampanasPos` y migrar llamadas internas. | Alto | Media | P0 |
| P0-06 | Controladores alias | Crear controladores de negocio que deleguen a casos de uso existentes. | Alto | Media | P0 |
| P0-07 | Politica legacy | Marcar endpoints legacy como compatibilidad temporal en documentacion y contrato API. | Medio | Baja | P0 |
| P0-08 | Decision Farmacias de Turno | Definir si las farmacias de turno se priorizan primero o se bloquean salvo excepcion. | Alto | Baja | P0 |
| P1-01 | Renombre casos de uso | Renombrar `GestionarDesplieguesCasoUso`, `OrquestarDesplieguesCasoUso`, `GestionarPaquetesPosCasoUso` hacia Campanas/Versiones POS. | Alto | Media | P1 |
| P1-02 | Catalogo Grupo TRX | Crear concepto `GrupoTrx` y reemplazar `targetGroup` libre en nuevas operaciones. | Alto | Media | P1 |
| P1-03 | Validacion max 100 TRX | Validar maximo 100 equipos por grupo TRX. | Alto | Media | P1 |
| P1-04 | Dashboard NOC semantico | Reordenar widgets actuales para responder preguntas NOC. | Alto | Media | P1 |
| P1-05 | Estado por farmacia | Agregar agregacion de estado de campana por farmacia. | Alto | Media/Alta | P1 |
| P1-06 | Alias alertas/auditoria | Crear `/api/alertas-operativas` y `/api/auditoria-operativa`. | Medio | Baja/Media | P1 |
| P1-07 | Auditoria de negocio | Normalizar acciones auditadas con nombres como `APROBAR_VERSION_POS`, `PAUSAR_CAMPANA_POS`. | Medio | Media | P1 |
| P1-08 | Bloqueo 1 descarga por farmacia | Incorporar regla de descarga simultanea por farmacia en entrega de instrucciones. | Alto | Alta | P1 |
| P1-09 | Priorizacion de turno en alertas | Elevar criticidad/prioridad cuando farmacia de turno este afectada. | Alto | Media | P1 |
| P1-10 | Separacion progresiva de componentes Angular | Extraer componentes despues del renombre conceptual. | Medio | Media | P1 |
| P2-01 | Renombre fisico DB | Evaluar migracion de tablas fisicas `branches/devices/deployments` a nombres de negocio o views canonicas. | Medio | Alta | P2 |
| P2-02 | Rollback manual por campana | Disenar caso de uso administrativo auditado para rollback por campana. | Alto | Alta | P2 |
| P2-03 | Rollback manual por equipo | Disenar accion auditada por equipo POS. | Alto | Alta | P2 |
| P2-04 | Diagnostico cola agente en UI | Exponer cola offline/dead-letter en detalle de equipo POS. | Medio | Media | P2 |
| P2-05 | Versionado formal API | Definir retiro de legacy y versionado si hay consumidores externos. | Medio | Media | P2 |

## Tablas y modelo relacional a fortalecer

| Tabla actual | Estado | Fortalecimiento recomendado |
| ------------ | ------ | --------------------------- |
| `branches` | Representa farmacias razonablemente. | Alias logico `farmacias`; agregar datos operativos cuando se requiera: enlace corporativo, prioridad, horario, turno vigente. |
| `devices` | Representa equipos POS. | Alias `equipos_pos`; reforzar rol/numero POS, diagnostico agente, cola offline resumida. |
| `pos_packages` | Representa artefactos/versiones POS. | Separar mentalmente `VersionPos` de artefacto ZIP; tabla puede mantenerse. |
| `deployments` | Representa campanas POS. | Alias `campanas_pos`; agregar `tipo_campana` cuando se implemente TRX formal. |
| `deployment_targets` | Representa objetivos por equipo. | Alias `objetivos_campana_pos`; base para estado por farmacia/equipo. |
| `deployment_waves` | Representa oleadas. | Alias `oleadas_campana_pos`; asociar formalmente a `grupo_trx` cuando exista. |
| `update_events` | Representa eventos del agente. | Alias `eventos_agente_pos`; ampliar semantica mas alla de update. |
| `alerts` | Representa alertas operativas. | Fortalecer impacto por farmacia de turno, campana y grupo TRX. |
| `audit_logs` | Auditoria tecnica correcta. | Normalizar acciones de negocio. |
| No existe | `grupos_trx` | Crear cuando se pase de string libre a modelo operativo. |

## Conceptos de dominio a crear

1. `GrupoTrx`: codigo, descripcion, maximo equipos, activo, prioridad.
2. `VersionPos`: version funcional aprobable; puede usar `pos_packages` como persistencia inicial.
3. `CampanaPos`: nombre canonico de `Despliegue`; debe expresar tipo piloto/TRX/general.
4. `EstadoCampanaPorFarmacia`: agregado operacional para NOC.
5. `FarmaciaTurno`: formalizar vigencia/prioridad si `is_on_duty` queda corto.
6. `AgentePos`: identidad operacional del agente instalado en un Equipo POS.
7. `DiagnosticoAgentePos`: cola offline, dead-letter, ultimo envio y lock local.

## Que mantener generico

- JWT, autenticacion y roles.
- Paginacion, filtros y ordenamiento.
- Auditoria como mecanismo.
- Firma, checksum y almacenamiento de artefactos.
- Idempotencia de eventos.
- Retencion operativa.
- Metricas tecnicas Micrometer/Prometheus.
- Excepciones y manejo HTTP.

## Que NO abstraer todavia

- No convertir POS en "aplicaciones" genericas.
- No convertir Farmacia en "tenant", "site" o "location" generico.
- No generalizar el agente a cualquier sistema operativo.
- No crear un motor universal de despliegues.
- No permitir `targetGroup` libre como sustituto de TRX.
- No ocultar `Zabyca.Pos.Desktop.exe` ni la ruta POS oficial detras de configuracion innecesaria.
- No disenar para multiples cadenas de farmacia si el producto es Farmamia.

## Secuencia recomendada por semanas

### Semana 1

- Aprobar vocabulario canonico.
- Crear aliases API de lectura.
- Cambiar copy visible de Angular.
- Actualizar contrato API y README con nombres legacy/canonicos.

### Semana 2

- Migrar `OperacionesApiService` a metodos canonicos.
- Renombrar interfaces Angular.
- Crear controladores alias para acciones principales: aprobar version, crear campana, pausar/reanudar/cancelar campana.
- Definir politica de farmacias de turno.

### Semana 3

- Renombrar casos de uso principales o crear fachadas canonicas.
- Disenar `GrupoTrx` formal.
- Validar limite de 100 equipos por grupo TRX en el punto de creacion/asignacion.
- Reordenar Dashboard NOC con KPIs semanticos existentes.

### Semana 4

- Implementar estado por farmacia para campanas si ya esta aprobado.
- Incorporar bloqueo de 1 descarga simultanea por farmacia.
- Normalizar auditoria de negocio.
- Preparar plan de migracion del agente a aliases nuevos.

## Resultado esperado

Al terminar esta reorientacion, Farmamia Operations Center debe sentirse y leerse asi:

- El operador entra a un Dashboard NOC, no a un panel tecnico generico.
- La unidad principal es la Farmacia.
- Los equipos son Equipos POS con agente, version POS y latido.
- Las actualizaciones son Campanas POS sobre Versiones POS.
- Los grupos operativos son TRX, no strings libres.
- Las alertas se entienden por impacto operativo, especialmente farmacias de turno.
- La auditoria registra decisiones de soporte y operacion.
- Los endpoints legacy existen, pero el lenguaje oficial del producto ya no depende de ellos.

El exito no es tener mas pantallas. El exito es que cada clase, endpoint, texto visible y reporte empuje la misma idea: Farmamia Operations Center opera farmacias y POS Farmamia, no endpoints anonimos.
