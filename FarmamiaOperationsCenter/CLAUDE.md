# CLAUDE.md — Farmamia Operations Center
> Última actualización: 2026-07-05
> Este archivo es la fuente de verdad para Claude. Leerlo completo antes de tocar cualquier archivo.

---

## Propósito del proyecto

Plataforma on-premise para actualizar de forma centralizada, segura y auditable el POS
de **1800-1900 equipos Windows** distribuidos en **645 farmacias** de Ecuador.

- Ejecutable POS: `Zabyca.Pos.Desktop.exe`
- Ruta en equipo: `C:\Program Files (x86)\Farmamia Cia Ltda - Elipsys\Cliente`
- Peso paquete: ~90 MB (ZIP)
- El sistema también funciona como **NOC (Network Operations Center)** de monitoreo en tiempo real.

---

## Estructura del monorepo

```
FarmamiaOperationsCenter/
├── backend/          # Spring Boot 3.3.5 — Java 21 — Maven — arquitectura hexagonal
├── agent/            # Agente Windows — .NET 8 — C# — Windows Service
│   ├── Farmamia.Agent/                Servicio real
│   ├── Farmamia.Agent.Tests/          Pruebas unitarias
│   ├── Farmamia.Agent.Tests.PosSimulado/  Ejecutable de prueba (doble uso: fixture de tests y POS de laboratorio real)
│   └── FarmamiaUpdater/               CLI manual para diagnostico/instalacion offline
├── frontend/         # Angular 18 standalone — panel NOC + administración
├── monitoring/       # Prometheus + Grafana (config y 3 dashboards reales y verificados)
├── infraestructura/  # Docker Compose (solo MVP local), scripts de despliegue
├── docs/             # Contratos API, decisiones técnicas, diseño
│   └── DISENO_NOC.md # Sistema de diseño del panel NOC ← LEER PARA UI
├── herramientas/     # Scripts de utilidad
│   └── laboratorio-pos/  # Scripts para probar el agente (aviso/cierre POS) en maquina real
└── CLAUDE.md         # Este archivo
```

---

## Stack técnico

| Componente | Tecnología | Estado |
|---|---|---|
| Backend API | Spring Boot 3.3.5 + Java 21 | ✅ Operativo (sin Virtual Threads configurados — verificado que no hay `spring.threads.virtual.enabled` ni executor de hilos virtuales en el código, pese a documentación anterior) |
| Base de datos | PostgreSQL + Flyway (24 migraciones) | ✅ Operativo |
| Agente Windows | .NET 8 — C# — Windows Service | 🔄 ~65-70% — validación real de actualización y aviso al operador implementados; falta verificar en máquina real (ver `herramientas/laboratorio-pos/`) |
| Frontend | Angular 18 standalone components | ✅ ~70% — NOC funcional; 0% cobertura de pruebas confirmada |
| Push servidor→agente | SSE (Server-Sent Events), con reconexión y backoff exponencial | ✅ Implementado |
| Comunicación agente→servidor | REST HTTP + cola SQLite (outbox) con reintento | ✅ Implementado |
| Monitoreo | Prometheus + Grafana | 🔄 Config y 3 dashboards reales verificados contra métricas y esquema reales; falta Alertmanager/canal de notificación |

---

## Estado actual del proyecto (Julio 2026)

### Frontend Angular — ~70%
**Implementado:**
- Sistema de autenticación admin (login/logout/guardias de ruta)
- Layout NOC: sidebar 3 grupos, sub-tabs dentro de secciones
- Theme toggle ☀️/🌙 con localStorage y CSS variables full
- Reloj en tiempo real HH:MM:SS en el header
- KPI bar con 6 indicadores en el header (Farmacias OK/Riesgo/Críticas/Alertas/POS/Turno)
- Drawer lateral "Alarm & Correlation Center" (panel deslizante derecha) — **hoy es solo agrupación visual por farmacia/equipo; no consume aún el `correlationId` real que el backend ya expone** (ver alertas más abajo)
- Dashboard NOC con zonas: Crítico, Red, POS, Campaña, Alertas
- Mapa SVG de Ecuador con nodos hexagonales por ciudad
- Tabla de campañas con filtros
- Todos los componentes usan CSS variables (dark/light theme-aware) — con excepciones puntuales (`kpi-card`, `alert-list`, `app-card`, `noc-table` en `componentes-ui/` tienen colores hardcodeados)

