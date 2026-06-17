# Backend API - Java Spring Boot

## Responsabilidad

API central para orquestar actualizaciones, agentes, paquetes, campanas, auditoria, monitoreo y administracion TI.

## Estado actual

Base inicial creada para el MVP:

```txt
Spring Boot 3.3.x
Java 21
Maven
Spring Web
Spring Security
Spring Data JPA
Flyway
PostgreSQL
Actuator
OpenAPI
```

Funcionalidad implementada:

```txt
Registro de agente con alta/actualizacion de equipo.
Alta automatica de sucursal cuando no existe catalogo previo.
Emision de token tecnico y almacenamiento como hash SHA-256.
Renovacion de token activo por equipo.
Validacion de Bearer token contra hash activo en base de datos.
Registro de ultimo uso del token valido.
Autenticacion administrativa con login y JWT.
Heartbeat con actualizacion de estado ONLINE.
Persistencia de metricas operativas por heartbeat.
Consulta de instrucciones `UPDATE_POS` para objetivos autorizados.
Registro de eventos de actualizacion enviados por el agente.
Registro de resultado final de actualizacion.
Actualizacion de estado del objetivo de despliegue.
Actualizacion de version POS del equipo cuando el resultado es COMPLETED.
Generacion de alerta critica ante fallo de actualizacion o rollback.
Consulta administrativa de sucursales y equipos inventariados.
Consulta de detalle operativo por equipo con metrica, eventos y despliegues.
Consulta administrativa de eventos recientes de actualizacion.
Consulta administrativa de alertas operativas recientes.
Auditoria administrativa de login, paquetes y despliegues.
Gestion basica de usuarios administrativos con roles ADMIN, OPERATOR, AUDITOR y VIEWER.
Bloqueo temporal de usuario tras 5 intentos fallidos de login.
Carga, validacion SHA-256, aprobacion, retiro y descarga de paquetes ZIP POS.
Creacion, consulta, programacion, pausa, reanudacion y cancelacion de despliegues.
Creacion de objetivos autorizados por equipo para alimentar instrucciones del agente.
Manejo basico de errores HTTP.
```

La estructura interna del backend esta en espanol:

```txt
src/main/java/com/farmamia/operations
  aplicacion/
    casouso/
    excepcion/
  dominio/
    modelo/
    puerto/
  infraestructura/
    persistencia/
      adaptador/
      entidad/
      repositorio/
    almacenamiento/
    seguridad/
  presentacion/
    controlador/
    dto/
```

El contrato REST mantiene nombres JSON compatibles con el documento MVP mediante `@JsonProperty`.

La migracion inicial esta en:

```txt
src/main/resources/db/migration/V1__farmamia_ops_schema.sql
```

## Configuracion local

Variables soportadas:

```txt
FARMAMIA_DB_URL=jdbc:postgresql://localhost:5432/farmamia_ops
FARMAMIA_DB_USER=farmamia
FARMAMIA_DB_PASSWORD=farmamia
FARMAMIA_API_PORT=8080
FARMAMIA_PACKAGE_STORAGE=./data/packages
FARMAMIA_JWT_SECRET=usar-un-secreto-fuerte-en-qa-prod
```

En perfiles `qa`, `prod` o `production`, `farmamia.security.jwt-secret` es obligatorio y no puede usar el secreto demo de desarrollo. Si falta, la aplicacion falla al iniciar.

Comandos:

```bash
./mvnw spring-boot:run
./mvnw test
```

En Windows:

```powershell
.\mvnw.cmd spring-boot:run
.\mvnw.cmd test
```

Nota: la aplicacion requiere PostgreSQL disponible porque Flyway aplica el esquema al iniciar.

## Pruebas de integracion

Las pruebas de integracion HTTP usan Testcontainers con PostgreSQL aislado. Para ejecutarlas completas se requiere Docker Desktop activo:

```bash
./mvnw test
```

Si Docker no esta disponible, las pruebas de integracion se omiten automaticamente y se mantienen activas las pruebas unitarias. Cubren paquetes ZIP reales, instrucciones generadas por despliegue, resultado con alerta, seguridad por rol, auditoria administrativa y filtros de alertas.

## Datos demo locales

Usuario administrativo local:

```txt
Usuario: admin
Contrasena: admin123
```

