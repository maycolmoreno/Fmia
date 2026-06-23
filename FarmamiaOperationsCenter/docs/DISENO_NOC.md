# DISENO_NOC.md — Sistema de Diseño del Panel NOC
> Farmamia Operations Center — Frontend Angular 18
> Última actualización: 2026-06-22
>
> Leer este documento antes de tocar cualquier archivo CSS o crear un componente nuevo.
> El objetivo es que todo el panel tenga coherencia visual y soporte tema oscuro/claro automáticamente.

---

## Principios de diseño

1. **NOC-first** — el panel es para operadores que miran pantallas largas horas. Prioridad: legibilidad, densidad de información, señales visuales claras.
2. **Theme-aware siempre** — ningún componente tiene colores hardcodeados. Todo usa `var(--color-*)`.
3. **Dark por defecto** — el tema oscuro es el principal. El claro es alternativo.
4. **CSS variables heredadas** — Angular ViewEncapsulation.Emulated no bloquea la herencia de custom properties. Los componentes hijo reciben las variables del padre automáticamente.

---

## Arquitectura del tema

### Dónde viven las variables

En `app.component.css`, bloque `:host`:

```css
/* Tema OSCURO (default) */
:host {
  --color-primary:       #58a6ff;
  --color-primary-soft:  #172340;
  --color-success:       #3fb950;
  --color-success-soft:  #0d2818;
  --color-warning:       #e3b341;
  --color-warning-soft:  #2b1d00;
  --color-danger:        #f85149;
  --color-danger-soft:   #2d0f0f;
  --color-purple:        #a371f7;
  --color-purple-soft:   #1e0a3a;
  --color-background:    #0d1117;
  --color-card:          #161b22;
  --color-card-raised:   #1c2128;
  --color-border:        #30363d;
  --color-text:          #e6edf3;
  --color-muted:         #8b949e;
  --shadow-card:         0 8px 24px rgb(0 0 0 / 40%);
  --color-sidebar:       #0d1117;
  --color-nav-text:      #8b949e;
  --color-nav-active-bg: #1f2937;
  --color-nav-active-border: #374151;
}

/* Tema CLARO — se activa cuando <main class="app-shell tema-claro"> */
:host:has(.tema-claro) {
  --color-primary:       #2563eb;
  --color-primary-soft:  #eff6ff;
  --color-success:       #16a34a;
  --color-success-soft:  #ecfdf5;
  --color-warning:       #d97706;
  --color-warning-soft:  #fffbeb;
  --color-danger:        #dc2626;
  --color-danger-soft:   #fef2f2;
  --color-background:    #f8fafc;
  --color-card:          #ffffff;
  --color-card-raised:   #f1f5f9;
  --color-border:        #e2e8f0;
  --color-text:          #0f172a;
  --color-muted:         #64748b;
  --shadow-card:         0 4px 16px rgb(15 23 42 / 6%);
  --color-sidebar:       #ffffff;
  --color-nav-text:      #334155;
  --color-nav-active-bg: #eff6ff;
  --color-nav-active-border: #bfdbfe;
}
```

### Cómo se activa el toggle

```typescript
// app.component.ts
tema: 'oscuro' | 'claro' = 'oscuro';

toggleTema(): void {
  this.tema = this.tema === 'oscuro' ? 'claro' : 'oscuro';
  localStorage.setItem('noc-tema', this.tema);
}
```

```html
<!-- app.component.html -->
<main class="app-shell" [class.tema-claro]="tema === 'claro'">
```

---

## Paleta de colores por contexto

| Variable | Oscuro | Claro | Uso |
|---|---|---|---|
| `--color-primary` | `#58a6ff` | `#2563eb` | Links, botones primarios, reloj NOC |
| `--color-success` | `#3fb950` | `#16a34a` | Farmacias OK, POS online, completados |
| `--color-warning` | `#e3b341` | `#d97706` | En riesgo, latencia alta, avisos |
| `--color-danger` | `#f85149` | `#dc2626` | Críticas, offline, rollback, alertas |
| `--color-purple` | `#a371f7` | *(igual)* | Campañas activas, turno |
| `--color-background` | `#0d1117` | `#f8fafc` | Fondo general del contenido |
| `--color-card` | `#161b22` | `#ffffff` | Tarjetas, panel drawer, header |
| `--color-card-raised` | `#1c2128` | `#f1f5f9` | Filas hover, inputs, sub-tarjetas |
| `--color-border` | `#30363d` | `#e2e8f0` | Bordes de tarjetas, separadores |
| `--color-text` | `#e6edf3` | `#0f172a` | Texto principal |
| `--color-muted` | `#8b949e` | `#64748b` | Subtítulos, timestamps, etiquetas |

**Regla de oro:** Si hardcodeas un color hex en un componente, estás rompiendo el tema.

---

## Layout general