**Pendiente:**
- Coordenadas lat/lng en `Farmacia`/`Sucursal` — **ya implementado en el backend** (`PUT /api/branches/{id}/coordenadas`), pero el mapa SVG sigue usando `ciudad` como proxy a propósito: se verificó que el SVG es un dibujo esquemático, no una proyección geográfica real, y migrar a lat/lng real requiere decidir si se rediseña el SVG o se reemplaza por un mapa real (Leaflet/MapLibre) — decisión de diseño pendiente, no de datos
- Tests E2E / unitarios (cero cobertura, confirmado — ni un solo `.spec.ts` bajo `frontend/src`)
- Manejo de errores de red / estado offline (parcial)
- Responsive mobile < 500px
- Consumir `correlationId` en el "Alarm & Correlation Center" para mostrar agrupación real por incidente (el backend ya lo expone)
- ~1000 líneas de CSS heredado/duplicado en `app.component.css` (pre-theming), sin usar

### Backend Spring Boot — ~65-70%
**Implementado:**
- Arquitectura hexagonal (dominio/aplicación/infraestructura/presentación)
- 18 controladores (no 13: Agente, Alertas, Auditoria, Autenticacion, Dashboard, Despliegues, Equipos, EventosActualizacion, GruposTrx, OrquestacionDespliegues, Paquetes, Salud, SeguridadAdministrativa, SseNoc, Sucursales, UsuariosAdministrativos, Webhook, + el advice de excepciones)
- Canal SSE para push a agentes y al panel NOC
- SNMP polling con corrección de overflow de contadores
- Motor de orquestación de despliegues: oleadas piloto→expansión, lease de instrucciones sin duplicados, reintento con backoff, auto-pausa por umbral de fallos, exclusión por turno nocturno
- Retención/limpieza de datos
- **Reglas de negocio de campaña**: tope de 3 campañas por mes calendario, exclusividad de campaña activa al lanzar, farmacia sin más de una actualización activa a la vez (garantiza también versión única por farmacia) — ver detalle en "Reglas de negocio críticas"
- **Bloqueo optimista** (`@Version`) en `DespliegueEntidad`, `ObjetivoDespliegueEntidad`, `EstadoControlDespliegueEntidad`
- **Coordenadas lat/lng** en `Sucursal` (columnas nullable + `PUT /api/branches/{id}/coordenadas`)
- **Caché** en `/api/dashboard/resumen-noc` (Caffeine, TTL configurable, default 15s — antes recalculaba todo en cada poll de cada usuario)
- **Deduplicación y correlación automática de alertas de red**: no se crean alertas duplicadas para el mismo equipo/farmacia+tipo mientras una siga activa; alertas de red relacionadas de la misma farmacia se enlazan (`correlationId`) a la primera que abrió el incidente
- Seguridad: BCrypt, bloqueo de cuenta tras 5 intentos fallidos, tokens de agente hasheados, sin secretos hardcodeados en `application.yml` (requiere `FARMAMIA_DB_PASSWORD`/`FARMAMIA_JWT_SECRET` sin valor por defecto), autorización con default seguro (`anyRequest().authenticated()`, ya no `permitAll()`)

**Pendiente:**
- Rate limiting en endpoints públicos
- Alertmanager o alerting nativo de Grafana (las reglas ya definidas en `monitoring/alert_rules.yml` se evalúan pero no notifican a nadie)
- Ampliar tests de integración con Testcontainers (varias ya existen; requieren Docker para correr)
- Reconciliar `docs/API_CONTRACT_MVP.md` con los endpoints reales (esquemas de URL distintos, ver sección de contrato más abajo)

### Agente .NET 8 — ~65-70%
**Implementado:**
- Heartbeat al backend
- Inicialización y registro del agente
- SSE consumer con **reconexión y backoff exponencial** (5s → 2min)
- Modelos de dominio: inventario, credenciales, respaldo, cola de eventos
- **Cola de eventos SQLite (outbox)** con estados PENDING/SENDING/SENT/FAILED/DEAD_LETTER, recuperación de envíos interrumpidos
- **Validación real de actualización POS** (`ActualizadorPosZip.ValidarAsync`): intenta `--smoke-test` con timeout configurable; si no responde (versión antigua), cae al método de respaldo (lanzar y confirmar que sigue vivo 20s); reporta el método usado (`SMOKE_TEST` / `PROCESO_VIVO_20S` / `EJECUTABLE_NO_ENCONTRADO`)
- **Rollback automático** end-to-end, con pruebas
- **Aviso al operador** vía `WTSSendMessage` (API de Terminal Services) a la sesión interactiva activa — con fallback a archivo si no hay sesión — **pendiente de verificar en máquina real** que se muestra correctamente desde el contexto de Sesión 0 del servicio
- Scripts de instalación como Windows Service (`herramientas/instalar-servicio.ps1`, `publicar-agente.ps1`, `instalar-agente-laptop.ps1`, `diagnosticar-agente-laptop.ps1`)
- `herramientas/laboratorio-pos/`: scripts para reproducir el riesgo de Sesión 0 en una máquina real sin depender del backend (POS de laboratorio real + tarea programada como SYSTEM)

