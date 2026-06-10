# Farmamia Operations Center - Plan Tecnico por Fases

> Estado: Propuesta inicial accionable | Stack: Java Spring Boot, .NET 8 Worker Service, PostgreSQL, Angular, Grafana

---

## 1. Vision del Proyecto

**Farmamia Operations Center** es una plataforma empresarial on-premise para operar, actualizar, monitorear y auditar la infraestructura POS de la cadena Farmamia.

El primer objetivo del proyecto es resolver el despliegue centralizado del POS portable ubicado en:

```txt
C:\Program Files (x86)\Farmamia Cia Ltda - Elipsys\Cliente
```

Ejecutable principal:

```txt
Zabyca.Pos.Desktop.exe
```

El sistema debe escalar para aproximadamente:

```txt
645 farmacias
1800-1900 equipos Windows
3 equipos POS por farmacia
90 MB aproximados por actualizacion
10 Mbps aproximados por farmacia
400 Mbps disponibles en servidor central
```

---

## 2. Arquitectura Base

```txt
[Panel Angular]
      |
      | HTTPS interno
      v
[API Java Spring Boot]
      |
      +--> [PostgreSQL]
      |
      +--> [Repositorio interno de paquetes ZIP]
      |
      +--> [Grafana / Prometheus / Actuator]
      ^
      |
      | HTTPS interno
      |
[Agente Windows .NET 8 Worker Service]
      |
      +--> Inventario local
      +--> Descarga autorizada
      +--> Backup
      +--> Actualizacion POS
      +--> Rollback
      +--> Reporte de eventos y metricas
```

---

## 3. Principios de Diseno

El proyecto debe aplicar **arquitectura limpia**, **arquitectura hexagonal** y principios **SOLID** desde el inicio.

Esta regla aplica a todos los componentes del sistema:

```txt
Backend Spring Boot
Agente Windows .NET
Panel Angular
Scripts de base de datos
Integraciones de monitoreo
```

### Reglas arquitectonicas obligatorias

```txt
El dominio no depende de frameworks, base de datos, HTTP, archivos ni librerias externas.
Los casos de uso orquestan reglas de negocio y dependen de puertos/interfaces.
La infraestructura implementa adaptadores de salida: JPA, archivos, API externas, monitoreo.
La presentacion implementa adaptadores de entrada: controladores REST, DTOs, validaciones HTTP.
Las dependencias siempre apuntan hacia el dominio/aplicacion, nunca al reves.
Los detalles tecnicos se inyectan mediante interfaces.
Cada clase debe tener una responsabilidad clara y pequena.
```

### Principios SOLID esperados

```txt
S - Responsabilidad unica: cada clase cambia por una sola razon.
O - Abierto/cerrado: extender comportamiento sin modificar reglas centrales.
L - Sustitucion de Liskov: las implementaciones respetan contratos de dominio.
I - Segregacion de interfaces: puertos pequenos y especificos por caso de uso.
D - Inversion de dependencias: aplicacion/dominio dependen de abstracciones.
```

### Arquitectura hexagonal esperada

```txt
Adaptadores de entrada:
  REST controllers
  DTOs HTTP
  Validadores de request

Nucleo:
  Casos de uso
  Modelos de dominio
  Politicas de negocio
  Puertos/interfaces

Adaptadores de salida:
  Repositorios JPA
  Almacenamiento de paquetes ZIP
  Clientes HTTP
  Servicios Windows
  Publicacion de metricas
```

### Backend Spring Boot

Estructura recomendada:

```txt
com.farmamia.operations
  aplicacion
    casouso
    servicio
  dominio
    modelo
    repositorio
    politica
  infraestructura
    persistencia
    almacenamiento
    seguridad
    monitoreo
  presentacion
    controlador
    dto
    mapeador
```

Regla central:

```txt
Los casos de uso dependen de interfaces de dominio.
La infraestructura implementa esas interfaces.
Los controladores solo traducen HTTP hacia casos de uso.
Ningun controlador debe contener reglas de negocio.
Ninguna entidad de dominio debe depender de JPA, Spring o detalles HTTP.
```

### Agente Windows .NET

Estructura recomendada:

