# Modelo operativo Farmamia Operations Center

## Contexto operacional

Farmamia Operations Center debe operar una red de aproximadamente 645 farmacias y 1900 equipos Windows POS, con conectividad por enlace corporativo hacia un servidor central. El objetivo no es administrar endpoints genericos, sino sostener la continuidad operativa de farmacias, POS, agentes, versiones, campanas, alertas, inventario TI y soporte.

La unidad operacional principal es la farmacia. El equipo POS es el activo critico dentro de esa farmacia. La campana POS es el mecanismo controlado para mover una version POS por pilotos, grupos TRX, oleadas y ventanas.

## Estado de implementacion - Fase A

La Fase A queda implementada con Farmacia como entidad operacional principal del NOC.

Capacidades activas:

- Vista de estado operacional por farmacia en Angular.
- Agregacion de equipos POS por farmacia.
- Agregacion de alertas abiertas y criticas por farmacia.
- Agregacion de campanas activas, objetivos pendientes y fallidos por farmacia.
- KPI de farmacias criticas.
- KPI de farmacias de turno en riesgo.
- Dashboard NOC reorientado a farmacias.
- Endpoint de lectura operacional:

```http
GET /api/farmacias/estado
GET /api/farmacias/{id}/estado
```

No se crearon tablas nuevas y no se modifico el agente Windows.

## Estado de implementacion - P0-003 Farmacias de Turno

La primera version operativa de Farmacias de Turno queda implementada usando unicamente `farmacia.deTurno`.

Capacidades activas:

- Elevacion de estado operacional para farmacias de turno con POS offline, heartbeat vencido, update fallido, rollback fallido o campana pendiente fuera de ventana.
- Estado `EN_RIESGO` para rollback completado o Grupo TRX pausado con campana pendiente.
- Elevacion automatica de severidad de alertas cuando el equipo POS pertenece a una farmacia de turno.
- Bloque superior Dashboard NOC: `Farmacias de Turno en Riesgo`.
- Advertencia visible al crear una campana que incluye farmacias de turno.
- Auditoria `ADVERTENCIA_CAMPANA_CON_TURNO`.

No se crearon tablas `FarmaciaTurno`, calendarios, workflows ni excepciones avanzadas.

## Estado de implementacion - P0-004 Campanas POS y Grupo TRX Farmacia First

P0-004 formaliza Grupo TRX como mecanismo de control dentro de Campanas POS sin desplazar a Farmacia como unidad de impacto.

Capacidades activas:

- Asociacion formal Campana POS - Grupo TRX.
- Estado de campana por Grupo TRX con KPIs obligatorios de farmacia.
- `targetGroup` legacy queda como fallback cuando no existe `grupoTrxId`.
- Pausa de Grupo TRX dentro de campana bloquea nuevas instrucciones del agente.
- Dashboard de Campanas POS muestra Estado por Grupo TRX con farmacias antes que equipos POS.
- Auditoria de asociar, quitar, pausar, reanudar y advertir impacto en farmacias de turno.

Jerarquia respetada:

```txt
Campana POS
Grupo TRX como control
Farmacias impactadas
Equipos POS afectados
```

La lectura NOC prioritaria sigue siendo:

```txt
Farmacia en riesgo
Campana POS
Grupo TRX
Equipo POS
```

## Estado de implementacion - P0-001 Grupo TRX Formal

Grupo TRX queda formalizado como entidad operacional para controlar oleadas POS y reducir riesgo en campanas.

Capacidades activas:

- Tabla `grupos_trx`.
- Relacion `equipo_pos_grupo_trx`.
- Maximo 100 equipos POS por grupo.
- Un equipo POS pertenece como maximo a un Grupo TRX.
- Grupos con estados `ACTIVO`, `PAUSADO`, `RETIRADO`.
- Un grupo pausado o retirado no acepta nuevas asignaciones.
- API operacional `/api/grupos-trx`.
- Vista Angular `Grupos TRX`.
- Auditoria de crear, modificar, pausar, reanudar, retirar, asignar y quitar equipos.

No se implemento balance automatico, simulacion, pausa automatica ni reglas especiales de farmacias de turno.

## Estado de implementacion - P0-002 Estado de Campana por Farmacia

