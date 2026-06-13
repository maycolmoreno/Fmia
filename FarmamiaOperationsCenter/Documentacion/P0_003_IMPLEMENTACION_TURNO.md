# P0-003 - Implementacion operativa de Farmacias de Turno

## Validacion arquitectonica

La funcionalidad ayuda directamente al NOC a identificar primero una farmacia de turno en riesgo.

La implementacion se mantiene Farmamia-first:

- La unidad principal sigue siendo la farmacia.
- El riesgo se calcula por impacto en Equipos POS, Campanas POS, Grupo TRX y Alertas Operativas.
- No se crearon tablas, calendarios, workflows, motores de reglas ni excepciones complejas.
- Se usa unicamente `farmacia.deTurno` como senal operativa inicial.

## Alcance implementado

### Estado operacional

El estado operacional por farmacia ahora eleva prioridad cuando `deTurno = true`:

| Condicion | Resultado |
| --------- | --------- |
| POS offline en farmacia de turno | `CRITICA` |
| Heartbeat vencido en farmacia de turno | `CRITICA` |
| `UPDATE_FAILED` o `ROLLBACK_FAILED` | `CRITICA` |
| `ROLLBACK_COMPLETED` | `EN_RIESGO` |
| Campana pendiente fuera de ventana | `CRITICA` |
| Grupo TRX pausado con campana pendiente | `EN_RIESGO` |

El endpoint de estado operacional agrega lectura NOC:

- `campanaActivaPrincipal`
- `grupoTrxPrincipal`
- `resumenRiesgo`

### Alertas

Se mantiene la estructura actual de alertas. Solo se eleva severidad cuando el equipo pertenece a una farmacia de turno:

| Tipo alerta | Severidad en farmacia de turno |
| ----------- | ------------------------------ |
| `UPDATE_FAILED` | `CRITICAL` |
| `ROLLBACK_FAILED` | `CRITICAL` |
| `POS_OFFLINE` / `DEVICE_OFFLINE` | `CRITICAL` |
| `HEARTBEAT_STALE` | `CRITICAL` |
| `CAMPANA_PENDIENTE_FUERA_VENTANA` | `CRITICAL` |
| `ROLLBACK_COMPLETED` | `HIGH` |

`ROLLBACK_COMPLETED` genera una alerta de verificacion operacional para que soporte confirme que el POS quedo operativo despues del rollback.

### Dashboard NOC

Se agrega el bloque superior:

```txt
Farmacias de turno en riesgo
```

Muestra:

- codigo farmacia;
- nombre;
- ciudad;
- estado operacional;
- POS offline;
- alertas criticas;
- campana activa;
- grupo TRX;
- ultimo heartbeat;
- resumen de riesgo.

El orden prioriza:

1. `CRITICA`
2. `EN_RIESGO`
3. `NORMAL`

### Campanas POS

Al crear una campana, si los objetivos incluyen farmacias de turno:

- Angular muestra advertencia visible no bloqueante.
- Backend registra auditoria `ADVERTENCIA_CAMPANA_CON_TURNO`.

No se bloquea la campana y no se crea flujo de aprobacion todavia.

## Fuera de alcance respetado

No se implemento:

- tabla `FarmaciaTurno`;
- calendario de turnos;
- excepciones avanzadas;
- workflow engine;
- scheduler;
- ITSM;
- multiempresa;
- IA.

## Riesgos conocidos

1. `deTurno` es booleano; no expresa vigencia, tipo de turno ni motivo.
2. El umbral inicial de heartbeat vencido para turno queda fijo en 15 minutos.
3. La advertencia Angular solo puede calcular farmacias de turno para equipos cargados en la pagina actual; backend audita el conteo real despues de crear la campana.
4. Las campanas con farmacias de turno todavia no se bloquean ni requieren aprobacion explicita.
5. `ROLLBACK_COMPLETED` como alerta `HIGH` exige disciplina NOC para verificar cierre operativo.

## Criterio de exito

El operador NOC debe abrir el dashboard y en menos de 10 segundos identificar:

- que farmacia de turno esta en riesgo;
- cuantos POS offline tiene;
- que campana POS esta relacionada;
- que Grupo TRX participa;
- cual es el ultimo heartbeat;
- que alerta requiere atencion inmediata.
