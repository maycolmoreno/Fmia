# P0-004 - Campanas POS y Grupo TRX Farmacia First

## Validacion arquitectonica

P0-004 formaliza la relacion entre Campana POS y Grupo TRX sin convertir TRX en la unidad principal.

Principio aplicado:

```txt
Grupo TRX no es la unidad operacional principal.
Grupo TRX es un mecanismo de control de oleadas.
La unidad de impacto siempre es la Farmacia.
```

La lectura NOC correcta es:

```txt
Farmacia en riesgo
Campana POS
Grupo TRX
Equipo POS
```

## Implementacion

Capacidades activas:

- Asociacion formal Campana POS - Grupo TRX.
- Estado de campana por Grupo TRX con agregacion obligatoria por farmacia.
- `targetGroup` queda como fallback legacy cuando el objetivo no tiene `grupoTrxId`.
- Pausa de Grupo TRX dentro de campana bloquea nuevas instrucciones del agente para objetivos pendientes de ese grupo.
- Auditoria operacional para asociar, quitar, pausar, reanudar y advertir impacto en farmacias de turno.
- Vista Angular `Estado por Grupo TRX`, mostrando farmacias antes que equipos POS.
- Dashboard NOC redisenado con lectura Farmacia First: turno en riesgo, farmacias criticas, campanas POS, Grupos TRX y detalle tecnico al final.

## Modelo operacional

Concepto:

```txt
CampanaGrupoTrx
```

Representa el estado de un Grupo TRX dentro de una Campana POS.

Estados:

```txt
PENDIENTE
EN_EJECUCION
EN_RIESGO
PAUSADO
BLOQUEADO
COMPLETADO
COMPLETADO_CON_FALLOS
FALLIDO
```

## KPIs obligatorios por TRX

Todo KPI de Grupo TRX debe incluir dimension farmacia:

| KPI | Motivo |
| --- | ------ |
| `totalFarmacias` | Dimensiona alcance operativo real. |
| `farmaciasAfectadas` | Indica impacto NOC. |
| `farmaciasTurnoAfectadas` | Prioridad maxima. |
| `farmaciasCriticas` | Determina urgencia de pausa o escalamiento. |
| `farmaciasPendientes` | Muestra atraso por farmacia. |
| `farmaciasConFallos` | Mide problema por farmacia, no solo por POS. |
| `equiposPosFallidos` | Explicacion tecnica secundaria. |
| `rollbacks` | Riesgo operativo aunque el rollback haya completado. |

## Dashboard NOC Farmacia First

El Dashboard NOC queda ordenado para que el operador piense primero en farmacias y despues en TRX o equipos:

1. Farmacias de turno en riesgo.
2. KPIs grandes por severidad operacional.
3. Farmacias criticas.
4. Campanas POS activas.
5. Grupos TRX.
6. Detalle tecnico de alertas y eventos del agente.

Regla aplicada: Equipos POS no aparece como vista principal del dashboard. El POS se muestra como causa tecnica dentro de una farmacia, alerta, campana o evento.

## Reglas implementadas

| Regla | Estado |
| ----- | ------ |
| Una Campana POS puede asociarse a uno o varios Grupos TRX formales. | Implementado |
| Grupo TRX `PAUSADO` o `RETIRADO` no puede asociarse a campana nueva. | Implementado |
| `grupoTrxId` es fuente principal; `targetGroup` solo fallback. | Implementado |
| Toda lectura por TRX incluye farmacias afectadas, turno y criticas. | Implementado |
| Pausar TRX dentro de campana impide nuevas instrucciones. | Implementado |
| Rollback completado cuenta como riesgo. | Implementado |
| Rollback fallido marca riesgo/fallo. | Implementado |

## Endpoints

```http
GET    /api/campanas-pos/{id}/estado-por-trx
POST   /api/campanas-pos/{id}/grupos-trx/{grupoTrxId}
DELETE /api/campanas-pos/{id}/grupos-trx/{grupoTrxId}
POST   /api/campanas-pos/{id}/grupos-trx/{grupoTrxId}/pausar
POST   /api/campanas-pos/{id}/grupos-trx/{grupoTrxId}/reanudar
```

## Auditoria

Acciones:

```txt
ASOCIAR_GRUPO_TRX_CAMPANA
QUITAR_GRUPO_TRX_CAMPANA
PAUSAR_GRUPO_TRX_CAMPANA
REANUDAR_GRUPO_TRX_CAMPANA
ADVERTENCIA_TRX_CON_FARMACIA_TURNO
```

Datos auditados:

- usuario;
- fecha;
- IP;
- campanaId;
- grupoTrxId;
- farmaciasAfectadas;
- farmaciasTurnoAfectadas;
- farmaciasCriticas;
- motivo;
- estado anterior;
- estado nuevo.

## Fuera de alcance

No se implemento:

- motor generico de oleadas;
- workflow de aprobacion;
- simulador;
- optimizador automatico;
- multiempresa;
- ITSM;
- dashboard ejecutivo;
- calendario avanzado de turnos;
- IA.

## Criterio de exito

El NOC puede responder:

- que farmacias estan afectadas por `trx001`;
- que farmacias de turno estan en riesgo dentro de `trx001`;
- que Campana POS esta generando fallos;
- que Grupo TRX debe pausarse;
- que Version POS esta generando problemas;
- que equipos POS explican tecnicamente el riesgo.
