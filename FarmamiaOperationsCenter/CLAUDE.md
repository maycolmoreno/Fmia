# CLAUDE.md — Farmamia Operations Center
> Última actualización: 2026-06-22
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
├── backend/          # Spring Boot 3.x — Java 21 — Maven — arquitectura hexagonal
├── agent/            # Agente Windows — .NET 8 — C# — Windows Service
├── frontend/         # Angular 18 standalone — panel NOC + administración
├── monitoring/       # Configuración Grafana / Prometheus (pendiente)
├── infraestructura/  # Docker Compose, scripts de despliegue
├── docs/             # Contratos API, decisiones técnicas, diseño
│   └── DISENO_NOC.md # Sistema de diseño del panel NOC ← LEER PARA UI
├── herramientas/     # Scripts de utilidad
└── CLAUDE.md         # Este archivo
```

---

## Stack técnico

| Componente | Tecnología | Estado |
|---|---|---|
| Backend API | Spring Boot 3.x + Java 21 + Virtual Threads | ✅ Operativo |
| Base de datos | PostgreSQL + Flyway migrations | ✅ Operativo |
| Agente Windows | .NET 8 — C# — Windows Service | 🔄 50% — falta ejecutar instalador real |
| Frontend | Angular 18 standalone components | ✅ 72% — NOC funcional |
| Push servidor→agente | SSE (Server-Sent Events) | ✅ Implementado |
| Comunicación agente→servidor | REST HTTP | ✅ Implementado |
| Monitoreo | Grafana + Prometheus | ❌ Pendiente |

---

## Estado actual del proyecto (Junio 2026)

### Frontend Angular — 72%
**Implementado:**
- Sistema de autenticación admin (login/logout/guardias de ruta)
- Layout NOC: sidebar 3 grupos, sub-tabs dentro de secciones
- Theme toggle ☀️/🌙 con localStorage y CSS variables full
- Reloj en tiempo real HH:MM:SS en el header
- KPI bar con 6 indicadores en el header (Farmacias OK/Riesgo/Críticas/Alertas/POS/Turno)
- Drawer lateral "Alarm & Correlation Center" (panel deslizante derecha)
- Dashboard NOC con zonas: Crítico, Red, POS, Campaña, Alertas
- Mapa SVG de Ecuador con nodos hexagonales por ciudad
- Tabla de campañas con filtros + filas expandibles (botón +)
- Todos los componentes usan CSS variables (dark/light theme-aware)

**Pendiente:**
- Coordenadas lat/lng en modelo `Farmacia` (el mapa usa `ciudad` como proxy)
- Tests E2E / unitarios (cero cobertura)
- Manejo de errores de red / estado offline
- Responsive mobile < 500px

### Backend Spring Boot — 60%
**Implementado:**
- Arquitectura hexagonal completa (dominio/aplicación/infraestructura/presentación)
- 13 controladores: Agente, Alertas, Auditoria, Autenticacion, Dashboard, Despliegues, Equipos, EventosActualizacion, GruposTrx, OrquestacionDespliegues, Paquetes, Salud, Sucursales, UsuariosAdministrativos
- Canal SSE para push a agentes
- SNMP polling (infraestructura existe)
- Retención/limpieza de datos

**Pendiente:**
- Campo `lat`/`lng` en entidad `Farmacia`/`Sucursal` (desbloquea mapa real)
- Caché del endpoint `/api/noc/estado-operacional` (se recalcula cada 30s × N usuarios)
- Correlación automática de alertas
- Tests de integración en casos de uso
- Rate limiting en endpoints públicos

### Agente .NET 8 — 50%
**Implementado:**
- Heartbeat al backend
- Inicialización y registro del agente
- SSE consumer para instrucciones push
- Modelos de dominio: inventario, credenciales, respaldo, cola de eventos
- Puertos/abstracciones definidos (interfaces)

**Pendiente (crítico):**
- `IActualizadorPos` — implementación real de ejecución del instalador POS
- `IAvisadorUsuario` — aviso al operador del POS antes de actualizar
- Rollback automático (modelo `RespaldoPos` existe, falta lógica)
- Reconexión SSE con backoff exponencial
- Tests en `Farmamia.Agent.Tests`
- Scripts de instalación como Windows Service

### Infraestructura / Monitoring — 30%
**Pendiente:**
- Grafana dashboards reales (el frontend tiene links pero no hay dashboards)
- `prometheus.yml` funcional
- Docker Compose completo (backend + BD + Grafana + Prometheus)
- Variables de entorno para producción

---

## Próximas prioridades (en orden)

```
1. 🔴 lat/lng en Farmacia — backend (migración Flyway) + frontend (mapa real)
2. 🔴 IActualizadorPos en agente — core del producto
3. 🔴 Rollback automático en agente
4. 🔴 Caché NOC estado operacional en backend
5. 🟡 Tests de integración backend (casos de uso críticos)
6. 🟡 Grafana dashboards funcionales
7. 🟡 Correlación de alertas
8. ⚪ Mobile responsive
9. ⚪ Rate limiting
```

---

## Reglas de negocio críticas — NO modificar sin leer esto

### Límite mensual de campañas
- Máximo **3 campañas por mes calendario** a nivel global.
- No se puede lanzar si hay otra en estado `EN_CURSO`.
- Una campaña cierra solo cuando **todos** los equipos son `EXITOSO` o `ROLLBACK`.
- Los reintentos de rollback **no** cuentan como campaña nueva.

### Flujo de autorización obligatorio
```
BORRADOR → PENDIENTE_APROBACION → AUTORIZADA → EN_CURSO → CERRADA
                                                    ↓
                                       PENDIENTE_OFFLINE → CERRADA
