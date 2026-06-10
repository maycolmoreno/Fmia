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
Documentacion/FARMAMIA_OPERATIONS_CENTER_PLAN_FASES.md
Documentacion/FARMAMIA_OPERATIONS_CENTER_SCHEMA_POSTGRESQL.sql
Documentacion/FARMAMIA_OPERATIONS_CENTER_BACKLOG_TECNICO_MVP.md
Documentacion/ESTADO_MVP.md
Documentacion/GUIA_VERIFICACION_MVP.md
Documentacion/CONFIGURACION_QA_PROD.md
```

## Verificacion MVP

La linea base completa se valida con un unico script:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\herramientas\verificacion\verificar-mvp.ps1
```

Ese script valida estructura, compila Angular, ejecuta pruebas backend con Maven Wrapper, ejecuta pruebas del agente .NET y confirma que existan los scripts demo/E2E.

Para levantar un stack local completo:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\herramientas\verificacion\levantar-stack-mvp-local.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\herramientas\verificacion\ejecutar-e2e-demo-local.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\herramientas\verificacion\detener-stack-mvp-local.ps1
```

Requiere Docker Desktop activo para PostgreSQL.

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