La campana POS ya puede leerse por impacto operacional en farmacias.

Capacidades activas:

- Endpoint `GET /api/campanas-pos/{id}/estado-por-farmacia`.
- Estado tecnico por farmacia: `PENDIENTE`, `EN_PROGRESO`, `COMPLETADA`, `COMPLETADA_CON_FALLOS`, `FALLIDA`.
- Estado operacional por farmacia: `NORMAL`, `EN_RIESGO`, `CRITICA`.
- KPIs por campana: farmacias objetivo, completadas, pendientes, en progreso, con errores, en riesgo, criticas, turno en riesgo, avance y exito.
- Detalle de equipos POS por farmacia dentro de la campana.
- Filtros por estado tecnico, estado operacional, grupo TRX, turno y busqueda por farmacia.

No se crearon tablas nuevas. La lectura se calcula desde campanas, objetivos, equipos POS, farmacias, alertas y Grupos TRX.

## Actores

| Actor | Responsabilidad | Necesita ver/operar |
| ----- | --------------- | ------------------- |
| Operador NOC | Vigilar continuidad diaria de farmacias y equipos POS. | Dashboard NOC, alertas, equipos offline, farmacias de turno, campanas activas. |
| Soporte Nivel 1 | Atender incidentes basicos de farmacia/POS/agente. | Farmacia, equipo POS, ultimo heartbeat, version POS, eventos del agente, alertas abiertas. |
| Soporte Nivel 2 | Diagnosticar fallas tecnicas, rollback, problemas de agente, red o version. | Detalle tecnico de equipo, eventos, metricas, cola offline, historial de campanas. |
| Lider de Operaciones TI | Autorizar campanas, pilotos, oleadas y decisiones criticas. | Estado por farmacia, estado por grupo TRX, fallas, riesgo en farmacias de turno. |
| Administrador de Version POS | Cargar, validar, aprobar, retirar versiones POS. | Versiones POS, artefactos, firma, checksum, aprobaciones, auditoria. |
| Responsable de Farmacias de Turno | Verificar continuidad de farmacias priorizadas por turno. | Farmacias de turno, alertas criticas, equipos offline, campanas pendientes. |
| Auditor / Control interno | Revisar acciones realizadas sobre versiones, campanas, alertas y usuarios. | Auditoria operativa, usuario, accion, entidad, fecha, IP, valores antes/despues. |
| Administrador de Usuarios | Gestionar usuarios, roles y accesos. | Usuarios de operaciones, roles, estado, bloqueo, ultimo acceso. |
| Agente POS | Reportar inventario, latido, eventos, resultados y ejecutar instrucciones. | API agente, instrucciones, descarga, validacion, backup, update, rollback, cola offline. |

## Procesos diarios

| Proceso | Frecuencia | Objetivo | Resultado esperado |
| ------- | ---------- | -------- | ------------------ |
| Apertura operacional NOC | Inicio de jornada | Revisar salud general antes de operacion de farmacias. | Lista de farmacias criticas, POS offline, alertas abiertas y campanas activas. |
| Monitoreo de heartbeat | Continuo | Detectar equipos POS sin agente o sin conectividad. | Equipos clasificados online/offline/stale con ultima comunicacion. |
| Revision de farmacias de turno | Diario y previo a ventana | Priorizar farmacias con impacto comercial/operativo alto. | Turnos sin alertas criticas o con incidentes escalados. |
| Revision de campanas activas | Diario y durante ventana | Controlar pilotos, TRX y despliegues generales. | Campanas en estado claro: planificada, corriendo, pausada, completada o fallida. |
| Gestion de alertas | Continuo | Reconocer, atender y cerrar alertas operativas. | Alertas con responsable y estado trazable. |
| Revision de eventos del agente | Segun incidente/campana | Diagnosticar actualizaciones, rollback, fallos de validacion o agente. | Causa tecnica identificada. |
| Control de versiones POS | Por release | Mantener versiones aprobadas y artefactos confiables. | Version aprobada, firmada, descargable y auditada. |
| Auditoria operativa | Diario/semanal | Verificar acciones sensibles. | Evidencia de quien hizo que, cuando y desde donde. |
| Inventario TI | Diario/semanal | Mantener visibilidad de equipos POS y activos relacionados. | Inventario actualizado por farmacia, equipo, version, agente y estado. |

