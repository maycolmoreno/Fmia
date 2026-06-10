# Farmamia Operations Center - Backlog Tecnico MVP

> Objetivo: convertir la propuesta de Fase 0 en una primera base ejecutable.

## Orden recomendado

```txt
1. Crear backend Spring Boot base.
2. Agregar Flyway y migracion V1 del esquema PostgreSQL.
3. Implementar registro de agente.
4. Implementar heartbeat e inventario basico.
5. Implementar consulta de instrucciones.
6. Implementar recepcion de eventos del agente.
7. Implementar carga y aprobacion de paquetes ZIP. (base implementada)
8. Implementar campanas/despliegues y targets autorizados. (base implementada)
9. Crear Worker Service .NET con registro y heartbeat.
10. Crear flujo local de descarga, checksum, backup, update y rollback.
11. Crear panel Angular minimo para dashboard, equipos, paquetes y campanas.
```

## Pruebas de integracion

Estado: implementadas con Testcontainers PostgreSQL y MockMvc.

Cobertura actual:

```txt
Paquetes ZIP reales: carga, SHA256, aprobacion, descarga aprobada y bloqueo de descarga no aprobada.
Instrucciones generadas por despliegue: UPDATE_POS con packageId, version, downloadUrl y checksum.
Eventos, resultado y alertas: FAILED genera alerta; COMPLETED actualiza target y version POS sin alerta adicional.
Seguridad por rol via HTTP: ADMIN, OPERATOR, AUDITOR, VIEWER y usuario inactivo.
Auditoria administrativa: login, paquetes, despliegues, cambio de rol y desactivacion.
Filtros de alertas: estado, severidad, tipo, equipo, sucursal, fecha, paginacion y orden.
```

Nota: requieren Docker activo. Si Docker no esta disponible, se omiten automaticamente para no bloquear las pruebas unitarias locales.

## Backend API

### MVP-BE-001 - Inicializar proyecto Spring Boot

