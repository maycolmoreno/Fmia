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
- diagnostico de cola por estado (`PENDING`, `SENDING`, `SENT`, `FAILED`, `DEAD_LETTER`);
- log periodico de salud de cola desde el despachador;
- alerta operativa en log cuando existen eventos `DEAD_LETTER`;
- pruebas unitarias con carpeta temporal.
- prueba con backend HTTP falso para verificar despacho end-to-end.

Pendiente para cierre completo:

- validar en host Windows con runtime .NET 8 instalado;
- decidir si la alerta `DEAD_LETTER` tambien debe escribirse como aviso local visible para soporte en carpeta de avisos.

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
- diagnostico de eventos `DEAD_LETTER`;
- encolado por decorador y despacho a backend HTTP falso.

Verificacion local:

- `dotnet build FarmamiaOperationsCenter\windows-agent\Farmamia.Agent.Tests\Farmamia.Agent.Tests.csproj`
- Compilacion correcta, 0 advertencias, 0 errores.

Limitacion de ambiente:

- `dotnet test` no pudo ejecutar porque el host actual no tiene runtime `Microsoft.NETCore.App 8.0.0`; solo se detecto runtime 10.0.8. El proyecto compila correctamente contra `net8.0`.

## Riesgo Pendiente

La cola ya es el camino principal para eventos/resultados y el despachador emite alerta en log cuando aparecen eventos `DEAD_LETTER`. El riesgo restante es operacional: definir si soporte necesita un aviso local adicional fuera del log del servicio.