```txt
Farmamia.Agent
  Aplicacion
    Servicios
    CasosUso
  Dominio
    Modelos
    Politicas
  Infraestructura
    Api
    SistemaArchivos
    Windows
    Logging
  Worker
```

Servicios esperados:

```txt
ServicioRegistroAgente
ServicioLatido
ServicioRecoleccionInventario
ServicioConsultaInstrucciones
ServicioDescargaPaquete
ServicioValidacionChecksum
ServicioProcesoPos
ServicioDeteccionActividad
ServicioBackup
ServicioEjecucionActualizacion
ServicioRollback
ServicioReporteEventos
```

---

## 4. Fase 0 - Preparacion Tecnica

### Objetivo

Definir el marco tecnico, operativo y documental antes de construir.

### Alcance

- Definir arquitectura oficial.
- Definir puertos, DNS interno y certificados.
- Definir ambientes: desarrollo, pruebas, produccion.
- Definir estructura de paquete ZIP.
- Definir convencion de versiones POS.
- Definir reglas de auditoria.
- Definir roles de usuario.
- Definir estructura de carpetas del servidor y del agente.
- Definir primera version del modelo PostgreSQL.

### Entregables

```txt
Documento de arquitectura
Modelo de datos inicial
Contrato REST inicial
Politicas de actualizacion
Politicas de seguridad
Backlog MVP
```

### Criterios de aceptacion

- El equipo conoce el alcance del MVP.
- Existe una estructura tecnica validada.
- Las reglas de actualizacion nocturna estan documentadas.
- El modelo de datos base esta aprobado.

---

## 5. Fase 1 - MVP de Actualizacion POS

### Objetivo

Permitir actualizaciones centralizadas, autorizadas, auditadas y reversibles del POS portable.

### Backend Spring Boot

Modulos:

```txt
Agentes
Equipos
Sucursales
Paquetes POS
Campanas de actualizacion
Eventos de actualizacion
Auditoria
```

APIs minimas:

```http
POST /api/agent/register
POST /api/agent/heartbeat
GET  /api/agent/{deviceId}/instructions
POST /api/agent/{deviceId}/events
POST /api/agent/{deviceId}/update-result

POST /api/packages
GET  /api/packages
GET  /api/packages/{id}
POST /api/packages/{id}/approve
GET  /api/packages/{id}/download

POST /api/deployments
GET  /api/deployments
GET  /api/deployments/{id}/status
```

### Agente Windows .NET 8

Funciones minimas:

- Instalarse como Windows Service.
- Registrar equipo ante API.
- Reportar heartbeat.
- Reportar inventario basico.
- Consultar instrucciones.
- Descargar ZIP autorizado.
- Validar checksum SHA256.
- Respaldar carpeta POS.
- Actualizar archivos.
- Validar existencia de `Zabyca.Pos.Desktop.exe`.
- Reportar resultado.
- Ejecutar rollback automatico ante fallo.

### Panel Angular

Pantallas:

```txt
Login
Dashboard operativo basico
Sucursales
Equipos
Paquetes POS
Campanas
Estado por equipo
Auditoria basica
```

### Criterios de aceptacion

- Un paquete ZIP puede cargarse y aprobarse.
- Un equipo puede registrarse automaticamente.
- El servidor puede autorizar una actualizacion.
- El agente puede descargar, validar, respaldar, actualizar y reportar.
- Si falla la actualizacion, el agente ejecuta rollback.
- Toda accion relevante queda en auditoria.

---

## 6. Fase 2 - Pilotos y Despliegue por Grupos

### Objetivo

Reducir riesgo operativo antes del despliegue masivo.

### Alcance

- Gestion de pilotos por farmacia.
- Aprobacion posterior al piloto.
- Creacion de grupos de hasta 100 equipos.
- Nombres de grupo: `trx001`, `trx002`, `trx003`, etc.
- Programacion por fecha y hora.
- Pausar, reanudar y cancelar campanas.
- Reportes de resultado por grupo.

### Reglas

- Ninguna version debe pasar a despliegue general sin piloto.
- Cada grupo debe tener maximo 100 equipos.
- La campana debe pausarse si supera el umbral de fallos configurado.
- Cada cambio de estado debe auditarse.

### Criterios de aceptacion

