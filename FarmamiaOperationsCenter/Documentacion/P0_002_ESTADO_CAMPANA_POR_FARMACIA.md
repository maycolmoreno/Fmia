# P0-002 - Estado de Campana por Farmacia

## Objetivo

Implementar una lectura operacional para que el NOC vea el avance real de una campana POS por farmacia.

La pregunta principal deja de ser cuantos objetivos tecnicos completaron y pasa a ser:

- que farmacias terminaron;
- que farmacias tienen errores;
- que farmacias estan pendientes;
- que farmacias de turno estan en riesgo;
- que grupo TRX concentra mayor riesgo.

## Alcance implementado

- Modelo de lectura `EstadoCampanaFarmacia`.
- Separacion entre estado tecnico y estado operacional.
- Endpoint:

```http
GET /api/campanas-pos/{id}/estado-por-farmacia
```

- Filtros:
  - `estadoTecnico`
  - `estadoOperacional`
  - `grupoTrx`
  - `deTurno`
  - `q`
  - `page`
  - `size`
  - `sort`
- Vista Angular dentro de Campanas POS.
- KPIs NOC de campana por farmacia.
- Detalle de equipos POS por farmacia dentro de la campana.

## Estados tecnicos

| Estado | Regla |
| ------ | ----- |
| `PENDIENTE` | Todos los equipos POS objetivo estan pendientes o sin inicio operativo. |
| `EN_PROGRESO` | Al menos un equipo inicio o completo y todavia quedan pendientes. |
| `COMPLETADA` | Todos los equipos completaron correctamente, sin fallos ni rollback. |
| `COMPLETADA_CON_FALLOS` | No quedan pendientes, pero hubo rollback exitoso o fallos recuperados. |
| `FALLIDA` | Hay fallos definitivos, rollback fallido o ningun POS recuperado. |

## Estados operacionales

| Estado | Regla |
| ------ | ----- |
| `NORMAL` | Sin alerta critica, sin turno en riesgo y sin POS offline critico. |
| `EN_RIESGO` | Rollback, alerta abierta, POS offline, grupo TRX pausado o farmacia de turno pendiente. |
| `CRITICA` | Alerta critica, farmacia de turno con POS offline, rollback fallido o fallo operativo. |

El estado operacional tiene prioridad visual sobre el estado tecnico.

## Fuente de datos

La lectura se calcula desde datos existentes:

- `deployments`
- `deployment_targets`
- `devices`
- `branches`
- `alerts`
- `grupos_trx`

No se crearon tablas nuevas.

## Fuera de alcance

- Tablas de resumen.
- Dashboard ejecutivo.
- Incidentes ITSM.
- Inventario TI.
- Motor generico de workflows.
- Simulador.
- IA.
- Multiempresa.
