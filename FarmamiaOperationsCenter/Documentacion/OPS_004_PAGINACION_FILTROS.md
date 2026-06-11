# OPS-004 - Paginacion y Filtros Globales

## Diagnostico

Endpoints que todavia requieren paginacion server-side completa:

- `GET /api/branches`

Endpoints legacy que siguen disponibles por compatibilidad, pero ya tienen alternativa paginada:

- `GET /api/update-events`
- `GET /api/alerts`
- `GET /api/audit-logs`
- `GET /api/packages`
- `GET /api/deployments`

Endpoints paginados ya agregados sin romper compatibilidad:

- `GET /api/devices/page`
- `GET /api/admin/users/page`
- `GET /api/packages/page`
- `GET /api/deployments/page`
- `GET /api/update-events/page`
- `GET /api/alerts/page`
- `GET /api/audit-logs/page`

## Contrato Estandar

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

## Regla de Implementacion

No exponer repositorios JPA desde controladores. La paginacion debe pasar por:

```text
Controlador -> Caso de uso -> Puerto -> Adaptador JPA
```

## Orden Recomendado

1. Crear DTO comun `RespuestaPagina<T>`. Completado.
2. Crear modelo dominio `Pagina<T>`. Completado.
3. Migrar `devices` y `admin/users` primero. Completado como endpoints `/page`.
4. Migrar `packages` y `deployments`. Completado.
5. Estandarizar `alerts`, `audit-logs` y `update-events`. Completado.
6. Migrar `branches` a paginacion cuando el catalogo de sucursales crezca o se use para NOC.
7. Actualizar Angular a server-side pagination. Iniciado para cargas principales y pantallas de alertas/auditoria.

## Filtros Implementados

### Equipos

`GET /api/devices/page`

- `q`
- `status`
- `branchCode`
- `posVersion`
- `agentVersion`
- `lastHeartbeatFrom`
- `lastHeartbeatTo`
- `page`
- `size`
- `sort`

### Usuarios

`GET /api/admin/users/page`

- `q`
- `role`
- `active`
- `locked`
- `page`
- `size`
- `sort`

### Paquetes POS

`GET /api/packages/page`

- `q`
- `status`
- `version`
- `uploadedFrom`
- `uploadedTo`
- `page`
- `size`
- `sort`

### Despliegues

`GET /api/deployments/page`

- `q`
- `status`
- `packageVersion`
- `createdFrom`
- `createdTo`
- `page`
- `size`
- `sort`

### Eventos

`GET /api/update-events/page`

- `deviceId`
- `deploymentId`
- `eventType`
- `from`
- `to`
- `page`
- `size`
- `sort`

### Alertas

`GET /api/alerts/page`

- `status`
- `severity`
- `type`
- `deviceId`
- `branchId`
- `branchCode`
- `hostname`
- `dateFrom`
- `dateTo`
- `page`
- `size`
- `sort`

### Auditoria

`GET /api/audit-logs/page`

- `action`
- `entityType`
- `actorUsername`
- `from`
- `to`
- `page`
- `size`
- `sort`

## Verificacion

`.\mvnw.cmd test` ejecutado el 2026-06-10:

- 27 pruebas ejecutadas.
- 0 fallos.
- 0 errores.
- 6 omitidas porque Docker/Testcontainers no estaba disponible localmente.

Angular quedo apuntando a endpoints `/page` para equipos, paquetes, despliegues, eventos,
alertas, auditoria y usuarios administrativos. En esta sesion no se pudo ejecutar
`npm run build` porque `node`/`npm` no estaban disponibles en el PATH del proceso.

## Indices Agregados

La migracion `V7` agrega indices iniciales:

```sql
idx_devices_status_branch
idx_deployment_targets_deployment_status
idx_deployment_targets_device_status
idx_update_events_deployment_created
idx_alerts_device_status_opened
idx_audit_logs_actor_created
```
