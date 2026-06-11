# OPS-002 - Cola Durable del Agente

## Decision

Se adopta SQLite como almacenamiento local durable del patron Outbox del agente Windows.

Ruta objetivo:

```txt
C:\Program Files (x86)\Farmamia Cia Ltda - Elipsys\Agent\State\agent-queue.db
```

## Estado Implementado

Implementado:

- paquete `Microsoft.Data.Sqlite`;
- puerto `IColaEventosAgente`;
- modelo `EventoPendienteAgente`;
- implementacion `ColaEventosAgenteSqlite`;
- decorador `ClienteOperacionesFarmamiaConCola`;
- despachador `DespachadorColaEventosServicio`;
- tabla `outbox_events`;
- tabla `agent_state`;
- recuperacion de eventos `SENDING` a `PENDING`;
- checksum SHA-256 del payload;
- estados `PENDING`, `SENDING`, `SENT`, `FAILED`, `DEAD_LETTER`;
- inicializacion de cola al arrancar `ServicioAgente`;
- eventos y resultados primero entran a la cola;
- reenvio automatico al backend usando credenciales locales;
- backoff exponencial con jitter desde el despachador;
- pruebas unitarias con carpeta temporal.

Pendiente para cierre completo:

- agregar alerta local/operativa visible para eventos en `DEAD_LETTER`;
- exponer diagnostico de cola en logs/health del agente;
- agregar prueba con backend HTTP falso para verificar despacho end-to-end.

## Tabla `outbox_events`

Campos:

- `id`
- `event_type`
- `payload_json`
- `idempotency_key`
- `status`
- `attempt_count`
- `next_attempt_at`
- `created_at`
- `updated_at`
- `last_error`
- `checksum`

## Pruebas

`ColaEventosAgenteSqliteTests` cubre:

- insercion de evento pendiente;
- recuperacion de `SENDING` a `PENDING`;
- paso a `DEAD_LETTER` tras maximo de intentos.

## Riesgo Pendiente

La cola ya es el camino principal para eventos/resultados. El riesgo restante es operacional: los eventos en `DEAD_LETTER` deben quedar visibles para soporte, no solo en SQLite/logs.