## Indicadores

### Indicadores NOC

| Indicador | Descripcion | Criticidad |
| --------- | ----------- | ---------- |
| Farmacias criticas | Farmacias con al menos una alerta critica abierta. | Alta |
| Farmacias de turno en riesgo | Farmacias de turno con POS offline, alerta critica o campana pendiente/fallida. | Maxima |
| Equipos POS offline | Equipos sin heartbeat reciente o estado offline/stale. | Alta |
| Equipos POS sin version esperada | Equipos cuya version POS no coincide con la version objetivo. | Media/Alta |
| Campanas POS activas | Campanas en piloto, TRX, general, running o paused. | Media/Alta |
| Fallas por campana | Porcentaje de objetivos fallidos por campana. | Alta |
| Fallas por grupo TRX | Fallos concentrados por grupo operativo. | Alta |
| Rollbacks recientes | Rollbacks completados o fallidos en ventana reciente. | Alta |
| Alertas sin reconocer | Alertas abiertas sin operador responsable. | Alta |
| Cola offline del agente | Eventos pendientes/dead-letter no sincronizados. | Media/Alta |

### Indicadores de despliegue

| Indicador | Descripcion |
| --------- | ----------- |
| Avance por campana | Completados / total objetivos. |
| Avance por farmacia | Equipos actualizados por farmacia. |
| Avance por grupo TRX | Equipos actualizados por `trx001`, `trx002`, `trx003`. |
| Pendientes por farmacia | Equipos no actualizados o no contactados. |
| Fallos por version POS | Fallos agrupados por version objetivo. |
| Tiempo de cierre de campana | Tiempo desde inicio hasta completada/fallida. |
| Reintentos usados | Cantidad de objetivos reintentados. |
| Pausas automaticas/manuales | Control de salud de oleadas. |

### Indicadores de inventario

| Indicador | Descripcion |
| --------- | ----------- |
| Equipos por farmacia | Normal esperado: alrededor de 3 POS por farmacia. |
| Version POS instalada | Version reportada por equipo. |
| Version agente | Version del agente instalado. |
| Ruta POS | Debe apuntar a la ruta oficial del POS. |
| Disco disponible | Riesgo para descarga/update. |
| Proceso POS activo | Contexto para cierre/reapertura. |
| Latencia / perdida | Salud del enlace corporativo. |

## Operaciones criticas

| Operacion | Por que es critica | Control esperado |
| --------- | ------------------ | ---------------- |
| Actualizar POS | Impacta operacion de caja/farmacia. | Campana, ventana, piloto, TRX, auditoria, rollback. |
| Cerrar POS para actualizar | Puede interrumpir venta. | Avisos, ventana, hora forzada, evento auditado. |
| Reabrir POS | Recupera operacion despues de update/rollback. | Evento y validacion del ejecutable. |
| Rollback | Recupera farmacia/equipo ante fallo de version. | Evento, resultado, alerta critica, auditoria si es manual. |
| Atender farmacia de turno | Alta exposicion operativa. | Priorizacion, alerta elevada y seguimiento NOC. |
| Aprobar version POS | Autoriza artefacto que llegara a 1900 equipos. | Firma, checksum, rol autorizado, auditoria. |
| Pausar campana | Evita propagacion de fallo. | Regla por umbral, operador, motivo, auditoria. |
| Reanudar campana | Reactiva riesgo operacional. | Confirmacion, rol autorizado, estado sano. |
| Gestionar token/agente | Controla identidad del equipo POS. | Autenticacion tecnica y trazabilidad. |

## Flujos de soporte

### Incidente: farmacia reporta POS caido

1. Operador busca farmacia.
2. Revisa equipos POS de la farmacia.
3. Identifica equipo offline/stale o con proceso POS caido.
4. Consulta ultimo heartbeat, version POS, version agente y eventos recientes.
5. Revisa alertas abiertas asociadas.
6. Escala a soporte nivel 2 si hay fallo de actualizacion, rollback o cola offline.
7. Cierra alerta con evidencia.

Cobertura actual: parcial. Hay equipos, detalle, heartbeat, eventos y alertas. Falta vista fuerte por farmacia y diagnostico de cola offline en UI/backend.

