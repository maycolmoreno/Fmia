# DISENO_NOC.md — Sistema de Diseño del Panel NOC
> Farmamia Operations Center — Frontend Angular 18
> Última actualización: 2026-07-06
>
> Leer este documento antes de tocar cualquier archivo CSS o crear un componente nuevo.
> El objetivo es que todo el panel tenga coherencia visual y soporte tema oscuro/claro automáticamente.

## Identidad visual — "Cruz Verde"

La paleta anterior (`#0d1117` / `#58a6ff` / `#3fb950`) era, hex por hex, la paleta de GitHub en modo
oscuro (Primer) — reconocible al instante como "modo oscuro genérico de developer", sin relación con
farmacias ni con Ecuador. La paleta actual parte de un objeto real del mundo de una farmacia: la cruz
iluminada que marca cada local, abstraída como el glifo de estado que reemplaza puntos/círculos en
todo el sistema (pendiente de implementar componente-por-componente; hoy solo los tokens y la
tipografía usan la nueva identidad).

**Regla de oro de la paleta:** `--color-primary` (verde cruz, la marca) y `--color-success` (verde
"todo OK") son colores **distintos a propósito** — para que "esto es Farmamia" y "esto está sano" no
se confundan visualmente en el mismo golpe de vista.

**Tipografía:** Bahnschrift (condensada, viene instalada en Windows — el mismo SO de cada PC del NOC
y de cada agente) para títulos/etiquetas vía `var(--font-display)`; Segoe UI para texto de lectura vía
`var(--font-body)`; Cascadia Mono para códigos/IPs/timestamps vía `var(--font-mono)` — con la lógica de
un ticket de POS impreso. Elegidas por estar ya instaladas en el entorno real de despliegue, no como
atajo: cero riesgo de fallback silencioso a una fuente distinta a la diseñada.

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
/* Tema OSCURO (default) — paleta "Cruz Verde" */
:host {
  --color-primary:       #1fae85;  /* verde cruz — MARCA, no "estado sano" */
  --color-primary-soft:  #12271f;
  --color-success:       #4cb977;  /* verde "sano", distinto a proposito del de marca */
  --color-success-soft:  #10251a;
  --color-warning:       #e8a33d;
  --color-warning-soft:  #2a2011;
  --color-danger:        #e1473b;
  --color-danger-soft:   #2c1512;
  --color-purple:        #b98cd6;
  --color-purple-soft:   #241a30;
  --color-background:    #0a120f;
  --color-card:          #121d19;
  --color-card-raised:   #182620;
  --color-border:        #24352c;
  --color-text:          #edeae2;
  --color-muted:         #8a9a90;
  --shadow-card:         0 8px 24px rgb(0 0 0 / 40%);
  --color-sidebar:       #121d19;
  --color-nav-text:      #8a9a90;
  --color-nav-active-bg: #12271f;
  --color-nav-active-border: #1f4a3e;

  --font-display: 'Bahnschrift', 'DIN Condensed', 'Arial Narrow', sans-serif;
  --font-body:    'Segoe UI', system-ui, -apple-system, sans-serif;
  --font-mono:    'Cascadia Mono', Consolas, 'SF Mono', monospace;
}

/* Tema CLARO — se activa cuando <main class="app-shell tema-claro"> */
:host:has(.tema-claro) {
  --color-primary:       #0e8f6c;
  --color-primary-soft:  #e3f3ec;
  --color-success:       #2e8b52;
  --color-success-soft:  #e6f3ea;
  --color-warning:       #b9761f;
  --color-warning-soft:  #f8ecd8;
  --color-danger:        #c43326;
  --color-danger-soft:   #fbe6e3;
  --color-purple:        #8449b3;
  --color-purple-soft:   #f2ebfa;
  --color-background:    #f7f5ef;
  --color-card:          #ffffff;
  --color-card-raised:   #eeece3;
  --color-border:        #dcd8ca;
  --color-text:          #171f1b;
  --color-muted:         #5c6b62;
  --shadow-card:         0 4px 16px rgb(23 31 27 / 6%);
  --color-sidebar:       #ffffff;
  --color-nav-text:      #5c6b62;
  --color-nav-active-bg: #e3f3ec;
  --color-nav-active-border: #bfe3d5;
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
| `--color-primary` | `#1fae85` | `#0e8f6c` | Marca (cruz), links, botones primarios, nav activo — **no** es "estado sano" |
| `--color-success` | `#4cb977` | `#2e8b52` | Farmacias OK, POS online, completados |
| `--color-warning` | `#e8a33d` | `#b9761f` | En riesgo, latencia alta, avisos, turno nocturno |
| `--color-danger` | `#e1473b` | `#c43326` | Críticas, offline, rollback, alertas |
| `--color-purple` | `#b98cd6` | `#8449b3` | Campañas activas, badge de turno |
| `--color-background` | `#0a120f` | `#f7f5ef` | Fondo general del contenido |
| `--color-card` | `#121d19` | `#ffffff` | Tarjetas, panel drawer, header |
| `--color-card-raised` | `#182620` | `#eeece3` | Filas hover, inputs, sub-tarjetas |
| `--color-border` | `#24352c` | `#dcd8ca` | Bordes de tarjetas, separadores |
| `--color-text` | `#edeae2` | `#171f1b` | Texto principal |
| `--color-muted` | `#8a9a90` | `#5c6b62` | Subtítulos, timestamps, etiquetas |

**Regla de oro:** Si hardcodeas un color hex en un componente, estás rompiendo el tema.

### Tipografía

| Variable | Fuente | Uso |
|---|---|---|
| `--font-display` | Bahnschrift | Títulos (`h1`/`h2`/`h3`), `.etiqueta`, `.grupo-nav` — condensada, caracter de señalética |
| `--font-body` | Segoe UI | Texto de lectura (heredado por defecto en `:host`) |
| `--font-mono` | Cascadia Mono | `.reloj-hora`, y todo dato tabular/código nuevo (IPs, timestamps, ids de farmacia) |

**Pendiente (fuera de esta pasada):** reestructurar `dashboard-noc` con el nuevo hero de "veredicto de
flota" y crear el componente de glifo de cruz que reemplace `status-badge`/puntos en el resto del
sistema — ver propuesta visual acordada en sesión (artefacto "Farmamia NOC — propuesta visual").

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