**Pendiente (crítico):**
- **Verificar en máquina real** que `WTSSendMessage` muestra el aviso y que `ProcesoPosWindows` puede cerrar/reabrir el POS desde el contexto de Sesión 0 del servicio (herramientas ya listas en `herramientas/laboratorio-pos/probar-sesion0-real.ps1`, falta ejecutarlas)
- Backup es por copia completa, no por rename (funciona, pero duplica I/O en paquetes de ~90MB)

### Infraestructura / Monitoring — ~40%
**Implementado:**
- CI (`mvp-verification.yml`) corregido — antes apuntaba a directorios que ya no existían (`backend-api`/`admin-panel`/`windows-agent`)
- Scripts de `herramientas/verificacion/` corregidos, con avisos (no fallos duros) para artefactos aún no construidos
- Sin contraseñas hardcodeadas en `docker-compose.mvp.yml`/`datasources.yml`/`application.yml` (antes se repetía la misma contraseña en 3 archivos)
- Prometheus + Grafana: `prometheus.yml`, `alert_rules.yml` y los 3 dashboards (`noc-overview`, `red-farmacias`, `farmacia-enlace`) verificados contra métricas Micrometer reales y columnas de esquema reales — más avanzado de lo que se documentaba antes

**Pendiente:**
- Alertmanager / canal de notificación real (email, Slack, webhook)
- Dockerfiles para backend/frontend (hoy solo Postgres/Prometheus/Grafana corren en Docker; backend/frontend corren directo en el host)
- Variables de entorno y `docker-compose` separados para producción
- Runbook operativo escrito (referenciado antes en una ruta externa al repo que nunca existió)

---

## Próximas prioridades (en orden)

```
1. 🔴 Verificar en máquina real: WTSSendMessage + cierre/reapertura POS desde Sesión 0
   (herramientas/laboratorio-pos/probar-sesion0-real.ps1 ya está listo)
2. 🟡 Frontend: consumir correlationId real en el "Alarm & Correlation Center"
3. 🟡 Alertmanager o alerting nativo de Grafana (canal de notificación real)
4. 🟡 Tests E2E / unitarios en frontend (cero cobertura)
5. 🟡 Dockerfiles + docker-compose de despliegue productivo
6. ⚪ Mobile responsive
7. ⚪ Rate limiting
8. ⚪ Reconciliar docs/API_CONTRACT_MVP.md con los endpoints reales
```

---

## Reglas de negocio críticas — NO modificar sin leer esto

### Límite mensual de campañas
- Máximo **3 campañas por mes calendario** a nivel global. ✅ Implementado (`RepositorioDesplieguesJpaAdaptador.validarTopeMensualCampanias`)
- No se puede lanzar si hay otra campaña en estado activo (`PILOT_RUNNING`/`RUNNING`). ✅ Implementado (`validarSinOtraCampaniaActiva`)
- Una farmacia no puede tener más de una actualización activa a la vez — esto garantiza también que todos sus equipos reciben la misma versión. ✅ Implementado (`validarFarmaciasSinCampaniaActiva`)
- Los reintentos de rollback **no** cuentan como campaña nueva (los reintentos ocurren a nivel de objetivo/equipo, no crean un nuevo `Despliegue`).

### Estados reales de campaña (`DespliegueEntidad`)
```
DRAFT → SCHEDULED / APPROVED → PILOT_RUNNING → RUNNING → COMPLETED
                                      ↓
                                   PAUSED → (reanudar)
                                      ↓
                                  CANCELLED
```
**Nota:** los nombres en español documentados en versiones anteriores de este archivo (BORRADOR/PENDIENTE_APROBACION/AUTORIZADA/EN_CURSO/CERRADA/PENDIENTE_OFFLINE) no corresponden a los strings reales del código. Los estados reales son los de arriba (en inglés); no existe un estado `PENDIENTE_OFFLINE`. Sin aprobación (`aprobar`) un despliegue no puede `lanzar()`.