```
┌─────────────────────────────────────────────────────────────────┐
│ <main class="app-shell [tema-claro]">                           │
│  ┌──────────┐  ┌─────────────────────────────────┐  ┌───────┐  │
│  │ .barra-  │  │ .contenido                      │  │ panel │  │
│  │ lateral  │  │  ┌──────────────────────────┐   │  │-drawer│  │
│  │ 220px    │  │  │ .encabezado              │   │  │ 360px │  │
│  │          │  │  │  fila-principal + kpi-bar│   │  │(fixed)│  │
│  │ Grupos:  │  │  └──────────────────────────┘   │  │       │  │
│  │ Monitoreo│  │  <nav class="sub-tabs-barra">   │  │       │  │
│  │ Despl.   │  │  <section class="vista ...">    │  │       │  │
│  │ Admin    │  │    contenido de la vista         │  │       │  │
│  └──────────┘  └─────────────────────────────────┘  └───────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### Clases de layout principales

| Clase | Archivo | Descripción |
|---|---|---|
| `.app-shell` | `app.component.css` | Grid principal (sidebar + contenido) |
| `.barra-lateral` | `app.component.css` | Sidebar fijo izquierda, 220px |
| `.contenido` | `app.component.css` | Área scrollable derecha |
| `.encabezado` | `app.component.css` | Header con fila principal + KPI bar |
| `.encabezado-fila-principal` | `app.component.css` | Flex: marca + centro (NOC+reloj) + derecha |
| `.kpi-bar` | `app.component.css` | Grid 6 columnas de KPI cards |
| `.sub-tabs-barra` | `app.component.css` | Nav de sub-tabs (Todas/Turno, etc.) |
| `.vista` | `app.component.css` | Contenedor de cada sección |
| `.panel-drawer` | `app.component.css` | Drawer fixed derecha, 360px |
| `.panel-backdrop` | `app.component.css` | Overlay semitransparente |

---

## Componentes UI standalone

Todos en `frontend/src/app/componentes-ui/`

### `stat-card.component.ts`
```html
<app-stat-card label="Farmacias OK" [value]="42" helper="de 80 totales"></app-stat-card>
```
Usa: `var(--color-card)`, `var(--color-text)`, `var(--color-muted)`

### `status-badge.component.ts`
```html
<app-status-badge [status]="'CRITICAL'"></app-status-badge>
```
Variantes CSS: `.success` `.warning` `.danger` `.purple`
Todas usan `var(--color-*-soft)` de fondo y `var(--color-*)` de texto.

### `mapa-ecuador.component.ts`
```html
<app-mapa-ecuador
  [estadoFarmacias]="estadoFarmacias"
  (seleccionarFarmacia)="cambiarVista('equipos')">