Este usuario se crea por Flyway en `V3__usuario_admin_desarrollo.sql` y debe cambiarse antes de usar el sistema fuera de desarrollo.
La semilla demo esta controlada por `FARMAMIA_SEED_DEMO_ADMIN`; en QA/PROD debe estar en `false`.

El usuario autenticado puede rotar su contrasena desde el panel o mediante:

```http
POST /api/admin/security/password
```

Reglas minimas de contrasena nueva:

```txt
Al menos 10 caracteres.
Debe incluir mayusculas, minusculas y numeros.
La accion queda auditada como ADMIN_PASSWORD_CHANGED.
```

La administracion de usuarios esta disponible solo para rol `ADMIN`:

```http
GET    /api/admin/users
GET    /api/admin/users/{id}
POST   /api/admin/users
PUT    /api/admin/users/{id}
POST   /api/admin/users/{id}/activate
POST   /api/admin/users/{id}/deactivate
POST   /api/admin/users/{id}/reset-password
POST   /api/admin/users/{id}/change-role
```

Reglas principales:

```txt
ADMIN administra usuarios y roles.
OPERATOR opera paquetes, equipos, despliegues y alertas, pero no usuarios.
AUDITOR consulta auditoria, eventos, alertas y reportes.
VIEWER tiene lectura general.
No hay borrado fisico de usuarios; se usa activacion/desactivacion.
Un ADMIN no puede desactivarse a si mismo ni quitarse su propio rol ADMIN.
El hash de contrasena nunca se expone por API.
Despues de 5 intentos fallidos, el usuario queda bloqueado por 15 minutos.
Login correcto reinicia intentos y registra ultimo acceso.
Creacion, edicion, activacion, desactivacion, cambio de rol, reset y fallos de login quedan auditados.
```

Con la API levantada en `http://localhost:8081`, se puede registrar un agente de prueba y generar heartbeat/evento operativo:

```powershell
powershell -ExecutionPolicy Bypass -File .\herramientas\desarrollo\registrar-agente-demo.ps1
```

El script usa el flujo real de la API:

```txt
POST /api/agent/register
POST /api/agent/heartbeat
POST /api/agent/{deviceId}/events
```

Despues de ejecutarlo, el panel muestra una sucursal demo, un equipo ONLINE y un evento reciente.

Para completar el flujo MVP con paquete aprobado y despliegue RUNNING:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\herramientas\desarrollo\crear-despliegue-demo.ps1
```

Este segundo script crea un ZIP POS de prueba, lo carga por `POST /api/packages`, lo aprueba y crea un despliegue para el equipo demo.

Para validar el escenario E2E de fallo controlado y alerta visible:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\herramientas\desarrollo\crear-alerta-fallo-demo.ps1
```

Este script crea o reutiliza el equipo `POS-DEMO-001`, carga/aprueba el paquete `2026.06.2-fail`, crea una campana RUNNING, consulta instrucciones como agente, descarga el paquete, detecta que el ZIP no contiene `Zabyca.Pos.Desktop.exe`, reporta `VALIDATION_FAILED` y `FAILED`, y valida que `/api/alerts` contenga una alerta `CRITICAL`.

