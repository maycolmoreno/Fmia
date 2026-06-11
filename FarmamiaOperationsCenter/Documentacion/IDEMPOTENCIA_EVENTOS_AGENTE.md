# Idempotencia de Eventos del Agente

## Objetivo

Evitar duplicados cuando el agente reenvia eventos o resultados despues de cortes de red, reinicios o errores temporales del backend.

## Endpoints Cubiertos

- `POST /api/agent/{deviceId}/events`
- `POST /api/agent/{deviceId}/update-result`

## Contrato

Ambos endpoints aceptan `idempotencyKey`.

Si la key no llega, el backend mantiene el comportamiento historico.

Si la key llega:

- primer request: procesa y persiste;
- mismo request repetido: responde exitosamente sin duplicar efectos;
- misma key con payload diferente: responde `409 IDEMPOTENCY_CONFLICT`.

## Persistencia

Migracion:

- `V7__idempotencia_eventos_indices_operativos.sql`

Cambios:

- `update_events.idempotency_key`
- indice unico parcial `device_id + idempotency_key`
- indices operativos para filtros y despliegues.

## Efectos Protegidos

Para resultados repetidos:

- no duplica `update_events`;
- no registra dos veces el resultado del target;
- no actualiza dos veces `posVersion`;
- no duplica alertas criticas.

## Pruebas

`RegistrarEventoAgenteCasoUsoTest` cubre:

- resultado fallido genera evento y alerta;
- resultado exitoso actualiza version;
- resultado duplicado con misma key no duplica efectos;
- misma key con payload distinto genera conflicto.