### Flujo piloto → expansión
1. Arranca en subconjunto piloto (pull por heartbeat, agrupado por oleadas)
2. Admin revisa y aprueba expansión (`expand`/`expandir`)
3. Servidor hace **push SSE** a los equipos conectados del resto de oleadas; los desconectados lo reciben por pull en su próximo heartbeat/reconexión (lease-based, sin duplicados)

### Versión por farmacia
- Todos los equipos de la misma farmacia reciben **siempre la misma versión**. ✅ Implementado
- Solo **1 actualización activa por farmacia** a la vez. ✅ Implementado
- (Ambas reglas se implementan como una sola validación: una farmacia con un objetivo no-final de otra campaña bloquea la creación de una nueva sobre ella.)

### Equipos offline
- Si un equipo estaba offline en la ventana nocturna → se actualiza **al reconectarse** (instrucción queda `AUTHORIZED` en BD independientemente de conectividad SSE).

### Estados reales de equipo en campaña (`ObjetivoDespliegueEntidad`)
```
PENDING → AUTHORIZED → COMPLETED
                ↓
            FAILED → ROLLBACK_COMPLETED / ROLLBACK_FAILED
                ↓
             SKIPPED
```
**Nota:** no hay estados discretos DESCARGANDO/ACTUALIZANDO/VALIDANDO en el backend — solo un campo `download_progress` (porcentaje 0-100). Los nombres reales están en inglés.

### Correlación de alertas
- Alertas de red relacionadas de la misma farmacia (`NETWORK_LINK_DOWN`, `LINK_DOWN`, `LATENCIA_ALTA`, `ROUTER_REINICIADO`, `VPN_CAIDA`, `HIGH_CPU_USAGE`, `LOW_MEMORY`, `NETWORK_EVENT`) se enlazan automáticamente (`correlationId`) a la primera que abrió el incidente, sin importar cuál de los dos productores (SNMP o Alertmanager) la generó primero.
- Alertas de despliegue (`UPDATE_FAILED`, `ROLLBACK_FAILED`, `ROLLBACK_COMPLETED`) **no** se correlacionan con alertas de red — no comparten causa raíz.
- No se crea una alerta duplicada del mismo tipo para el mismo equipo/farmacia mientras una ya esté activa (`OPEN`/`ACKNOWLEDGED`).

---

## Flujo de validación POS (lógica del agente)

```
1. Backup: copiar Cliente/ a Backups/{version}-{timestamp}/ (mantiene ultimas 3 versiones;
   es una copia completa, no un rename)
2. Extraer ZIP en Cliente/
3. Validar:
   a. Intentar Zabyca.Pos.Desktop.exe --smoke-test (timeout configurable, default 20s)
      - Exit 0  → EXITOSO (método SMOKE_TEST)
      - Exit ≠0 → ROLLBACK (método SMOKE_TEST)
   b. Si no termina dentro del timeout (versión antigua sin soporte del flag):
      se mata el proceso y se cae al método de respaldo:
      - Lanzar ejecutable, esperar 20 segundos (configurable)
      - Proceso activo → EXITOSO / Proceso muerto → ROLLBACK (método PROCESO_VIVO_20S)
   c. Si el ejecutable no existe tras aplicar el paquete → ROLLBACK (método EJECUTABLE_NO_ENCONTRADO)
4. En ROLLBACK: restaurar el backup
5. Reportar al servidor: { resultado, versión, mensaje (incluye el método usado) }
6. Aviso al operador antes de actualizar: WTSSendMessage a la sesión interactiva activa,
   con fallback a archivo si no hay sesión — pendiente de verificar en máquina real
7. Los backups se conservan por cantidad (últimos 3), no hay un paso de eliminación
   condicionado a la confirmación del servidor
```

---

## Contrato API — endpoints reales

> `docs/API_CONTRACT_MVP.md` documenta un esquema de URLs distinto (sin prefijo de versión) al de
> este archivo en versiones anteriores (`/api/v1/...`). Ninguno de los dos coincidía con el código.
> Lo de abajo son las rutas reales verificadas en los controladores.