</app-mapa-ecuador>
```
- SVG embebido, sin dependencias externas
- Nodos hexagonales agrupados por `ciudad`
- Colores: `hex-ok` / `hex-riesgo` / `hex-critico` / `hex-inactivo`
- Anillo cyan punteado cuando `deTurno > 0`
- **Limitación actual:** usa `ciudad` como proxy. Agregar `lat`/`lng` a `EstadoOperacionalFarmacia` para precisión real.

---

## Convención de colores semánticos en componentes

### Fondo de filas / estados
```css
/* Usar siempre así — nunca hex directo */
.fila-critica    { border-left: 3px solid var(--color-danger, #f85149); }
.fila-riesgo     { border-left: 3px solid var(--color-warning, #e3b341); }
.metrica-alerta  { background: var(--color-danger-soft, #2d0f0f); }
.metrica-aviso   { background: var(--color-warning-soft, #2b1d00); }
.metrica-ok      { background: var(--color-success-soft, #0d2818); }
```

### Badges inline
```css
.badge-critico { background: var(--color-danger-soft);  color: var(--color-danger); }
.badge-riesgo  { background: var(--color-warning-soft); color: var(--color-warning); }
.badge-turno   { background: var(--color-primary-soft); color: var(--color-primary); }
.badge-campana { background: var(--color-purple-soft);  color: var(--color-purple); }
```

---

## Estructura del Dashboard NOC

Componente: `dashboard-noc/dashboard-noc.component.ts`
Zonas (sub-componentes en `dashboard-noc/zonas/`):

| Componente | Datos | Prioridad visual |
|---|---|---|
| `noc-zona-critico` | Farmacias críticas y turno en riesgo | Borde rojo superior |
| `noc-zona-red` | linkDown, vpnDown, latencia alta | Normal |
| `noc-zona-pos` | Total/online/offline POS | Normal |
| `noc-zona-campana` | Campaña activa, progreso % | Normal |
| `noc-zona-alertas` | Alertas operativas recientes | Normal |

El dashboard se refresca cada **30 segundos** automáticamente.

---

## Navegación y sub-tabs

### Grupos del sidebar
```
Monitoreo
  → Dashboard NOC       (vistaActiva: 'dashboard')
  → Farmacias           (vistaActiva: 'equipos')
  → Alertas             (vistaActiva: 'alertas')
  → Agentes             (vistaActiva: 'agentes')

Despliegues
  → Campañas POS        (vistaActiva: 'despliegues')
  → Grupos TRX          (vistaActiva: 'gruposTrx')
  → Versiones POS       (vistaActiva: 'paquetes')

Administración
  → Auditoria           (vistaActiva: 'auditoria')
  → Usuarios            (vistaActiva: 'usuarios')
  → Configuración       (vistaActiva: 'seguridad')
```

### Sub-tabs por sección

| Vista | Sub-tabs | Variable |
|---|---|---|
| equipos | Todas / De Turno | `subTabFarmacias: 'todas' \| 'turno'` |
| alertas | Activas / Incidentes / Red | `subTabAlertas: 'activas' \| 'incidentes' \| 'red'` |
| agentes | Equipos / Eventos | `subTabAgentes: 'equipos' \| 'eventos'` |
| despliegues | Campañas / Orquestación | `subTabDespliegues: 'estado' \| 'orquestacion'` |

---

## Panel Drawer (Alarm & Correlation Center)

- Se abre con botón 🔔 en el header
- `panelDerecho: boolean` en `app.component.ts`
- `togglePanel()` abre/cierra
- Backdrop clickeable cierra el panel
- Secciones: Estado global (4 KPIs) / Alertas críticas / Farmacias críticas / Red

---

## Tabla de campañas (Campaign Control)

- Filtros: `filtroCampana = { nombre: '', estado: '' }`
- Getter: `desplieguesFiltrados` — filtra `campanasPos` en tiempo real
- Expansión: `campanaExpandidaId: string | null` — toggle por `id`
- Filas expandidas muestran: descripción + acciones agrupadas (Consultar / Operaciones)

---

## Animaciones y transiciones

```css
/* Transición de tema — en todos los elementos que cambian color */
transition: background 0.2s ease, border-color 0.2s ease, color 0.2s ease;

/* Animación del NOC badge */
@keyframes pulso-noc { 0%, 100% { opacity: 1; } 50% { opacity: 0.4; } }

/* Drawer deslizante */
transform: translateX(100%);
transition: transform 0.28s cubic-bezier(0.4, 0, 0.2, 1);

/* Filas expandibles */
@keyframes expandirFila {
  from { opacity: 0; transform: translateY(-4px); }
  to   { opacity: 1; transform: translateY(0); }
}

/* Detalle de farmacia en NOC */
@keyframes slideDown {
  from { opacity: 0; transform: translateY(-6px); }
  to   { opacity: 1; transform: translateY(0); }
}
```

---

## Checklist para crear un componente nuevo

- [ ] Archivo standalone en `componentes-ui/` o subdirectorio propio
- [ ] CSS en archivo separado o en `styles: []` usando `var(--color-*)`
- [ ] Ningún color hex hardcodeado (usar fallback como segundo argumento: `var(--color-danger, #f85149)`)
- [ ] Si tiene fondo tipo tarjeta: `background: var(--color-card)` + `border: 1px solid var(--color-border)`
- [ ] Si tiene texto secundario: `color: var(--color-muted)`
- [ ] Agregar `transition: background 0.2s ease` si el color cambia con el tema
- [ ] Importar en `app.component.ts` en el array `imports`
- [ ] Verificar con `npx tsc --noEmit` antes de terminar

---

## Fases del NOC completadas

| Fase | Descripción | Estado |
|---|---|---|
| 1 | Theme toggle ☀️/🌙 + CSS variables + localStorage | ✅ |
| 2 | Header redesign + reloj en tiempo real + KPI bar (6 cards) | ✅ |
| 3 | Drawer lateral Alarm & Correlation Center | ✅ |
| 4 | Tabla campañas con filtros y filas expandibles | ✅ |
| 5 | Mapa SVG Ecuador con nodos hexagonales por ciudad | ✅ |

---

## Deuda técnica de UI conocida

| Ítem | Impacto | Solución |
|---|---|---|
| Mapa usa `ciudad` como proxy sin coordenadas exactas | El mapa no es geográficamente preciso | Agregar `lat`/`lng` a `Farmacia` en backend y `EstadoOperacionalFarmacia` en frontend |
| `tabla-alertas th` usa `background: #0f172a` hardcodeado | No responde al tema claro | Reemplazar con `var(--color-card-raised)` |
| `sev-disaster/warning/info` en `dashboard-noc.component.css` usan hex | No responden al tema claro | Migrar a `var(--color-danger)`, `var(--color-warning)`, `var(--color-primary)` |
| Sin tests de componentes Angular | Riesgo de regresión visual | Agregar tests con Angular Testing Library |
