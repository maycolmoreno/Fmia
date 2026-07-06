# Monitoring - Grafana

## Responsabilidad

Visualizar metricas operativas y ejecutivas de equipos, farmacias, actualizaciones y red corporativa.

## Fuentes sugeridas

```txt
PostgreSQL
Spring Boot Actuator
Micrometer
Prometheus
```

## Artefactos incluidos

- `prometheus.yml`: scrape de `/actuator/prometheus` cada 15 segundos (target `host.docker.internal:8081`, el backend corre en el host, no en Docker).
- `alert_rules.yml`: reglas para API 5xx, alertas criticas abiertas, autopausas de despliegue, caida de heartbeats y conexiones HikariCP pendientes. Verificado (2026-07-05) que las 10 metricas custom que usan estas reglas y los dashboards existen realmente en el backend (`MetricasOperativasFarmamia`, `EvaluadorOrquestacionProgramado`, `RepositorioOrquestacionDesplieguesJpaAdaptador`).
- `dashboards/noc-overview.json`, `dashboards/red-farmacias.json`, `dashboards/farmacia-enlace.json`: los 3 dashboards realmente provisionados (ver `provisioning/dashboards/dashboards.yml`, monta `monitoring/dashboards/`). Los dos ultimos usan el datasource Postgres (`farmamia-postgres`) con SQL directo contra `device_metrics`/`devices`/`alerts`/`branches` — columnas verificadas contra las migraciones Flyway.

**Pendiente conocido:** no hay Alertmanager (ni "unified alerting" de Grafana) configurado — las reglas de `alert_rules.yml` se evaluan y aparecen como "firing" en la UI de Prometheus, pero no se envia ninguna notificacion externa (email/Slack/webhook) hasta que se defina un canal real.

## Runbook operativo

Aun no existe un runbook operativo escrito para este proyecto. Si se crea, debe vivir dentro de este repositorio (p.ej. `docs/RUNBOOK_OPERACIONES_NOC.md`), no en una carpeta externa al monorepo.