Para validar el escenario E2E exitoso:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\herramientas\desarrollo\crear-actualizacion-exitosa-demo.ps1
```

Este script registra `POS-DEMO-SUCCESS-001`, carga/aprueba el paquete `2026.06.2-success`, crea una campana RUNNING, consulta instrucciones como agente, descarga el ZIP, valida SHA-256, crea respaldo local, actualiza una carpeta POS temporal, valida `Zabyca.Pos.Desktop.exe`, reporta `COMPLETED` y confirma que no se generen alertas.

## Endpoints iniciales

```http
GET  /api/health
POST /api/auth/login
GET  /api/admin/users
POST /api/agent/register
POST /api/agent/heartbeat
GET  /api/agent/{deviceId}/instructions
POST /api/agent/{deviceId}/events
POST /api/agent/{deviceId}/update-result
GET  /api/branches
GET  /api/devices
GET  /api/devices/{id}
GET  /api/update-events
GET  /api/alerts
GET  /api/audit-logs
```

Los endpoints del agente ya tienen DTOs y casos de uso base.
`register`, `heartbeat`, `instructions`, `events`, `update-result` y `packages` ya consultan/persisten datos reales.

Los endpoints del agente, excepto `POST /api/agent/register`, ya exigen el encabezado:

```http
Authorization: Bearer {agent_token}
```

El token recibido se hashea con SHA-256 y se valida contra `agent_tokens`. Si es valido, se actualiza `last_used_at`.

## Reglas de autenticacion administrativa

Los endpoints administrativos requieren:

```http
Authorization: Bearer {accessToken}
```

Endpoints publicos:

```http
GET  /api/health
POST /api/auth/login
POST /api/agent/register
GET  /actuator/health
GET  /swagger-ui/**
GET  /api-docs/**
```

Los endpoints del agente mantienen su token tecnico independiente.

## Reglas de auditoria administrativa

El modulo de auditoria registra acciones administrativas relevantes sin exponer JPA desde los controladores:

```txt
dominio/modelo/DatosAuditoria.java
dominio/modelo/AuditoriaRegistrada.java
dominio/puerto/RepositorioAuditoria.java
aplicacion/casouso/GestionarAuditoriaCasoUso.java
infraestructura/persistencia/adaptador/RepositorioAuditoriaJpaAdaptador.java
presentacion/controlador/ControladorAuditoria.java
```

Acciones auditadas:

```txt
ADMIN_LOGIN
PACKAGE_UPLOADED
PACKAGE_APPROVED
PACKAGE_RETIRED
DEPLOYMENT_CREATED
DEPLOYMENT_SCHEDULED
DEPLOYMENT_PAUSED
DEPLOYMENT_RESUMED
DEPLOYMENT_CANCELLED
```

Endpoint implementado:

```http
GET /api/audit-logs?limit=100
```

La descarga de paquetes acepta dos escenarios autorizados:

```txt
Panel administrativo: JWT administrativo.
Agente Windows: token tecnico del agente.
```

## Reglas de instrucciones

`GET /api/agent/{deviceId}/instructions` devuelve `UPDATE_POS` solo si:

```txt
Existe un deployment target para el equipo.
El target esta en estado AUTHORIZED.
El deployment esta en estado SCHEDULED, APPROVED, PILOT_RUNNING o RUNNING.
El paquete POS esta en estado APPROVED.
```

Si alguna condicion no se cumple, responde:

```json
{
  "hasInstruction": false
}
```

La URL de descarga se entrega segun el contrato:

```txt
/api/packages/{packageId}/download
```

El endpoint de descarga ya esta implementado en el modulo de paquetes.

Este flujo aplica el patron hexagonal:

```txt
dominio/modelo/InstruccionAgente.java
dominio/puerto/RepositorioInstruccionesAgente.java
aplicacion/casouso/ConsultarInstruccionesAgenteCasoUso.java
infraestructura/persistencia/adaptador/RepositorioInstruccionesAgenteJpaAdaptador.java
presentacion/controlador/ControladorAgente.java
```

Regla aplicada:

```txt
ConsultarInstruccionesAgenteCasoUso depende de un puerto de dominio.
La consulta JPA de deployment_targets queda aislada en infraestructura.
ControladorAgente traduce el modelo de dominio a RespuestaInstruccionAgente.
```

## Reglas de eventos y resultados

`POST /api/agent/{deviceId}/events`:

```txt
Valida que el equipo exista.
Valida el objetivo de despliegue si viene informado.
Persiste el evento en update_events.
Guarda metadata como JSON.
```

`POST /api/agent/{deviceId}/update-result`:

```txt
Valida equipo y objetivo de despliegue.
Actualiza el estado del objetivo.
Actualiza pos_version del equipo solo si el resultado es COMPLETED.
Registra evento UPDATE_COMPLETED, ROLLBACK_COMPLETED o FAILED.
Genera alerta CRITICAL si el resultado falla.
```

## Reglas de alertas operativas

El modulo de alertas expone lectura administrativa para el panel y mantiene separada la persistencia JPA del caso de uso:

```txt
dominio/modelo/AlertaEquipo.java
dominio/modelo/AlertaRegistrada.java
dominio/puerto/RepositorioAlertas.java
aplicacion/casouso/ConsultarAlertasCasoUso.java
infraestructura/persistencia/adaptador/RepositorioAlertasJpaAdaptador.java
presentacion/controlador/ControladorAlertas.java
```

Endpoint implementado:

```http
GET /api/alerts?limit=100
```

Regla aplicada:

```txt
RegistrarEventoAgenteCasoUso crea alertas ante resultados fallidos.
ConsultarAlertasCasoUso normaliza el limite de consulta.
ControladorAlertas traduce el modelo de dominio a DTO REST.
Las entidades JPA de alerts, devices y branches quedan encapsuladas en infraestructura.
```

## Reglas de paquetes POS

## Reglas de catalogo operativo

Este modulo expone consultas administrativas para el panel web y mantiene el patron hexagonal:

```txt
dominio/modelo/Equipo.java
dominio/modelo/Sucursal.java
dominio/puerto/RepositorioEquipos.java
dominio/puerto/RepositorioSucursales.java
aplicacion/casouso/ConsultarCatalogoOperativoCasoUso.java
infraestructura/persistencia/adaptador/RepositorioEquiposJpaAdaptador.java
infraestructura/persistencia/adaptador/RepositorioSucursalesJpaAdaptador.java
presentacion/controlador/ControladorEquipos.java
presentacion/controlador/ControladorSucursales.java
```

Endpoints implementados:

```http
GET /api/branches
GET /api/devices
GET /api/update-events?limit=100
GET /api/alerts?limit=100
```

Regla aplicada:

```txt
El panel consulta equipos y sucursales mediante casos de uso.
El panel consulta eventos recientes mediante casos de uso.
El panel consulta alertas recientes mediante casos de uso.
Los controladores no acceden a repositorios JPA.
Las entidades `branches`, `devices`, `update_events` y `alerts` quedan encapsuladas en infraestructura.
```

Este modulo ya aplica el patron hexagonal como referencia para el resto del backend:

```txt
dominio/modelo/PaquetePos.java
dominio/puerto/RepositorioPaquetesPos.java
dominio/puerto/AlmacenamientoPaquetes.java
aplicacion/casouso/GestionarPaquetesPosCasoUso.java
infraestructura/persistencia/adaptador/RepositorioPaquetesPosJpaAdaptador.java
infraestructura/almacenamiento/ServicioAlmacenamientoPaquetes.java
presentacion/controlador/ControladorPaquetes.java
```

Regla aplicada:

```txt
El caso de uso depende de puertos de dominio.
JPA y almacenamiento local son adaptadores de infraestructura.
MultipartFile y Resource quedan en presentacion/infraestructura, no en aplicacion.
```

## Reglas de agentes y heartbeat

Este modulo tambien aplica el patron hexagonal:

```txt
dominio/modelo/Equipo.java
dominio/modelo/DatosRegistroAgente.java
dominio/modelo/DatosLatido.java
dominio/modelo/DatosEventoAgente.java
dominio/modelo/ResultadoActualizacion.java
dominio/modelo/EventoActualizacion.java
dominio/modelo/AlertaEquipo.java
dominio/modelo/MetricaEquipo.java
dominio/modelo/RegistroAgente.java
dominio/puerto/RepositorioEquipos.java
dominio/puerto/RepositorioSucursales.java
dominio/puerto/RepositorioTokensAgente.java
dominio/puerto/RepositorioMetricasEquipo.java
dominio/puerto/RepositorioObjetivosDespliegue.java
dominio/puerto/RepositorioEventosActualizacion.java
dominio/puerto/RepositorioAlertas.java
dominio/puerto/HasherTokens.java
infraestructura/persistencia/adaptador/RepositorioEquiposJpaAdaptador.java
infraestructura/persistencia/adaptador/RepositorioSucursalesJpaAdaptador.java
infraestructura/persistencia/adaptador/RepositorioTokensAgenteJpaAdaptador.java
infraestructura/persistencia/adaptador/RepositorioMetricasEquipoJpaAdaptador.java
infraestructura/persistencia/adaptador/RepositorioObjetivosDespliegueJpaAdaptador.java
infraestructura/persistencia/adaptador/RepositorioEventosActualizacionJpaAdaptador.java
infraestructura/persistencia/adaptador/RepositorioAlertasJpaAdaptador.java
```

Regla aplicada:

```txt
RegistrarAgenteCasoUso depende de puertos de dominio.
RegistrarLatidoCasoUso depende de puertos de dominio.
RegistrarEventoAgenteCasoUso depende de puertos de dominio.
Los DTOs HTTP se traducen en ControladorAgente.
JPA, entidades persistentes, hashing y serializacion JSON quedan como adaptadores de infraestructura.
```

Endpoints implementados:

```http
POST /api/packages
GET  /api/packages
GET  /api/packages/{id}
POST /api/packages/{id}/approve
POST /api/packages/{id}/retire
GET  /api/packages/{id}/download
```

Carga:

```txt
Content-Type: multipart/form-data
version: version funcional del POS
file: archivo .zip
```

Reglas:

```txt
Solo acepta archivos .zip.
Calcula SHA-256 al guardar el archivo.
Guarda el ZIP en FARMAMIA_PACKAGE_STORAGE.
Un paquete cargado queda en estado VALIDATED.
Solo paquetes APPROVED pueden descargarse.
```

## Reglas de despliegues

Este modulo tambien aplica el patron hexagonal:

```txt
dominio/modelo/DatosCrearDespliegue.java
dominio/modelo/Despliegue.java
dominio/modelo/EstadoDespliegue.java
dominio/puerto/RepositorioDespliegues.java
aplicacion/casouso/GestionarDesplieguesCasoUso.java
infraestructura/persistencia/adaptador/RepositorioDesplieguesJpaAdaptador.java
presentacion/controlador/ControladorDespliegues.java
```

Regla aplicada:

```txt
GestionarDesplieguesCasoUso depende del puerto RepositorioDespliegues.
Las entidades JPA DespliegueEntidad y ObjetivoDespliegueEntidad quedan en infraestructura.
ControladorDespliegues traduce DTOs REST a modelos de dominio y respuestas HTTP.
```

Endpoints implementados:

```http
POST /api/deployments
GET  /api/deployments
GET  /api/deployments/{id}
POST /api/deployments/{id}/schedule
POST /api/deployments/{id}/pause
POST /api/deployments/{id}/resume
POST /api/deployments/{id}/cancel
GET  /api/deployments/{id}/status
```

Creacion:

```txt
Requiere un paquete POS aprobado.
Recibe lista de deviceIds.
Crea un deployment.
Crea deployment_targets en estado AUTHORIZED.
Si scheduledAt viene vacio, el deployment queda RUNNING.
Si scheduledAt viene informado, el deployment queda SCHEDULED.
```

Efecto operativo:

```txt
Un target AUTHORIZED con paquete APPROVED queda disponible para GET /api/agent/{deviceId}/instructions.
```

## Dependencias recomendadas

```txt
Java 21
Spring Boot 3.x
Spring Web
Spring Security
Spring Data JPA
PostgreSQL Driver
Flyway
Bean Validation
Actuator
Micrometer
OpenAPI
MapStruct
```

## Arquitectura obligatoria

El backend debe construirse con **arquitectura limpia**, **arquitectura hexagonal** y principios **SOLID**.

Reglas:

```txt
La capa aplicacion contiene casos de uso y orquestacion.
La capa dominio contiene reglas, modelos y puertos cuando aplique.
La capa infraestructura contiene adaptadores de salida: JPA, archivos, seguridad, monitoreo.
La capa presentacion contiene adaptadores de entrada: controladores REST y DTOs.
Las dependencias apuntan hacia aplicacion/dominio.
Los controladores no contienen logica de negocio.
Los repositorios JPA no deben filtrarse hacia controladores.
Los detalles tecnicos deben quedar detras de servicios/adaptadores.
```

Principios SOLID:

```txt
Responsabilidad unica por clase.
Casos de uso pequenos y enfocados.
Interfaces especificas para puertos de dominio/aplicacion.
Dependencias sobre abstracciones cuando exista logica de negocio relevante.
Extensiones mediante nuevos adaptadores o servicios, no modificando reglas centrales.
```

## Estructura recomendada

```txt
src/main/java/com/farmamia/operations
  aplicacion/
    casouso/
    servicio/
  dominio/
    modelo/
    puerto/
    politica/
  infraestructura/
    persistencia/
    almacenamiento/
    seguridad/
    monitoreo/
  presentacion/
    controlador/
    dto/
    mapeador/
```

Lectura hexagonal:

```txt
presentacion/      -> adaptadores de entrada
aplicacion/        -> casos de uso
dominio/           -> nucleo de negocio y puertos
infraestructura/   -> adaptadores de salida
```

## Primeros modulos

```txt
AgentModule
BranchModule
DeviceModule
PackageModule
DeploymentModule
AuditModule
AlertModule
```
