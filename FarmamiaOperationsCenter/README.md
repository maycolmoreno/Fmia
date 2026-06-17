# CLAUDE.md — Farmamia Operations Center
## Propósito del proyecto
Plataforma on-premise para actualizar de forma centralizada, segura y auditable el POS
de 1800-1900 equipos Windows distribuidos en 645 farmacias.
Ejecutable POS: `Zabyca.Pos.Desktop.exe`
Ruta: `C:\Program Files (x86)\Farmamia Cia Ltda - Elipsys\Cliente`
Peso del paquete: ~90 MB (ZIP)
---
## Estructura del monorepo
```
farmamia-operations-center/
├── backend/          # Spring Boot 3.x — Java 21 — Maven
├── agent/            # Agente Windows — .NET 8 — C#
├── frontend/         # Angular — panel de administración
├── docs/             # Contratos API, decisiones técnicas, actas
└── CLAUDE.md
```
---
## Stack técnico
| Componente | Tecnología | Notas |
|---|---|---|
| Backend API | Spring Boot 3.x + Java 21 | Virtual threads habilitados |
| Base de datos | PostgreSQL | Migraciones con Flyway |
| Agente Windows | .NET 8 — C# | Servicio Windows, auto-actualización por `.bat` |
| Frontend | Angular | Panel mínimo en v1.0 |
| Push servidor→agente | SSE (Server-Sent Events) | Agente mantiene conexión SSE abierta |
| Comunicación agente→servidor | REST HTTP | Toda la comunicación saliente del agente |
| Despliegue | Directo en servidor Windows/Linux | Sin Docker en v1.0 |
| Conectividad | Proxy Inverso en DMZ | URL base única para el agente |
---
## Configuración Spring Boot
```properties
# application.properties — obligatorio
spring.threads.virtual.enabled=true
spring.datasource.url=jdbc:postgresql://localhost:5432/farmamia_pos
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
```
---
## Reglas de negocio críticas — leer antes de tocar campañas
### Límite mensual de campañas
- Máximo **3 campañas por mes calendario** a nivel global del sistema.
- Una campaña no puede lanzarse si hay otra en curso (estado `EN_CURSO`).
- Una campaña está cerrada solo cuando **todos** sus equipos tienen estado `EXITOSO` o `ROLLBACK`.
- Los reintentos por rollback **no** cuentan como campaña nueva.
### Flujo de autorización
1. Administrador TI crea la campaña → estado `BORRADOR`.
2. Administrador TI la envía a aprobación → estado `PENDIENTE_APROBACION`.
3. Jefe de Operaciones aprueba → estado `AUTORIZADA`.
4. Administrador TI lanza → estado `EN_CURSO`.
- Sin aprobación del Jefe de Operaciones, la campaña **nunca** puede pasar a `EN_CURSO`.
### Flujo piloto → expansión
1. La campaña arranca en un subconjunto piloto de farmacias.
2. El administrador revisa resultados del piloto y aprueba expansión.
3. El servidor hace **push activo vía SSE** a los equipos no piloto.
4. Los equipos piloto usaron **pull por heartbeat**; los de expansión usan **push SSE**.
### Versión por farmacia
- Todos los equipos de una misma farmacia reciben **siempre la misma versión** en la misma campaña.
- Una farmacia tiene entre 2 y 5 equipos.
- Solo **1 descarga activa por farmacia** a la vez — el servidor serializa por `farmacia_id`.
### Equipos offline
- Si un equipo estaba offline durante la ventana nocturna, se actualiza **de inmediato al reconectarse**, sin esperar la próxima noche.
- La campaña permanece en estado `EN_CURSO` hasta que ese equipo resuelva.
---
## Estados de campaña
```
BORRADOR → PENDIENTE_APROBACION → AUTORIZADA → EN_CURSO → CERRADA
                                                    ↓
                                          PENDIENTE_OFFLINE (equipos que no respondieron)
                                                    ↓
                                               CERRADA
```
## Estados de equipo dentro de una campaña
```
PENDIENTE → DESCARGANDO → ACTUALIZANDO → VALIDANDO → EXITOSO
                                                    ↓
                                                ROLLBACK
```
---
## Flujo de validación POS — lógica del agente
```
1. Backup: renombrar Cliente/ → Cliente.bak/
2. Extraer ZIP en Cliente/
3. Intentar: Zabyca.Pos.Desktop.exe --smoke-test
   - Exit Code == 0 → EXITOSO
   - Exit Code != 0 → ROLLBACK
4. Si --smoke-test no disponible (versión antigua):
   - Lanzar ejecutable y esperar 20 segundos
   - Proceso activo al cumplirse → EXITOSO
   - Proceso muerto antes → ROLLBACK
5. En caso de ROLLBACK: restaurar Cliente.bak/
6. Reportar al servidor: { resultado, metodo, version, timestamp, causa }
7. Solo eliminar Cliente.bak/ cuando el servidor confirme recepción del reporte EXITOSO
```
---
## Auto-actualización del agente
El agente no puede actualizarse a sí mismo mientras está corriendo.
El `.bat` externo sigue esta secuencia:
```
1. Descargar FarmamiaAgent_new.exe
2. Detener servicio Windows del agente
3. Renombrar FarmamiaAgent.exe → FarmamiaAgent.old
4. Renombrar FarmamiaAgent_new.exe → FarmamiaAgent.exe
5. Iniciar servicio
6. Si el servicio no levanta en 30 segundos → restaurar FarmamiaAgent.old e iniciar
```
El servidor tiene un endpoint de versión del agente **separado** del endpoint de versión POS.
---
## Contrato API — endpoints principales
Todos los endpoints son relativos a la URL base configurada en el agente.
El agente nunca conoce la IP del servidor — solo la URL base del proxy.
### Agente → Servidor (REST)
| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/v1/agentes/registro` | Registro inicial del equipo |
| `POST` | `/api/v1/agentes/{id}/heartbeat` | Señal de vida periódica |
| `GET` | `/api/v1/agentes/{id}/instrucciones` | Consultar instrucción activa (pull) |
| `POST` | `/api/v1/agentes/{id}/eventos` | Reportar evento (descarga, validación, rollback) |
| `GET` | `/api/v1/paquetes/{campaniaId}/descarga` | Descargar ZIP de actualización |
### Servidor → Agente (SSE)
| Ruta | Descripción |
|---|---|
| `GET /api/v1/agentes/{id}/notificaciones` | Canal SSE persistente — recibe instrucciones push |
### Panel → Servidor (REST)
| Método | Ruta | Descripción |
|---|---|
| `POST` | `/api/v1/campanias` | Crear campaña |
| `PUT` | `/api/v1/campanias/{id}/aprobar` | Aprobar campaña (rol Jefe Operaciones) |
| `PUT` | `/api/v1/campanias/{id}/lanzar` | Lanzar campaña (rol Administrador TI) |
| `PUT` | `/api/v1/campanias/{id}/expandir` | Aprobar expansión de piloto a total |
| `GET` | `/api/v1/campanias/{id}/estado` | Estado de campaña y equipos |
| `GET` | `/api/v1/equipos` | Listar equipos con versión y estado |
---
## Modelo de datos — tablas principales
```sql
farmacias         (id, nombre, codigo, direccion, es_turno)
equipos           (id, farmacia_id, hostname, mac, version_pos_actual, estado_agente, ultimo_heartbeat)
versiones_pos     (id, numero_version, ruta_zip, checksum_sha256, fecha_subida, subido_por)
versiones_agente  (id, numero_version, ruta_exe, checksum_sha256, fecha_subida)
campanias         (id, version_pos_id, estado, mes_calendario, es_piloto_activo, creado_por, aprobado_por, fecha_lanzamiento)
campania_equipos  (campania_id, equipo_id, estado, metodo_validacion, causa_fallo, fecha_resolucion)
eventos_auditoria (id, equipo_id, campania_id, tipo_evento, detalle, timestamp)
```
---
## Secuencia de desarrollo — v1.0
Seguir este orden estrictamente. No pasar al siguiente paso sin que el anterior funcione en laboratorio.
```
1. Contrato API + esquema SQL (Fase 0) ← ESTAMOS AQUÍ
2. Modelo PostgreSQL + Flyway migrations
3. API Spring Boot: registro, heartbeat, instrucciones, eventos
4. Agente .NET: registro y heartbeat
5. Agente .NET: descarga ZIP + validación SHA256
6. Agente .NET: backup + reemplazo de archivos
7. Agente .NET: validación POS + rollback
8. Agente .NET: canal SSE (recibir push)
9. Panel Angular mínimo: equipos, campañas, estado
10. Piloto controlado en laboratorio (1 equipo)
11. Piloto real (5 farmacias)
12. Expansión a 100 equipos
```
---
## Convenciones de código
### Backend (Java)
- Paquete base: `com.farmamia.posupdate`
- Estructura por capa: `controller`, `service`, `repository`, `domain`, `dto`, `event`
- Los estados de campaña y equipo son `enum` con transiciones validadas en `service`, nunca en el controlador
- Toda operación que modifique estado de campaña va dentro de `@Transactional`
- Los eventos SSE se publican desde `ApplicationEventPublisher`, no desde el servicio directamente
- Flyway: archivos en `resources/db/migration/` con formato `V{n}__{descripcion}.sql`
### Agente (.NET)
- El agente es un `BackgroundService` registrado como servicio Windows
- Toda comunicación con el servidor usa `HttpClient` con `IHttpClientFactory`
- La URL base del servidor se lee de `appsettings.json`, nunca hardcodeada
- El canal SSE se maneja en un `Task` separado con `CancellationToken`
- Los logs van a archivo rotativo en `C:\ProgramData\Farmamia\Agent\logs\`
### General
- Sin secretos en el repositorio — usar variables de entorno o archivos `.env` ignorados por git
- Cada PR debe tener tests para la lógica de negocio modificada
- Los cambios al contrato API deben actualizarse en `docs/contrato-api.yaml` antes de implementar
---
## Lo que NO entra en v1.0
No implementar esto aunque parezca urgente:
- Gestión de activos / actas
- Base de conocimiento
- Control remoto integrado
- Dashboard ejecutivo con métricas avanzadas
- Sistema de tickets
- Reemplazo de Active Directory
- Grafana / observabilidad (eso es v2.0)
Pregunta de validación antes de agregar cualquier feature:
> ¿Esto ayuda a actualizar, controlar, monitorear o recuperar el POS de las farmacias de forma más segura?
Si la respuesta es no → se deja para una fase posterior.
---
## Indicador de éxito — v1.0
```
Actualizar al menos 100 equipos piloto sin intervención manual directa
y con trazabilidad completa de cada equipo, versión y resultado.
```