Crear el proyecto `backend-api` con:

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
```

Criterios:

```txt
La aplicacion inicia correctamente.
Existe endpoint de salud.
La configuracion usa perfiles dev/test/prod.
La estructura interna usa paquetes y clases en espanol.
La estructura respeta arquitectura limpia y hexagonal.
Los casos de uso no dependen directamente de controladores.
La infraestructura queda aislada como adaptador tecnico.
Se aplican principios SOLID desde el primer modulo.
```

### MVP-BE-002 - Migracion inicial

Copiar:

```txt
Documentacion/FARMAMIA_OPERATIONS_CENTER_SCHEMA_POSTGRESQL.sql
```

a:

```txt
backend-api/src/main/resources/db/migration/V1__farmamia_ops_schema.sql
```

Criterios:

```txt
Flyway crea el esquema en PostgreSQL.
La aplicacion falla al arrancar si la migracion no aplica.
```

### MVP-BE-003 - Registro de agente

Estado: base implementada.
Estado arquitectonico: refactorizado con puertos/adaptadores.

Implementar:

```http
POST /api/agent/register
```

Criterios:

```txt
Registra sucursal si existe o rechaza si no existe, segun politica definida.
Crea o actualiza equipo por hostname/macAddress.
Emite token tecnico del agente.
Guarda token como hash, nunca en texto plano.
Registra auditoria.
```

### MVP-BE-004 - Heartbeat

Estado: base implementada.
Estado arquitectonico: refactorizado con puertos/adaptadores.

Implementar:

```http
POST /api/agent/heartbeat
```

Criterios:

```txt
Valida Bearer token del agente.
Actualiza last_heartbeat_at, version POS y version agente.
Guarda metrica operativa.
Marca equipo como ONLINE.
```

### MVP-BE-004A - Validacion de token tecnico

Estado: base implementada.

Criterios:

```txt
Los endpoints del agente, excepto register, exigen Authorization: Bearer.
El token se valida contra el hash activo en agent_tokens.
El token invalido responde 401.
El token valido actualiza last_used_at.
```

### MVP-BE-005 - Instrucciones

Estado: base implementada.
Estado arquitectonico: refactorizado con puertos/adaptadores.

Implementar:

```http
GET /api/agent/{deviceId}/instructions
```

Criterios:

```txt
Devuelve hasInstruction=false si no hay tareas.
Devuelve UPDATE_POS si hay deployment target autorizado.
Incluye packageId, version, downloadUrl y sha256Checksum.
No entrega paquetes no aprobados.
```

Pendiente:

```txt
Ampliar cobertura de integracion para casos de error adicionales.
```

### MVP-BE-006 - Eventos y resultado

Estado: base implementada.
Estado arquitectonico: refactorizado con puertos/adaptadores.
Estado de alertas: lectura administrativa implementada.
Estado demo E2E exitoso: implementado con `herramientas/desarrollo/crear-actualizacion-exitosa-demo.ps1`.

Implementar:

```http
POST /api/agent/{deviceId}/events
POST /api/agent/{deviceId}/update-result
```

Criterios:

```txt
Guarda eventos en update_events.
Actualiza status en deployment_targets.
Actualiza pos_version del device si el resultado es COMPLETED.
Genera alerta si falla o si rollback falla.
No genera alerta cuando el resultado es COMPLETED.
```

Pendiente:

```txt
Ampliar cobertura de integracion para rollback y errores no felices.
```

### MVP-BE-006A - Consulta administrativa de alertas

Estado: implementado.
Estado arquitectonico: caso de uso, puerto y adaptador JPA.
Estado demo E2E: implementado con `herramientas/desarrollo/crear-alerta-fallo-demo.ps1`.
Estado gestion: reconocimiento y cierre de alertas implementados para ADMIN/OPERATOR con auditoria.
Estado filtros: implementados por estado, severidad, tipo, equipo, sucursal, rango de fechas, paginacion y orden.

Implementado:

```http
GET /api/alerts?limit=100
```

Criterios:

```txt
Lista alertas recientes generadas por fallos de actualizacion o rollback.
Incluye equipo, sucursal, severidad, tipo, estado, titulo, mensaje y fecha de apertura.
Requiere JWT administrativo.
Permite reconocer alertas con `POST /api/alerts/{id}/acknowledge`.
Permite cerrar alertas con `POST /api/alerts/{id}/close`.
Registra auditoria `ALERT_ACKNOWLEDGED` y `ALERT_CLOSED`.
Soporta filtros en `GET /api/alerts` por `status`, `severity`, `type`, `deviceId`, `branchId`, `branchCode`, `hostname`, `dateFrom`, `dateTo`, `limit`, `page`, `size` y `sort`.
No expone repositorios JPA desde controladores.
El script demo genera un fallo reproducible con un ZIP sin `Zabyca.Pos.Desktop.exe`.
```

### MVP-BE-007 - Paquetes POS

Estado: base implementada.
Estado arquitectonico: refactorizado como modulo hexagonal de referencia.
Estado seguridad: escritura limitada a ADMIN/OPERATOR; lectura general permitida a usuarios autenticados.

Implementar:

```http
POST /api/packages
GET  /api/packages
GET  /api/packages/{id}
POST /api/packages/{id}/approve
POST /api/packages/{id}/retire
GET  /api/packages/{id}/download
```

Criterios:

```txt
Permite cargar archivo ZIP.
Calcula SHA256.
Persiste version, nombre, ruta, checksum, tamano y estado.
Permite aprobar y retirar paquete.
Solo descarga paquetes aprobados.
```

Pendiente:

```txt
Ampliar pruebas de integracion con variantes de ZIP invalido.
Agregar validacion opcional de estructura interna del ZIP.
Replicar el mismo patron de puertos/adaptadores en despliegues.
```

### MVP-BE-008 - Despliegues y targets

Estado: base implementada.
Estado arquitectonico: refactorizado con puertos/adaptadores.
Estado seguridad: escritura limitada a ADMIN/OPERATOR; lectura general permitida a usuarios autenticados.

Implementar:

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

Criterios:

```txt
Solo permite crear despliegues con paquetes aprobados.
Crea targets por equipo en estado AUTHORIZED.
Permite programar, pausar, reanudar y cancelar.
Permite consultar conteo de targets por estado.
Alimenta GET /api/agent/{deviceId}/instructions.
```

Pendiente:

```txt
Separar flujo piloto y aprobacion formal antes de despliegue general.
Agregar grupos trx001, trx002, etc.
Agregar limites de 100 equipos por grupo.
Agregar reglas de ancho de banda por sucursal.
Agregar pruebas de integracion para instrucciones generadas por deployment.
```

## Windows Agent

### MVP-AG-001 - Worker Service base

Estado: base implementada.
Estado arquitectonico: creado con capas Dominio, Aplicacion, Infraestructura y Servicio.
Estado robustecimiento: servicio Windows formal, estructura final y publicacion operativa implementados.

Criterios:

```txt
Crea ejecutable Farmamia.Agent.Service.exe.
Publica Farmamia.Agent.Service.exe y FarmamiaUpdater.exe en una carpeta Agent lista para copiar.
Instala como Windows Service FarmamiaOpsAgent.
Configura inicio automatico y recuperacion por fallo.
Lee configuracion desde C:\Program Files (x86)\Farmamia Cia Ltda - Elipsys\Agent.
Genera config.json con seccion AgenteFarmamia.
Crea carpetas Downloads, Backups, Logs, Temp y State.
Guarda logs diarios en Logs.
```

### MVP-AG-002 - Registro y heartbeat

Estado: base implementada.
Estado robustecimiento: persistencia local y backoff implementados.

Criterios:

```txt
Recolecta hostname, IP, MAC, Windows, version agente, version POS y ruta POS.
Se registra contra la API.
Persiste deviceId y token tecnico.
Recupera deviceId y token despues de reinicio.
No se vuelve a registrar si las credenciales locales existen.
Envia heartbeat periodico.
Mantiene vivo el servicio si la API no responde.
Reintenta inicializacion, heartbeat e instrucciones con backoff progresivo.
```

Pendiente:

```txt
Validar rotacion futura de token tecnico.
Definir politica de renovacion si el backend invalida credenciales.
```

### MVP-AG-003 - Flujo de actualizacion local

Estado: flujo robustecido implementado.

Criterios:

```txt
Consulta instrucciones.
Descarga ZIP autorizado.
Reintenta descarga hasta 3 veces.
Valida SHA256.
Si SHA256 falla, reporta VALIDATION_FAILED y no modifica POS.
Crea backup antes de modificar.
Actualiza archivos del POS.
Valida existencia de Zabyca.Pos.Desktop.exe.
Reporta UPDATE_STARTED, UPDATE_COMPLETED y resultado COMPLETED.
Reporta UPDATE_FAILED si falla la actualizacion.
Ejecuta rollback si falla despues de modificar archivos.
Reporta ROLLBACK_STARTED, ROLLBACK_COMPLETED o ROLLBACK_FAILED.
No actualiza version POS local/backend si falla.
```

Pendiente:

```txt
Evaluar integracion futura con notificaciones interactivas de Windows si el entorno POS lo permite.
Agregar instalador MSI o pipeline de publicacion para equipos reales.
Ampliar pruebas locales para API no disponible con tiempos/backoff controlados.
```

### MVP-AG-004 - Ejecutable manual y pruebas locales

Estado: base implementada.
Estado E2E real: procedimiento y scripts implementados para maquina Windows limpia.

Criterios:

```txt
FarmamiaUpdater permite consultar estado local.
FarmamiaUpdater permite ver version POS.
FarmamiaUpdater permite consultar instruccion autorizada.
FarmamiaUpdater permite ejecutar instalar-ahora usando el mismo caso de uso del servicio.
FarmamiaUpdater permite enviar diagnostico al backend.
Script `herramientas/publicar-agente.ps1` publica servicio, updater, config.json y estructura local.
Script de publicacion permite copiar a ruta final e instalar el servicio opcionalmente.
Pruebas locales cubren ZIP valido, ZIP sin ejecutable, checksum incorrecto, backup, rollback y token persistido.
Pruebas del caso de uso cubren COMPLETED, UPDATE_COMPLETED, VALIDATION_FAILED y FAILED sin modificar POS.
Pruebas del caso de uso cubren reintento de descarga y rollback despues de modificar archivos.
Script E2E valida instalacion de servicio, heartbeat, paquete valido, despliegue, COMPLETED, evento y ausencia de alerta.
Script E2E valida paquete fallido sin ejecutable, FAILED, posVersion sin cambio y alerta critica.
Documento E2E y plantilla de resultados disponibles en Documentacion.
```

## Admin Panel

### MVP-WEB-001 - Base Angular

Estado: base implementada.

Criterios:

```txt
Login.
Route guards.
JWT interceptor.
Layout operativo sin landing page.
```

### MVP-WEB-002 - Pantallas minimas

Criterios:

```txt
Dashboard operativo.
Listado de equipos.
Detalle de equipo.
Listado de paquetes.
Carga de paquete.
Crear campana.
Estado de campana.
Eventos y auditoria.
Alertas operativas.
```

Estado actual:

```txt
Dashboard operativo implementado.
Login administrativo JWT implementado.
Listado de equipos y sucursales implementado.
Detalle de equipo implementado con inventario, metrica, eventos y despliegues asociados.
Listado, carga, aprobacion, retiro y descarga de paquetes implementado.
Creacion y estado de campanas/despliegues implementado.
Listado de eventos recientes de actualizacion implementado.
Listado de alertas operativas recientes implementado.
Resaltado visual de alertas criticas implementado.
Reconocimiento y cierre de alertas implementado en panel para ADMIN/OPERATOR.
Demo E2E de fallo controlado y alerta visible implementada.
Demo E2E de actualizacion exitosa implementada.
Script de datos demo implementado para registrar agente, heartbeat y evento contra la API real.
Script de despliegue demo implementado para cargar ZIP, aprobar paquete y crear campana RUNNING.
Gestion completa de usuarios administrativos implementada para ADMIN.
Permisos finos por rol implementados para usuarios, paquetes, despliegues, eventos, alertas y auditoria.
Filtros avanzados de auditoria administrativa implementados por accion, entidad, usuario y rango de fechas.
```

### MVP-WEB-004 - Seguridad administrativa

Estado: implementado para MVP.

Criterios cubiertos:

```txt
El usuario autenticado puede cambiar su propia contrasena.
La contrasena actual se valida antes del cambio.
La nueva contrasena exige longitud minima, mayusculas, minusculas y numeros.
La nueva contrasena se guarda usando PasswordEncoder.
El cambio queda auditado como ADMIN_PASSWORD_CHANGED.
El panel incluye vista Seguridad.
El ADMIN puede listar, crear, editar, activar/desactivar, cambiar rol y resetear contrasena de usuarios administrativos.
La opcion Usuarios solo aparece para ADMIN.
Los usuarios inactivos no pueden iniciar sesion.
Despues de 5 intentos fallidos, el usuario queda bloqueado temporalmente por 15 minutos.
El login correcto reinicia el contador de intentos y registra ultimo acceso.
El secreto JWT es obligatorio en perfiles QA/PROD.
Las acciones criticas quedan auditadas.
No se expone el hash de contrasena por API.
```

Pendiente:

```txt
Politicas de expiracion periodica de contrasena.
Rotacion operacional de secreto JWT por ambiente.
```

### MVP-WEB-003 - Auditoria administrativa

Estado: base implementada.
Estado arquitectonico: caso de uso, puerto, adaptador JPA y controlador REST.
Estado filtros: implementado por accion, entidad, usuario y rango de fechas.

Criterios cubiertos:

```txt
Registra login administrativo.
Registra carga, aprobacion y retiro de paquetes.
Registra creacion y cambios de estado de despliegues.
Expone GET /api/audit-logs?limit=100 protegido por JWT.
El panel muestra auditoria reciente.
Permite filtrar por accion, entidad, usuario y rango de fechas.
```

Pendiente:

```txt
Auditar acciones operativas adicionales del agente cuando esos flujos esten implementados.
```

## Decisiones pendientes

```txt
Politica para alta automatica de sucursales.
Duracion y rotacion de token de agente.
Ruta fisica del repositorio ZIP.
Limite global de descargas desde el MVP.
Metodo de deteccion de actividad POS.
Metodo de cierre/reapertura del POS a la 01:00.
```