### Incidente: actualizacion fallo en equipo POS

1. Alerta critica `UPDATE_FAILED` o `ROLLBACK_FAILED`.
2. Operador abre evento del agente.
3. Verifica campana, version anterior/nueva, error, metadatos.
4. Confirma si rollback automatico ocurrio.
5. Si farmacia esta de turno, eleva prioridad.
6. Decide pausar campana si se repite por version/TRX.

Cobertura actual: buena en agente/eventos/alertas; parcial en priorizacion por turno y decision por TRX.

### Incidente: enlace corporativo intermitente

1. NOC detecta varios POS offline en una misma farmacia/zona.
2. Verifica latencia/perdida reportada.
3. Agrupa impacto por farmacia/zona.
4. Escala a redes/proveedor.
5. Monitorea retorno de heartbeat.

Cobertura actual: parcial. Hay latencia/perdida por metrica, pero falta agregacion por farmacia/zona y flujo de incidente.

## Flujos de despliegue

### Flujo version POS

1. Cargar artefacto ZIP.
2. Validar checksum/firma.
3. Aprobar version POS.
4. Retirar version si no debe usarse.
5. Auditar acciones.

Cobertura actual: alta. Existe carga, aprobacion, retiro, firma/checksum y auditoria. La brecha principal era lenguaje, ya corregido visualmente en etapa 1.

### Flujo campana piloto

1. Crear campana POS con version aprobada.
2. Seleccionar equipos piloto.
3. Planificar oleada piloto.
4. Ejecutar en ventana.
5. Monitorear eventos/fallos.
6. Aprobar avance a TRX/general si piloto sano.

Cobertura actual: parcial/alta. Existen campanas, pilotos, oleadas, ventana y eventos. Falta criterio operacional explicito para pasar de piloto a TRX/general.

### Flujo campana por grupo TRX

1. Seleccionar grupo `trx001`, `trx002` o `trx003`.
2. Validar maximo 100 equipos por grupo.
3. Planificar oleada por grupo.
4. Ejecutar con limites de paralelismo.
5. Medir fallos por grupo.
6. Pausar si supera umbral.

Cobertura actual: parcial. Existe `targetGroup` y orquestacion, pero no existe `GrupoTrx` formal ni validacion de maximo 100.

### Flujo rollback

1. Agente detecta fallo despues de modificar POS.
2. Ejecuta rollback automatico desde backup.
3. Reporta eventos `ROLLBACK_STARTED`, `ROLLBACK_COMPLETED` o `ROLLBACK_FAILED`.
4. Backend registra resultado y alerta si corresponde.
5. NOC analiza impacto por farmacia/campana.

Cobertura actual: buena para rollback automatico por equipo. Falta rollback manual por equipo/campana como proceso operativo auditado.

## Flujos de monitoreo

| Flujo | Cobertura actual | Brecha |
| ----- | ---------------- | ------ |
| Heartbeat agente POS | Alta | Falta dashboard agregado por farmacia/turno. |
| Eventos del agente | Alta | Falta filtro operacional por farmacia/campana/TRX en UI. |
| Alertas operativas | Media/Alta | Falta priorizacion por farmacia de turno. |
| Salud de campana | Media/Alta | Falta estado por farmacia y TRX formal. |
| Salud de version POS | Media | Falta KPI de version objetivo vs instalada. |
| Salud de red corporativa | Parcial | Falta agregacion por farmacia/zona. |
| Cola offline agente | Parcial | Existe en agente, falta visibilidad central. |
| Dashboard NOC | Media | Existe dashboard, pero debe responder preguntas de operacion de farmacias. |

## Inventario TI

### Inventario minimo del Operations Center

| Elemento | Campos clave | Estado actual |
| -------- | ------------ | ------------- |
| Farmacia | codigo, nombre, ciudad, zona, direccion, turno, activa, estado operacional agregado | Cubierto en Fase A. |
| Equipo POS | farmacia, hostname, IP, MAC, Windows, version agente, version POS, ruta POS, estado, ultimo heartbeat | Cubierto. |
| Agente POS | version, token, heartbeat, eventos, cola offline | Parcial. Token/version/heartbeat cubierto; cola offline no centralizada. |
| Version POS | version, artefacto, checksum, firma, estado, aprobado por, fecha | Cubierto. |
| Campana POS | version, objetivos, estado, piloto, grupo, ventana, fallos | Cubierto parcialmente. |
| Grupo TRX | codigo, equipos, maximo 100, estado operativo | Falta como entidad formal. |
| Alerta | farmacia, equipo, severidad, tipo, estado, gestion | Cubierto parcialmente. |
| Auditoria | usuario, accion, entidad, valores, IP, fecha | Cubierto. |

