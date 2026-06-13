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

- `prometheus.yml`: scrape de `/actuator/prometheus` cada 15 segundos.
- `alert_rules.yml`: reglas iniciales para API 5xx, alertas criticas, autopausas, heartbeats y DB.
- `grafana-noc-overview.json`: dashboard inicial NOC Overview.

## Runbook operativo

Ante alertas o degradacion sostenida, usar:

```txt
../Documentacion/RUNBOOK_OPERACIONES_NOC.md
```

## Dashboards iniciales

```txt
Estado de actualizacion POS
Equipos offline
Farmacias offline
Farmacias de turno
Latencia por sucursal
Disco critico
Fallos de actualizacion
Rollbacks
```
