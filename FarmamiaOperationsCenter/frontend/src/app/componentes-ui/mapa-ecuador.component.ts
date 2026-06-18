import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output } from '@angular/core';
import { EstadoOperacionalFarmacia } from '../modelos/modelos-operaciones';

interface NodoMapa {
  ciudad: string;
  x: number;
  y: number;
  total: number;
  criticas: number;
  enRiesgo: number;
  deTurno: number;
  ok: number;
  estado: 'critico' | 'riesgo' | 'turno' | 'ok' | 'inactivo';
  farmacias: EstadoOperacionalFarmacia[];
  hexPuntos: string;
}

/* Coordenadas aproximadas por ciudad en viewBox 0 0 320 420 */
const CIUDAD_COORDS: Record<string, { x: number; y: number }> = {
  'esmeraldas': { x: 62, y: 38 },
  'ibarra': { x: 148, y: 62 },
  'quito': { x: 155, y: 98 },
  'santo domingo': { x: 92, y: 110 },
  'santo domingo de los tsachilas': { x: 92, y: 110 },
  'portoviejo': { x: 40, y: 118 },
  'manta': { x: 28, y: 134 },
  'latacunga': { x: 152, y: 134 },
  'ambato': { x: 155, y: 152 },
  'guaranda': { x: 125, y: 165 },
  'riobamba': { x: 152, y: 178 },
  'babahoyo': { x: 104, y: 180 },
  'guayaquil': { x: 84, y: 205 },
  'cuenca': { x: 168, y: 218 },
  'machala': { x: 92, y: 258 },
  'loja': { x: 158, y: 282 },
  'zamora': { x: 204, y: 272 },
  'lago agrio': { x: 224, y: 68 },
  'nueva loja': { x: 224, y: 68 },
  'tena': { x: 216, y: 132 },
  'puyo': { x: 214, y: 178 },
  'macas': { x: 232, y: 222 },
  'orellana': { x: 248, y: 102 },
  'francisco de orellana': { x: 248, y: 102 },
};

function hexPuntos(cx: number, cy: number, r: number): string {
  const puntos: string[] = [];
  for (let i = 0; i < 6; i++) {
    const ang = (Math.PI / 180) * (60 * i - 30);
    puntos.push(`${(cx + r * Math.cos(ang)).toFixed(1)},${(cy + r * Math.sin(ang)).toFixed(1)}`);
  }
  return puntos.join(' ');
}

function coordsCiudad(ciudad: string): { x: number; y: number } | null {
  if (!ciudad) return null;
  const key = ciudad.toLowerCase().trim();
  if (CIUDAD_COORDS[key]) return CIUDAD_COORDS[key];
  for (const k of Object.keys(CIUDAD_COORDS)) {
    if (key.includes(k) || k.includes(key)) return CIUDAD_COORDS[k];
  }
  return null;
}