## Auditoria

La auditoria debe responder: quien hizo que, sobre que farmacia/equipo/version/campana, cuando, desde donde y con que resultado.

### Acciones auditables clave

| Accion | Estado actual |
| ------ | ------------- |
| Login administrativo | Cubierto. |
| Carga de version POS | Cubierto como paquete. |
| Aprobacion de version POS | Cubierto como paquete. |
| Retiro de version POS | Cubierto como paquete. |
| Creacion de campana POS | Cubierto como despliegue. |
| Pausa/reanudacion/cancelacion de campana | Cubierto como despliegue. |
| Reconocimiento/cierre de alerta | Cubierto. |
| Cambio de rol/usuario | Cubierto. |
| Excepcion de farmacia de turno | Falta. |
| Rollback manual por equipo/campana | Falta. |
| Cambio de grupo TRX | Falta. |

Brecha principal: la auditoria existe tecnicamente, pero sus acciones deben renombrarse a lenguaje operativo.

## Gestion de incidentes

### Estados recomendados

| Estado | Descripcion |
| ------ | ----------- |
| Abierto | Alerta o incidente detectado. |
| Reconocido | Operador lo tomo. |
| En diagnostico | Se revisan eventos, heartbeat o campana. |
| Escalado | Requiere soporte nivel 2, redes o desarrollo POS. |
| Mitigado | Hay workaround, rollback o recuperacion parcial. |
| Cerrado | Causa documentada y servicio recuperado. |

Cobertura actual: alertas tienen OPEN/ACKNOWLEDGED/CLOSED. Falta modelo de incidente mas rico, pero no conviene crearlo antes de consolidar el dominio.

## Mapeo de procesos al sistema actual

| Proceso operativo | Cubierto hoy | Componentes actuales | Brecha |
| ----------------- | ------------ | -------------------- | ------ |
| Registrar equipo POS | Si | Agente, backend, equipos | Lenguaje publico ya corregido en aliases. |
| Reportar heartbeat | Si | Agente, metricas, equipo | Falta agregacion NOC por farmacia/turno. |
| Consultar inventario POS | Si | Equipos, farmacias | Falta vista de inventario TI mas completa. |
| Ver detalle de equipo POS | Si | Detalle equipo, eventos, metricas | Falta cola offline y diagnostico agente. |
| Cargar version POS | Si | Versiones/paquetes | Lenguaje ya reorientado en UI/API alias. |
| Aprobar version POS | Si | Versiones/paquetes, auditoria | Accion auditada aun usa nomenclatura package internamente. |
| Crear campana POS | Si | Despliegues/campanas | Falta tipo formal piloto/TRX/general. |
| Ejecutar piloto | Parcial | Piloto y oleadas | Falta decision operacional de promocion. |
| Ejecutar TRX | Parcial | targetGroup + Grupo TRX formal | Falta conectar campanas directamente a GrupoTrx formal. |
| Ejecutar campana general | Parcial | Campanas/objetivos | Falta separacion formal de tipo. |
| Controlar ventana 23:55-01:00 | Si | Campana, instruccion, agente | Debe visualizarse mejor en NOC. |
| Cerrar/reabrir POS | Si | Agente | Falta visibilidad ejecutiva por farmacia. |
| Rollback automatico | Si | Agente, eventos, alertas | Falta rollback manual operativo. |
| Alertar fallo update/rollback | Si | Alertas | Falta elevar por farmacia de turno. |
| Gestionar alertas | Si | Alertas, auditoria | Falta flujo de incidente completo. |
| Auditar acciones | Si | Audit logs | Falta lenguaje de negocio en acciones. |
| Monitorear cola offline | Parcial | Agente | Falta centralizacion y KPI. |
| Monitorear farmacias de turno | Parcial | `is_on_duty`, estado de campana por farmacia | Falta politica clara de excepcion/priorizacion. |
| Monitorear red corporativa | Parcial | Latencia/perdida por metrica | Falta agregacion por farmacia/zona. |

