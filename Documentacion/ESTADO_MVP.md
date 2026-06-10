# Estado MVP - Farmamia Operations Center

Fecha de corte: 2026-06-10.

## Resumen

El MVP tiene una base funcional verificable para operar actualizaciones POS centralizadas:

- Backend Spring Boot con registro de agente, heartbeat, paquetes, despliegues, eventos, alertas, auditoria, seguridad administrativa y migraciones Flyway.
- Agente Windows .NET 8 con registro, heartbeat, consulta de instrucciones, descarga, checksum, backup, actualizacion, rollback y reporte de eventos.
- Panel Angular con login, dashboard, equipos, paquetes, despliegues, alertas, auditoria y cambio de contrasena.
- Scripts demo/E2E para escenarios exitosos y fallidos.
- Stack local automatizado para PostgreSQL, backend, panel y ejecucion E2E demo.
- Workflow CI agregado para backend, panel, agente y scripts PowerShell.
- Panel desacoplado de `localhost` mediante environments Angular.
- CORS de API configurable por `FARMAMIA_CORS_ALLOWED_ORIGINS`.
- Descarga de paquetes desde panel usando HttpClient autenticado.

## Evidencia actual

- Panel Angular: `npm run build` correcto.
- Panel Angular production: `npx ng build --configuration production` correcto.
- Agente Windows: `DOTNET_ROLL_FORWARD=Major dotnet test` correcto, 12/12 pruebas pasan.
- Backend: Maven Wrapper agregado en `backend-api/mvnw.cmd` y `backend-api/mvnw`.
- Backend verificado con wrapper: 21 pruebas, 0 fallos, 0 errores, 6 omitidas por Docker/Testcontainers no disponible.

## Backend

Implementado:

- `POST /api/agent/register`
- `POST /api/agent/heartbeat`
- `GET /api/agent/{deviceId}/instructions`
- `POST /api/agent/{deviceId}/events`
- `POST /api/agent/{deviceId}/update-result`
- Gestion administrativa de paquetes, despliegues, usuarios, alertas, auditoria, sucursales y equipos.
- Token tecnico de agente almacenado como hash.
- JWT administrativo con validacion estricta de secreto en perfiles `qa`, `prod` y `production`.
- Semilla demo `admin/admin123` controlada por `FARMAMIA_SEED_DEMO_ADMIN`.

Pendiente/riesgo:

- Ejecutar integracion completa con Docker activo en una maquina con acceso a imagen `postgres:16-alpine`.
- Ejecutar `levantar-stack-mvp-local.ps1` y `ejecutar-e2e-demo-local.ps1` cuando Docker Desktop este activo.

## Agente Windows

Implementado:

- Worker Service.
- Persistencia local de credenciales y estado.
- Descarga autorizada.
- Validacion SHA-256.
- Backup antes de modificar POS.
- Rollback ante fallo posterior a cambios.
- Reapertura de POS al finalizar/restaurar.
- Herramienta manual `FarmamiaUpdater`.

## Panel Angular

Implementado:

- Build productivo correcto.
- Login y rutas protegidas.
- Vistas operativas principales del MVP.

Pendiente:

- No hay pruebas Angular `*.spec.ts`; el verificador las omite explicitamente si no existen.

## Base de datos

La documentacion canonica del esquema esta en:

```txt
Documentacion/FARMAMIA_OPERATIONS_CENTER_SCHEMA_POSTGRESQL.sql
```

Las migraciones ejecutables estan en:

```txt
FarmamiaOperationsCenter/backend-api/src/main/resources/db/migration
```

El esquema documentado refleja el estado acumulado V1-V5.
