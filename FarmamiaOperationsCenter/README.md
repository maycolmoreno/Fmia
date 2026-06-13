# Farmamia Operations Center

Plataforma on-premise para despliegue centralizado de actualizaciones POS, monitoreo operativo, inventario, auditoria y gestion TI de farmacias Farmamia.

## Stack

```txt
Backend: Java Spring Boot
Agente Windows: .NET 8 Worker Service
Base de datos: PostgreSQL
Panel web: Angular
Monitoreo: Grafana
Comunicacion: HTTPS interno + API REST
```

## Componentes

```txt
backend-api/
  API Spring Boot central.

windows-agent/
  Servicio Windows .NET 8 para cada equipo POS. Base Worker Service creada con registro y heartbeat.

admin-panel/
  Panel administrativo Angular.

database/
  Migraciones, modelo de datos y scripts PostgreSQL.

monitoring/
  Dashboards, metricas y configuracion Grafana.

contracts/
  Contratos REST, estados y eventos.
```

## Lenguaje del Producto

Farmamia Operations Center usa lenguaje de operacion de farmacias:

```txt
Farmacia
Equipo POS
Version POS
Campana POS
Agente POS
Evento del agente
Alerta operativa
Usuario de operaciones
```

Los endpoints de negocio preferidos son `/api/farmacias`, `/api/equipos-pos`, `/api/versiones-pos`, `/api/campanas-pos` y `/api/eventos-agente`. Los endpoints legacy `/api/branches`, `/api/devices`, `/api/packages`, `/api/deployments` y `/api/update-events` siguen activos por compatibilidad temporal.

## Objetivo del MVP

Actualizar el POS portable de forma centralizada, segura y auditada:

```txt
C:\Program Files (x86)\Farmamia Cia Ltda - Elipsys\Cliente
```

Ejecutable:

```txt
Zabyca.Pos.Desktop.exe
```

## Orden de Construccion

```txt
1. Backend API: registro de agente, heartbeat, paquetes, campanas y eventos.
2. PostgreSQL: migraciones iniciales.
3. Agente Windows: registro, heartbeat, descarga, backup, update y rollback.
4. Panel Angular: dashboard basico, paquetes, equipos y campanas.
5. Grafana: metricas operativas basicas.
```

## Arquitectura

Todo el proyecto debe construirse con:

```txt
Arquitectura limpia
Arquitectura hexagonal
Principios SOLID
```

Regla general:

```txt
El nucleo de negocio no depende de frameworks ni detalles tecnicos.
Los casos de uso orquestan reglas de negocio.
Los adaptadores de entrada traducen solicitudes externas hacia casos de uso.
Los adaptadores de salida implementan persistencia, archivos, red y monitoreo.
Las dependencias apuntan hacia aplicacion/dominio.
```

## Documentacion Tecnica

```txt
Documentacion/SPRINT_OPERATIONS_CENTER_PILOTO.md
Documentacion/RUNBOOK_OPERACIONES_NOC.md
Documentacion/OPS_002_COLA_DURABLE_AGENTE.md
Documentacion/OPS_004_PAGINACION_FILTROS.md
Documentacion/IDEMPOTENCIA_EVENTOS_AGENTE.md
Documentacion/DIAGNOSTICO_REORIENTACION_FARMAMIA_OPERATIONS_CENTER.md
Documentacion/PLAN_REFACTORIZACION_DOMINIO_FARMAMIA_OPERATIONS_CENTER.md
```

## Verificacion MVP

La linea base completa se valida con un unico script:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\herramientas\verificacion\verificar-mvp.ps1
```

Ese script valida estructura, compila Angular, ejecuta pruebas backend con Maven Wrapper, ejecuta pruebas del agente .NET y confirma que existan los scripts demo/E2E.

Para levantar un stack local completo:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\herramientas\verificacion\levantar-stack-mvp-local.ps1 -PostgresLocal
powershell -NoProfile -ExecutionPolicy Bypass -File .\herramientas\verificacion\ejecutar-e2e-demo-local.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\herramientas\verificacion\detener-stack-mvp-local.ps1
```

El modo recomendado de desarrollo usa PostgreSQL local en `localhost:5432` con `postgres/.r4e3w2q1`.
Si quieres usar Docker para PostgreSQL, omite `-PostgresLocal`.

La verificacion automatica de CI vive en:

```txt
.github/workflows/mvp-verification.yml
```

Backend sin Maven instalado:

```powershell
cd backend-api
.\mvnw.cmd test
```

Linux/macOS:

```bash
cd backend-api
./mvnw test
```

Las pruebas de integracion backend usan Testcontainers. Si Docker esta disponible, se ejecutan; si Docker no esta disponible, se omiten explicitamente por `@Testcontainers(disabledWithoutDocker = true)`.

## QA/PROD

Antes de usar ambientes no locales, revisar:

```txt
Documentacion/CONFIGURACION_QA_PROD.md
```

Reglas principales:

```txt
No usar admin/admin123 fuera de desarrollo.
No usar dev-secret-change-me en QA/PROD.
Configurar FARMAMIA_JWT_SECRET fuerte.
Configurar FARMAMIA_SEED_DEMO_ADMIN=false.
Configurar base de datos y repositorio ZIP por variables de entorno.
Preparar HTTPS interno con keystore PKCS12.
```

## Artefactos no versionables

No subir al repositorio:

```txt
node_modules/
dist/
target/
bin/
obj/
.vs/
.idea/
*.user
*.log
data/
logs/
```