- Se puede ejecutar piloto en farmacias seleccionadas.
- Se pueden crear grupos `trx`.
- Se puede programar una campana.
- Se puede ver avance por grupo y por farmacia.
- Se puede pausar una campana activa.

---

## 7. Fase 3 - Reglas Horarias y Farmacias de Turno

### Objetivo

Implementar el flujo operativo real de actualizacion nocturna.

### Flujo horario

```txt
23:55 -> intentar actualizar
00:10 -> reintentar si hay actividad
00:25 -> reintentar si hay actividad
00:50 -> primer aviso al usuario
00:55 -> segundo aviso al usuario
01:00 -> cerrar POS, respaldar, actualizar, validar, reabrir y reportar
```

### Reglas

- Si el POS esta abierto, se permite actualizar.
- Si hay actividad, se espera.
- A la 01:00 se fuerza la actualizacion.
- Si el equipo estuvo apagado, al encender debe actualizar antes de abrir el POS.
- Farmacias de turno tienen prioridad.
- Farmacias de turno generan alertas criticas ante fallo.

### Criterios de aceptacion

- El agente respeta la ventana `23:55` a `01:00`.
- El agente muestra avisos a las `00:50` y `00:55`.
- El agente puede cerrar el POS a la `01:00`.
- El agente puede reabrir el POS despues de actualizar.
- Las farmacias de turno aparecen separadas y priorizadas.

---

## 8. Fase 4 - Control de Ancho de Banda

### Objetivo

Evitar saturacion de enlaces de farmacia y del servidor central.

### Configuracion inicial recomendada

```txt
max_global_downloads = 50
max_downloads_per_branch = 1
pause_between_devices_seconds = 180
max_retry_attempts = 3
retry_backoff_seconds = 300
```

### Alcance

- Maximo 1 descarga simultanea por farmacia.
- Maximo global configurable.
- Pausa entre equipos de la misma farmacia.
- Reintentos automaticos.
- Backoff progresivo.
- Registro de velocidad de descarga.
- Alerta por descarga lenta o interrumpida.

### Criterios de aceptacion

- Dos equipos de la misma farmacia no descargan al mismo tiempo.
- El limite global se respeta.
- Un equipo puede reintentar descarga si falla.
- La campana muestra cola, activos, completados y fallidos.

---

## 9. Fase 5 - Monitoreo Operativo

### Objetivo

Medir salud de equipos, enlaces y servicios criticos.

### Metricas

```txt
Online/offline
Ultimo heartbeat
Latencia hacia servidor central
Perdida de paquetes
Espacio libre en disco
Version POS instalada
Estado del agente
Estado del proceso POS
Servicios criticos
Posible corte electrico
```

### Alertas

```txt
Equipo offline
Farmacia offline
Disco bajo
Agente detenido
POS no abre
Alta latencia
Perdida de paquetes
Rollback ejecutado
Actualizacion fallida
Farmacia de turno con incidente
```

### Grafana

Dashboards:

```txt
Estado general de farmacias
Estado de actualizaciones
Equipos offline
Latencia por zona
Disco critico
Fallos por version
Rollbacks por campana
Farmacias de turno
```

---

## 10. Fase 6 - Gestion de Activos TI

### Objetivo

Centralizar el ciclo de vida de activos tecnologicos.

### Alcance

- Inventario de activos.
- Asignacion a farmacia.
- Actas de entrega.
- Actas de devolucion.
- Actas de traslado.
- Actas de mantenimiento.
- Actas de baja.
- Historial por activo.

### Estados

```txt
DISPONIBLE
ASIGNADO
EN_MANTENIMIENTO
TRASLADADO
DADO_DE_BAJA
PERDIDO
```

---

## 11. Fase 7 - Gestion de Cambios y Base de Conocimiento

### Objetivo

Formalizar cambios operativos y documentar soluciones.

### Gestion de cambios

Estados:

```txt
REQUESTED
UNDER_REVIEW
APPROVED
SCHEDULED
EXECUTED
FAILED
CANCELLED
CLOSED
```

### Base de conocimiento

Contenido:

```txt
Procedimientos de actualizacion
Solucion de errores frecuentes
Guias de soporte por farmacia
Protocolos ante caida de enlace
Protocolos ante rollback
Manuales del agente
```

---

## 12. Fase 8 - Control Remoto Integrado

### Objetivo

