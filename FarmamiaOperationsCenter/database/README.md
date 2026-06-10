# Database - PostgreSQL

## Responsabilidad

Almacenar sucursales, equipos, paquetes POS, campanas, eventos, auditoria, metricas, alertas, activos y gestion de cambios.

## Migracion inicial

El esquema base inicial ya esta documentado en:

```txt
Documentacion/FARMAMIA_OPERATIONS_CENTER_SCHEMA_POSTGRESQL.sql
```

Para Spring Boot se recomienda moverlo luego a:

```txt
backend-api/src/main/resources/db/migration/V1__farmamia_ops_schema.sql
```

## Tablas MVP

```txt
branches
devices
agent_tokens
app_users
pos_packages
deployments
deployment_targets
update_events
device_metrics
alerts
audit_logs
```

## Cobertura funcional

```txt
Sucursales y equipos POS
Tokens tecnicos del agente
Usuarios administrativos
Paquetes ZIP del POS
Campanas de actualizacion
Objetivos por equipo
Eventos de actualizacion
Metricas operativas
Alertas
Auditoria
```

## Recomendaciones

- Usar Flyway.
- Indexar campos de busqueda frecuente.
- Evaluar particionamiento futuro para `device_metrics`, `update_events` y `audit_logs`.
- Configurar backups diarios y retencion.