```
- Sin aprobación del Jefe de Operaciones → la campaña **nunca** pasa a `EN_CURSO`.

### Flujo piloto → expansión
1. Arranca en subconjunto piloto (pull por heartbeat)
2. Admin revisa y aprueba expansión
3. Servidor hace **push SSE** al resto de equipos

### Versión por farmacia
- Todos los equipos de la misma farmacia reciben **siempre la misma versión**.
- Solo **1 descarga activa por farmacia** a la vez.

### Equipos offline
- Si un equipo estaba offline en la ventana nocturna → se actualiza **al reconectarse**.

### Estados de equipo en campaña
```
PENDIENTE → DESCARGANDO → ACTUALIZANDO → VALIDANDO → EXITOSO
                                                    ↓
                                                ROLLBACK
```

---

## Flujo de validación POS (lógica del agente)

```
1. Backup: renombrar Cliente/ → Cliente.bak/
2. Extraer ZIP en Cliente/
3. Intentar: Zabyca.Pos.Desktop.exe --smoke-test
   - Exit 0  → EXITOSO
   - Exit ≠0 → ROLLBACK
4. Si --smoke-test no disponible (versión antigua):
   - Lanzar ejecutable, esperar 20 segundos
   - Proceso activo → EXITOSO / Proceso muerto → ROLLBACK
5. En ROLLBACK: restaurar Cliente.bak/
6. Reportar al servidor: { resultado, método, versión, timestamp, causa }
7. Eliminar Cliente.bak/ solo tras confirmación del servidor de EXITOSO
```

---

## Contrato API — endpoints principales

### Agente → Servidor (REST)
| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/v1/agentes/registro` | Registro inicial |
| `POST` | `/api/v1/agentes/{id}/heartbeat` | Señal de vida |
| `GET` | `/api/v1/agentes/{id}/instrucciones` | Pull de instrucción activa |
| `POST` | `/api/v1/agentes/{id}/eventos` | Reporte de evento |
| `GET` | `/api/v1/paquetes/{campaniaId}/descarga` | Descarga ZIP |

### Servidor → Agente (SSE)
| Ruta | Descripción |
|---|---|
| `GET /api/v1/agentes/{id}/notificaciones` | Canal SSE persistente |

### Panel → Servidor (REST)
| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/v1/campanias` | Crear campaña |
| `PUT` | `/api/v1/campanias/{id}/aprobar` | Aprobar (Jefe Operaciones) |
| `PUT` | `/api/v1/campanias/{id}/lanzar` | Lanzar (Admin TI) |
| `PUT` | `/api/v1/campanias/{id}/expandir` | Aprobar expansión piloto |
| `GET` | `/api/v1/campanias/{id}/estado` | Estado + equipos |
| `GET` | `/api/v1/noc/estado-operacional` | Dashboard NOC (30s refresh) |

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
- Estados de campaña/equipo: `enum` con transiciones validadas en caso de uso, nunca en controlador
- Toda operación que modifique estado de campaña: `@Transactional`
- Eventos SSE: publicar desde `ApplicationEventPublisher`
- Flyway: `resources/db/migration/V{n}__{descripcion}.sql`

### Agente .NET
- `BackgroundService` registrado como servicio Windows
- `HttpClient` via `IHttpClientFactory`
- URL base del servidor desde `appsettings.json` — nunca hardcodeada
- Canal SSE en `Task` separado con `CancellationToken`
- Logs en `C:\ProgramData\Farmamia\Agent\logs\`

### General
- Sin secretos en el repositorio (`.env` ignorado por git)
- Cada cambio de contrato API → actualizar `docs/contrato-api.yaml` antes de implementar
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