### Agente → Servidor (REST) — base `/api/agent`
| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/agent/register` | Registro inicial |
| `POST` | `/api/agent/heartbeat` | Señal de vida |
| `GET` | `/api/agent/{id}/instructions` | Pull de instrucción activa |
| `POST` | `/api/agent/{id}/events` | Reporte de evento |
| `POST` | `/api/agent/{id}/download-progress` | Progreso de descarga |
| `POST` | `/api/agent/{id}/update-result` | Resultado final de actualización |
| `GET` | `/api/packages/{id}/download` | Descarga de paquete ZIP |

### Servidor → Agente (SSE)
| Ruta | Descripción |
|---|---|
| `GET /api/agent/{id}/notifications` | Canal SSE persistente |

### Panel → Servidor (REST) — base `/api/deployments` (alias `/api/campanas-pos`)
| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/deployments` | Crear campaña |
| `POST` | `/api/deployments/{id}/approve` (alias `/aprobar`) | Aprobar (Jefe Operaciones) |
| `POST` | `/api/deployments/{id}/launch` (alias `/lanzar`) | Lanzar (Admin TI) |
| `POST` | `/api/deployments/{id}/expand` (alias `/expandir`) | Aprobar expansión piloto |
| `GET` | `/api/deployments/{id}/status` | Estado por equipo |
| `GET` | `/api/deployments/{id}/estado-por-farmacia` | Estado agregado por farmacia |
| `GET` | `/api/dashboard/resumen-noc` | Dashboard NOC (cacheado, TTL 15s) |
| `PUT` | `/api/branches/{id}/coordenadas` | Actualizar lat/lng de una farmacia |
| `GET` | `/api/alerts`, `/api/alerts/page` | Listar alertas (con `correlationId`) |
| `POST` | `/api/alerts/{id}/acknowledge`, `/api/alerts/{id}/close` | Gestionar alertas |
| `GET` | `/api/noc/stream` | Canal SSE del panel NOC |

---

## Convenciones de código

### Frontend Angular
- **Solo componentes standalone** — no usar NgModules
- **CSS variables** para todos los colores — ver `docs/DISENO_NOC.md`
- No hardcodear colores hex en componentes — usar `var(--color-*)` con fallback oscuro
- Template en archivo `.html` separado (no inline en `@Component`)
- CSS en archivo `.css` separado
- Rutas de vista: tipo union `Vista` en `app.component.ts`, no usar Angular Router para sub-vistas del NOC
- Verificar con `npx tsc --noEmit` antes de dar una tarea por terminada

### Backend Java
- Paquete base: `com.farmamia.posupdate`
- Capas: `presentacion.controlador` / `aplicacion.casouso` / `dominio.modelo` / `infraestructura`
- Toda operación que modifique estado de campaña: `@Transactional`
- Eventos SSE: publicar desde `ApplicationEventPublisher`
- Flyway: `resources/db/migration/V{n}__{descripcion}.sql` (última: V24)
- Bloqueo optimista (`@Version`) en entidades mutadas concurrentemente por el scheduler y por acciones manuales

### Agente .NET
- `BackgroundService` registrado como servicio Windows
- `HttpClient` via `IHttpClientFactory`
- URL base del servidor desde `appsettings.json`/`config.json` — nunca hardcodeada
- Canal SSE en `Task` separado con `CancellationToken`
- Logs en `RutaAgente\Logs` (no en `C:\ProgramData\...` como se documentaba antes — es intencional y consistente en scripts/config, solo se corrige la documentación)
- Para probar cambios que toquen `IAvisadorUsuario`/`IProcesoPos`: usar `herramientas/laboratorio-pos/` antes de dar el cambio por probado — ningún test unitario puede confirmar comportamiento real de Sesión 0

### General
- Sin secretos en el repositorio — variables de entorno sin valor por defecto en `application.yml` (`FARMAMIA_DB_PASSWORD`, `FARMAMIA_JWT_SECRET`); usar `backend/.env.local.ps1` (gitignored) o `herramientas/desarrollo/arrancar-backend-local.ps1` en local
- Cada cambio de contrato API → actualizar la sección de arriba y `docs/contrato-api.yaml` antes de implementar
- Commit atómico por feature/fix — no mezclar

---

## Lo que NO entra en v1.0

No implementar aunque parezca urgente:
- Gestión de activos / actas
- Base de conocimiento
- Control remoto integrado
- Dashboard ejecutivo con métricas avanzadas
- Sistema de tickets
- Reemplazo de Active Directory

**Pregunta de validación antes de agregar cualquier feature:**
> ¿Esto ayuda a actualizar, controlar, monitorear o recuperar el POS de las farmacias de forma más segura?

Si la respuesta es no → se deja para una fase posterior.

---

## Indicador de éxito v1.0

```
Actualizar al menos 100 equipos piloto sin intervención manual directa
y con trazabilidad completa de cada equipo, versión y resultado.
```

El bloqueador restante para este indicador es la verificación en máquina real de
`herramientas/laboratorio-pos/probar-sesion0-real.ps1` (aviso al operador + cierre/reapertura
del POS desde el contexto de Sesión 0) — todo lo demás en el flujo del agente ya está
implementado y probado a nivel unitario.