## Procesos ya cubiertos

- Registro de agente/equipo POS.
- Heartbeat.
- Inventario basico de farmacia/equipo.
- Version POS como paquete/artefacto.
- Aprobacion/retiro/descarga de artefacto.
- Campanas POS basadas en despliegues.
- Pilotos y oleadas.
- Ventana operativa.
- Eventos del agente.
- Idempotencia.
- Alertas por fallo.
- Backup y rollback automatico.
- Auditoria administrativa/operativa basica.
- Usuarios y roles.
- Dashboard inicial.

## Procesos faltantes o incompletos

- Gestion formal de Grupo TRX.
- Validacion maximo 100 equipos por grupo TRX.
- Estado de campana por farmacia.
- Maximo 1 descarga simultanea por farmacia.
- Politica definitiva de farmacia de turno.
- Priorizacion automatica de alertas por farmacia de turno.
- Vista NOC centrada en farmacias criticas.
- Diagnostico central de cola offline del agente.
- Rollback manual auditado por equipo.
- Rollback manual auditado por campana.
- Gestion de incidentes con estados mas ricos que alerta.
- Agregacion de salud de enlace por farmacia/zona.
- Auditoria con acciones 100% en lenguaje de negocio.
- Inventario TI ampliado mas alla del POS cuando el negocio lo pida.

## Modulos que deben existir

No todos deben implementarse ahora. Este es el mapa de capacidades para convertir el producto en un verdadero Operations Center.

| Modulo | Responsabilidad | Estado |
| ------ | --------------- | ------ |
| Dashboard NOC | Vista ejecutiva de continuidad operacional. | Existe parcial. |
| Farmacias | Unidad operacional principal, turno, estado, equipos, alertas. | Implementado en Fase A como vista operacional agregada. |
| Equipos POS | Inventario y salud de POS/agente. | Existe. |
| Versiones POS | Carga, firma, aprobacion, retiro y artefactos. | Existe con lenguaje nuevo. |
| Campanas POS | Pilotos, TRX, generales, oleadas, ventana y estado. | Existe parcial. |
| Grupos TRX | Catalogo, membresia, limite 100, salud por grupo. | Implementado en P0-001 como entidad formal y vista operativa. |
| Farmacias de Turno | Priorizacion operacional y excepciones. | Parcial/falta. |
| Alertas Operativas | Deteccion, reconocimiento, cierre, prioridad. | Existe parcial. |
| Eventos del Agente | Trazabilidad tecnica de agente POS. | Existe. |
| Monitoreo de Agentes | Heartbeat, cola offline, dead-letter, version agente. | Parcial. |
| Inventario TI | Activos por farmacia, POS, red, periféricos futuros. | Parcial. |
| Auditoria Operativa | Evidencia de decisiones y acciones sensibles. | Existe parcial. |
| Gestion de Incidentes | Ciclo de vida de incidentes y escalamiento. | Falta. |
| Usuarios de Operaciones | Roles y accesos. | Existe. |
| Configuracion Operativa | Politicas de ventana, umbrales, retencion. | Parcial. |

## Recomendacion critica

El sistema ya tiene buena base tecnica para actualizacion POS, agente, rollback, eventos y auditoria. Lo que todavia no lo convierte plenamente en un Operations Center de farmacias es la falta de agregacion operacional por farmacia, turno y grupo TRX.

La siguiente evolucion no debe ser "mas CRUD". Debe ser:

1. Farmacia como centro de lectura operacional.
2. Campana POS vista por farmacia y TRX.
3. Alertas priorizadas por impacto real.
4. Monitoreo de agente visible centralmente.
5. Incidentes y auditoria expresados en lenguaje de soporte.

El producto sera Farmamia Operations Center cuando el operador pueda responder en menos de un minuto:

- que farmacias estan en riesgo;
- que equipos POS estan afectando venta;
- que version POS esta causando problemas;
- que grupo TRX debe pausarse;
- que farmacia de turno necesita atencion inmediata;
- quien hizo la ultima accion critica.