@Component({
  selector: 'app-mapa-ecuador',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="mapa-contenedor">
      <svg class="mapa-svg" viewBox="0 0 320 420" xmlns="http://www.w3.org/2000/svg"
           role="img" aria-label="Mapa de farmacias en Ecuador">

        <!-- Fondo de agua -->
        <rect width="320" height="420" fill="var(--mapa-agua, #0d1f33)" rx="8"/>

        <!-- Contorno continental Ecuador (simplificado) -->
        <path class="mapa-tierra" d="
          M 64 34
          L 48 52
          L 22 88
          L 14 118
          L 18 148
          L 30 170
          L 54 198
          L 68 224
          L 76 252
          L 88 268
          L 120 292
          L 148 300
          L 178 294
          L 204 280
          L 228 248
          L 252 230
          L 268 200
          L 274 164
          L 270 128
          L 262 92
          L 244 58
          L 210 38
          L 180 28
          L 148 26
          L 120 28
          Z
        "/>

        <!-- Leyenda de ciudades no mapeadas -->
        <text x="280" y="340" class="mapa-sin-coords" *ngIf="sinCoords > 0">
          +{{ sinCoords }} sin coord.
        </text>

        <!-- Nodos hexagonales por ciudad -->
        <g *ngFor="let nodo of nodos" class="nodo-grupo"
           (click)="seleccionarNodo(nodo)"
           (mouseenter)="nodoHover = nodo"
           (mouseleave)="nodoHover = null"
           style="cursor:pointer">

          <!-- Sombra -->
          <polygon [attr.points]="hexPuntosSombra(nodo)"
                   class="hex-sombra"/>

          <!-- Hexagono principal -->
          <polygon [attr.points]="nodo.hexPuntos"
                   [class]="'hex hex-' + nodo.estado"/>

          <!-- Punto de turno extra (borde cyan) -->
          <polygon *ngIf="nodo.deTurno > 0" [attr.points]="hexPuntosRing(nodo)"
                   class="hex-turno-ring"/>

          <!-- Número de farmacias -->
          <text [attr.x]="nodo.x" [attr.y]="nodo.y + 1"
                class="hex-label" text-anchor="middle" dominant-baseline="middle">
            {{ nodo.total }}
          </text>

          <!-- Indicador de criticas -->
          <circle *ngIf="nodo.criticas > 0"
                  [attr.cx]="nodo.x + 10" [attr.cy]="nodo.y - 8"
                  r="5" class="badge-critico"/>
          <text *ngIf="nodo.criticas > 0"
                [attr.x]="nodo.x + 10" [attr.y]="nodo.y - 7"
                class="badge-num" text-anchor="middle" dominant-baseline="middle">
            {{ nodo.criticas }}
          </text>
        </g>

        <!-- Tooltip flotante -->
        <g *ngIf="nodoHover" class="mapa-tooltip-g">
          <rect [attr.x]="tooltipX(nodoHover)" [attr.y]="tooltipY(nodoHover)"
                width="130" height="80" rx="6" class="tooltip-fondo"/>
          <text [attr.x]="tooltipX(nodoHover) + 8" [attr.y]="tooltipY(nodoHover) + 16"
                class="tooltip-titulo">{{ nodoHover.ciudad }}</text>
          <text [attr.x]="tooltipX(nodoHover) + 8" [attr.y]="tooltipY(nodoHover) + 30"
                class="tooltip-linea">Total: {{ nodoHover.total }}</text>
          <text [attr.x]="tooltipX(nodoHover) + 8" [attr.y]="tooltipY(nodoHover) + 43"
                class="tooltip-linea">OK: {{ nodoHover.ok }}</text>
          <text [attr.x]="tooltipX(nodoHover) + 8" [attr.y]="tooltipY(nodoHover) + 56"
                class="tooltip-linea-alerta" *ngIf="nodoHover.criticas > 0">Criticas: {{ nodoHover.criticas }}</text>
          <text [attr.x]="tooltipX(nodoHover) + 8" [attr.y]="tooltipY(nodoHover) + 69"
                class="tooltip-linea-aviso" *ngIf="nodoHover.deTurno > 0">Turno: {{ nodoHover.deTurno }}</text>
        </g>

        <!-- Leyenda -->
        <g transform="translate(8, 348)">
          <rect width="140" height="64" rx="4" class="leyenda-fondo"/>
          <text x="6" y="12" class="leyenda-titulo">Estado</text>
          <polygon points="6,20 14,16 18,20 14,24" class="hex hex-ok"/>
          <text x="22" y="24" class="leyenda-txt">OK</text>
          <polygon points="6,32 14,28 18,32 14,36" class="hex hex-riesgo"/>
          <text x="22" y="36" class="leyenda-txt">En riesgo</text>
          <polygon points="6,44 14,40 18,44 14,48" class="hex hex-critico"/>
          <text x="22" y="48" class="leyenda-txt">Critica</text>
          <polygon points="6,56 14,52 18,56 14,60" class="hex hex-inactivo"/>
          <text x="22" y="60" class="leyenda-txt">Sin datos</text>
          <polygon points="70,20 78,16 82,20 78,24" class="hex hex-turno-ring" style="fill:none"/>
          <text x="86" y="24" class="leyenda-txt">De turno</text>
        </g>
      </svg>

      <!-- Panel lateral del nodo seleccionado -->
      <div class="nodo-panel" *ngIf="nodoSeleccionado">
        <div class="nodo-panel-header">
          <div>
            <h3>{{ nodoSeleccionado.ciudad }}</h3>
            <small>{{ nodoSeleccionado.total }} farmacia{{ nodoSeleccionado.total !== 1 ? 's' : '' }}</small>
          </div>
          <button type="button" class="panel-cerrar-nodo" (click)="nodoSeleccionado = null">✕</button>
        </div>
        <div class="nodo-panel-kpis">
          <div class="nodo-kpi nodo-kpi-ok"><strong>{{ nodoSeleccionado.ok }}</strong><small>OK</small></div>
          <div class="nodo-kpi nodo-kpi-riesgo"><strong>{{ nodoSeleccionado.enRiesgo }}</strong><small>Riesgo</small></div>
          <div class="nodo-kpi nodo-kpi-critico"><strong>{{ nodoSeleccionado.criticas }}</strong><small>Criticas</small></div>
          <div class="nodo-kpi nodo-kpi-turno"><strong>{{ nodoSeleccionado.deTurno }}</strong><small>Turno</small></div>
        </div>
        <ul class="nodo-farm-lista">
          <li class="nodo-farm-item" *ngFor="let f of nodoSeleccionado.farmacias"
              (click)="seleccionarFarmacia.emit(f.codigoFarmacia)">
            <div class="nodo-farm-info">
              <strong>{{ f.codigoFarmacia }}</strong>
              <small>{{ f.nombreFarmacia }}</small>
            </div>
            <div class="nodo-farm-badges">
              <span class="nodo-badge" [class.nodo-badge-critico]="f.critica" [class.nodo-badge-riesgo]="f.turnoEnRiesgo && !f.critica" [class.nodo-badge-ok]="!f.critica && !f.turnoEnRiesgo">
                {{ f.critica ? 'CRITICA' : f.turnoEnRiesgo ? 'RIESGO' : f.estadoOperacional }}
              </span>
              <span class="nodo-badge nodo-badge-turno" *ngIf="f.deTurno">TURNO</span>
            </div>
          </li>
        </ul>
      </div>
    </div>
  `,
  styles: [`
    .mapa-contenedor {
      display: flex;
      gap: 16px;
      align-items: flex-start;
    }

    .mapa-svg {
      flex-shrink: 0;
      width: 320px;
      height: auto;
      border-radius: 10px;
      border: 1px solid var(--color-border, #30363d);
      background: var(--mapa-agua, #0d1f33);
      transition: border-color 0.2s ease;
    }

    .mapa-tierra {
      fill: var(--mapa-tierra, #1a2d1a);
      stroke: var(--color-border, #30363d);
      stroke-width: 1;
    }

    /* Hexagonos */
    .hex { stroke-width: 1.5; }
    .hex-ok       { fill: var(--color-success-soft, #0d2818); stroke: var(--color-success, #3fb950); }
    .hex-riesgo   { fill: var(--color-warning-soft, #2b1d00); stroke: var(--color-warning, #e3b341); }
    .hex-critico  { fill: var(--color-danger-soft, #2d0f0f);  stroke: var(--color-danger, #f85149); }
    .hex-turno    { fill: var(--color-primary-soft, #172340); stroke: var(--color-primary, #58a6ff); }
    .hex-inactivo { fill: var(--color-card-raised, #1c2128);  stroke: var(--color-border, #30363d); }

    .hex-sombra {
      fill: rgb(0 0 0 / 40%);
      transform: translate(1.5px, 2px);
    }

    .hex-turno-ring {
      fill: none;
      stroke: #00e5ff;
      stroke-width: 1.5;
      stroke-dasharray: 3 2;
    }

    .hex-label {
      font-size: 8px;
      font-weight: 700;
      fill: var(--color-text, #e6edf3);
      pointer-events: none;
    }

    .badge-critico { fill: var(--color-danger, #f85149); }
    .badge-num {
      font-size: 5px;
      font-weight: 700;
      fill: #fff;
      pointer-events: none;
    }

    /* Leyenda */
    .leyenda-fondo   { fill: var(--color-card, #161b22); stroke: var(--color-border, #30363d); stroke-width: 0.8; }
    .leyenda-titulo  { font-size: 7px; font-weight: 700; fill: var(--color-muted, #8b949e); text-transform: uppercase; }
    .leyenda-txt     { font-size: 7px; fill: var(--color-text, #e6edf3); }

    /* Tooltip */
    .tooltip-fondo  { fill: var(--color-card, #161b22); stroke: var(--color-border, #30363d); stroke-width: 0.8; }
    .tooltip-titulo { font-size: 8px; font-weight: 700; fill: var(--color-text, #e6edf3); }
    .tooltip-linea  { font-size: 7px; fill: var(--color-muted, #8b949e); }
    .tooltip-linea-alerta { font-size: 7px; fill: var(--color-danger, #f85149); }
    .tooltip-linea-aviso  { font-size: 7px; fill: #00e5ff; }

    .mapa-sin-coords { font-size: 7px; fill: var(--color-muted, #8b949e); }

    /* Panel de ciudad seleccionada */
    .nodo-panel {
      flex: 1;
      min-width: 0;
      background: var(--color-card, #161b22);
      border: 1px solid var(--color-border, #30363d);
      border-radius: 10px;
      overflow: hidden;
      animation: slideIn 0.15s ease-out;
    }

    @keyframes slideIn {
      from { opacity: 0; transform: translateX(8px); }
      to   { opacity: 1; transform: translateX(0); }
    }

    .nodo-panel-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 12px 14px;
      border-bottom: 1px solid var(--color-border, #30363d);
      background: var(--color-card-raised, #1c2128);
    }

    .nodo-panel-header h3 { margin: 0 0 2px; font-size: 0.95rem; color: var(--color-text, #e6edf3); }
    .nodo-panel-header small { font-size: 0.72rem; color: var(--color-muted, #8b949e); }

    .panel-cerrar-nodo {
      background: transparent;
      border: 1px solid var(--color-border, #30363d);
      border-radius: 5px;
      color: var(--color-muted, #8b949e);
      cursor: pointer;
      font-size: 0.85rem;
      padding: 3px 7px;
    }

    .nodo-panel-kpis {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      border-bottom: 1px solid var(--color-border, #30363d);
    }

    .nodo-kpi {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 10px 8px;
      border-right: 1px solid var(--color-border, #30363d);
    }
    .nodo-kpi:last-child { border-right: none; }

    .nodo-kpi strong { font-size: 1.3rem; font-weight: 700; line-height: 1; font-variant-numeric: tabular-nums; }
    .nodo-kpi small  { font-size: 0.65rem; color: var(--color-muted, #8b949e); }

    .nodo-kpi-ok      strong { color: var(--color-success, #3fb950); }
    .nodo-kpi-riesgo  strong { color: var(--color-warning, #e3b341); }
    .nodo-kpi-critico strong { color: var(--color-danger, #f85149); }
    .nodo-kpi-turno   strong { color: #00e5ff; }

    .nodo-farm-lista {
      list-style: none;
      margin: 0;
      padding: 8px;
      display: flex;
      flex-direction: column;
      gap: 4px;
      max-height: 280px;
      overflow-y: auto;
    }

    .nodo-farm-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 8px;
      background: var(--color-card-raised, #1c2128);
      border-radius: 6px;
      padding: 7px 10px;
      cursor: pointer;
      transition: background 0.12s ease;
    }
    .nodo-farm-item:hover { background: var(--color-primary-soft, #172340); }

    .nodo-farm-info { display: flex; flex-direction: column; min-width: 0; }
    .nodo-farm-info strong { font-size: 0.82rem; color: var(--color-text, #e6edf3); }
    .nodo-farm-info small  { font-size: 0.7rem; color: var(--color-muted, #8b949e); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

    .nodo-farm-badges { display: flex; gap: 4px; flex-shrink: 0; }

    .nodo-badge {
      font-size: 0.6rem;
      font-weight: 700;
      border-radius: 3px;
      padding: 2px 5px;
      white-space: nowrap;
      background: var(--color-card, #161b22);
      color: var(--color-muted, #8b949e);
    }
    .nodo-badge-ok      { background: var(--color-success-soft, #0d2818); color: var(--color-success, #3fb950); }
    .nodo-badge-riesgo  { background: var(--color-warning-soft, #2b1d00); color: var(--color-warning, #e3b341); }
    .nodo-badge-critico { background: var(--color-danger-soft, #2d0f0f);  color: var(--color-danger, #f85149); }
    .nodo-badge-turno   { background: rgb(0 229 255 / 12%); color: #00e5ff; }

    /* Ajuste de tema claro */
    :host-context(.tema-claro) .mapa-svg {
      --mapa-agua: #dbeafe;
      --mapa-tierra: #d1fae5;
    }
  `]
})
export class MapaEcuadorComponent implements OnChanges {
  @Input() estadoFarmacias: EstadoOperacionalFarmacia[] = [];
  @Output() seleccionarFarmacia = new EventEmitter<string>();

  nodos: NodoMapa[] = [];
  nodoHover: NodoMapa | null = null;
  nodoSeleccionado: NodoMapa | null = null;
  sinCoords = 0;

  ngOnChanges(): void {
    this.construirNodos();
  }

  private construirNodos(): void {
    const grupos = new Map<string, EstadoOperacionalFarmacia[]>();
    let sinCoords = 0;

    for (const f of this.estadoFarmacias) {
      const ciudad = (f.ciudad ?? '').trim();
      const coords = coordsCiudad(ciudad);
      if (!coords) { sinCoords++; continue; }
      const key = coords.x + ',' + coords.y;
      if (!grupos.has(key)) grupos.set(key, []);
      grupos.get(key)!.push(f);
    }

    this.sinCoords = sinCoords;

    this.nodos = Array.from(grupos.entries()).map(([key, farmacias]) => {
      const [x, y] = key.split(',').map(Number);
      const criticas = farmacias.filter(f => f.critica).length;
      const enRiesgo = farmacias.filter(f => f.turnoEnRiesgo && !f.critica).length;
      const deTurno = farmacias.filter(f => f.deTurno).length;
      const ok = farmacias.filter(f => !f.critica && !f.turnoEnRiesgo).length;
      const ciudad = farmacias[0].ciudad ?? key;

      let estado: NodoMapa['estado'];
      if (criticas > 0) estado = 'critico';
      else if (enRiesgo > 0) estado = 'riesgo';
      else if (deTurno > 0 && ok === farmacias.length - deTurno) estado = 'ok';
      else if (ok === farmacias.length) estado = 'ok';
      else estado = 'inactivo';

      return {
        ciudad,
        x,
        y,
        total: farmacias.length,
        criticas,
        enRiesgo,
        deTurno,
        ok,
        estado,
        farmacias,
        hexPuntos: hexPuntos(x, y, 13),
      };
    });
  }

  hexPuntosSombra(nodo: NodoMapa): string {
    return hexPuntos(nodo.x + 1.5, nodo.y + 2, 13);
  }

  hexPuntosRing(nodo: NodoMapa): string {
    return hexPuntos(nodo.x, nodo.y, 16);
  }

  tooltipX(nodo: NodoMapa): number {
    return nodo.x > 190 ? nodo.x - 138 : nodo.x + 18;
  }

  tooltipY(nodo: NodoMapa): number {
    return Math.min(nodo.y - 10, 330);
  }

  seleccionarNodo(nodo: NodoMapa): void {
    this.nodoSeleccionado = this.nodoSeleccionado?.ciudad === nodo.ciudad ? null : nodo;
  }
}