Permitir soporte tecnico desde la ficha del equipo.

### Recomendacion

No construir control remoto desde cero en el MVP. Integrar una herramienta existente o corporativa.

### Alcance

- Boton de acceso remoto desde equipo.
- Validacion de permisos.
- Registro de sesion.
- Auditoria.
- Asociacion con incidente o solicitud.

---

## 13. Fase 9 - Dashboard Ejecutivo

### Objetivo

Entregar visibilidad gerencial de disponibilidad, cumplimiento y riesgo.

### KPIs

```txt
% equipos actualizados
% farmacias actualizadas
Tiempo promedio de despliegue
Fallos por campana
Rollbacks por version
Equipos offline
Farmacias con riesgo operativo
Farmacias de turno con incidentes
Cumplimiento antes de 01:00
Versiones POS activas
Disponibilidad promedio de enlace
Equipos con disco critico
Activos por estado
```

---

## 14. MVP Recomendado

El MVP debe incluir:

```txt
API Spring Boot
PostgreSQL
Agente .NET 8 Worker Service
Panel Angular basico
Repositorio ZIP interno
Checksum SHA256
Backup obligatorio
Rollback automatico
Inventario basico
Campanas
Piloto
Grupos trx
Auditoria
Estado por equipo
```

No incluir en MVP:

```txt
Control remoto
Gestion documental avanzada
Base de conocimiento completa
Dashboard ejecutivo avanzado
Integraciones externas complejas
```

---

## 15. Backlog Inicial del MVP

| ID | Historia | Prioridad |
|---|---|---|
| MVP-001 | Como agente, quiero registrarme en el servidor para quedar inventariado. | Alta |
| MVP-002 | Como agente, quiero enviar heartbeat para reportar que estoy online. | Alta |
| MVP-003 | Como administrador, quiero cargar un paquete ZIP del POS. | Alta |
| MVP-004 | Como sistema, quiero calcular SHA256 del paquete para validarlo. | Alta |
| MVP-005 | Como administrador, quiero aprobar una version antes de desplegarla. | Alta |
| MVP-006 | Como administrador, quiero crear una campana de actualizacion. | Alta |
| MVP-007 | Como administrador, quiero asignar equipos o farmacias a una campana. | Alta |
| MVP-008 | Como agente, quiero consultar instrucciones pendientes. | Alta |
| MVP-009 | Como agente, quiero descargar solo paquetes autorizados. | Alta |
| MVP-010 | Como agente, quiero crear backup antes de actualizar. | Alta |
| MVP-011 | Como agente, quiero actualizar la carpeta POS. | Alta |
| MVP-012 | Como agente, quiero validar que el ejecutable principal exista. | Alta |
| MVP-013 | Como agente, quiero hacer rollback si la actualizacion falla. | Alta |
| MVP-014 | Como administrador, quiero ver el estado de actualizacion por equipo. | Alta |
| MVP-015 | Como auditor, quiero consultar eventos de actualizacion. | Media |

---

## 16. Riesgos y Mitigaciones

| Riesgo | Impacto | Mitigacion |
|---|---:|---|
| Saturacion de enlaces | Alto | Limite global y maximo 1 descarga por farmacia |
| Fallo de paquete | Alto | SHA256 obligatorio y aprobacion previa |
| Fallo de actualizacion | Alto | Backup y rollback automatico |
| Equipo apagado | Medio | Actualizacion al encender antes de abrir POS |
| Farmacia de turno afectada | Alto | Prioridad y alertas criticas |
| Falta de disco | Alto | Validacion previa y alerta |
| Agente detenido | Alto | Windows Service con recovery y alerta |
| Manipulacion no autorizada | Alto | Token por equipo, HTTPS y auditoria |

---

## 17. Siguiente Paso Tecnico

Construir la **Fase 0** y comenzar el **MVP** con este orden:

```txt
1. Crear repositorio/estructura del proyecto Farmamia Operations Center.
2. Crear API Spring Boot base.
3. Crear migraciones PostgreSQL iniciales.
4. Crear endpoints de agente: register, heartbeat, instructions, events.
5. Crear Worker Service .NET con registro y heartbeat.
6. Crear carga de paquetes ZIP y checksum.
7. Crear primer flujo de actualizacion local controlada.
```
