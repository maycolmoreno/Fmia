import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { EstadoOperacionalFarmacia, FarmaciaCriticaNoc } from '../../modelos/modelos-operaciones';


@Component({
  selector: 'app-noc-zona-critico',
  standalone: true,
  imports: [CommonModule],
  template: `
    <article class="tarjeta bloque-noc bloque-prioritario">
      <div class="titulo-tarjeta">
        <h2>Farmacias criticas y en riesgo</h2>
        <small>Primera lectura operacional del NOC</small>
      </div>

      <div class="lista-turno" *ngIf="farmaciasCriticas.length > 0">
        <p class="etiqueta-zona">CRITICAS</p>
        <button type="button" class="fila-turno fila-critica fila-boton-noc" *ngFor="let f of farmaciasCriticas"
          (click)="seleccionarFarmacia.emit(f.code)">
          <div class="fila-info">
            <strong>{{ f.code }} — {{ f.name }}</strong>
            <small>{{ f.riskSummary }}</small>
          </div>
          <span class="badge badge-critico">CRITICA</span>
          <span><b>{{ f.criticalAlerts }}</b><small>alertas criticas</small></span>
          <span class="badge" [ngClass]="badgeTurno(f.onDuty)">{{ f.onDuty ? 'DE TURNO' : 'Fuera turno' }}</span>
          <span class="badge badge-campana" *ngIf="estadoFarmacia(f.code)?.campanaActivaPrincipal as campana">
            Campana: {{ campana }}
          </span>
          <a *ngIf="grafanaUrl" [href]="urlGrafanaFarmacia(f.code)" target="_blank" rel="noopener" class="enlace-grafana"
            title="Ver enlace en Grafana" (click)="$event.stopPropagation()">Grafana</a>
        </button>
      </div>

      <div class="lista-turno" *ngIf="farmaciasDeTurnoEnRiesgo.length > 0">
        <p class="etiqueta-zona">DE TURNO EN RIESGO</p>
        <button type="button" class="fila-turno fila-riesgo fila-boton-noc" *ngFor="let f of farmaciasDeTurnoEnRiesgo"
          (click)="seleccionarFarmacia.emit(f.code)">
          <div class="fila-info">
            <strong>{{ f.code }} — {{ f.name }}</strong>
            <small>{{ f.riskSummary }}</small>
          </div>
          <span class="badge badge-riesgo">EN RIESGO</span>
          <span><b>{{ f.criticalAlerts }}</b><small>alertas criticas</small></span>
          <span class="badge badge-turno">DE TURNO</span>
          <span class="badge badge-campana" *ngIf="estadoFarmacia(f.code)?.campanaActivaPrincipal as campana">
            Campana: {{ campana }}
          </span>
          <a *ngIf="grafanaUrl" [href]="urlGrafanaFarmacia(f.code)" target="_blank" rel="noopener" class="enlace-grafana"
            title="Ver enlace en Grafana" (click)="$event.stopPropagation()">Grafana</a>
        </button>
      </div>

      <p class="vacio" *ngIf="farmaciasCriticas.length === 0 && farmaciasDeTurnoEnRiesgo.length === 0">
        Sin farmacias criticas ni de turno en riesgo.
      </p>
    </article>
  `,
  styles: [`
    .etiqueta-zona {
      font-size: 0.72rem;
      font-weight: 700;
      letter-spacing: 0.06em;
      text-transform: uppercase;
      color: #607089;
      margin: 12px 0 4px;
    }
    .fila-boton-noc {
      width: 100%;
      text-align: left;
      background: none;
      border: none;
      cursor: pointer;
      padding: 0;
      font-family: inherit;
    }
    .fila-boton-noc:hover { background: rgba(0,0,0,0.03); border-radius: 4px; }
    .fila-critica { border-left: 3px solid #e53e3e; }
    .fila-riesgo  { border-left: 3px solid #ed8936; }
    .fila-info { display: flex; flex-direction: column; flex: 1; min-width: 0; }
    .enlace-grafana {
      font-size: 0.72rem;
      color: #e8700a;
      text-decoration: none;
      border: 1px solid #e8700a;
      border-radius: 4px;
      padding: 2px 6px;
      white-space: nowrap;
    }
    .enlace-grafana:hover { background: #fff7ed; }
    .badge {
      display: inline-block;
      padding: 2px 8px;
      border-radius: 4px;
      font-size: 0.72rem;
      font-weight: 700;
      white-space: nowrap;
    }
    .badge-critico { background: #fef2f2; color: #c53030; }
    .badge-riesgo  { background: #fffaf0; color: #c05621; }
    .badge-turno   { background: #ebf8ff; color: #2b6cb0; }
    .badge-campana { background: #f0f4ff; color: #3730a3; font-weight: 600; }
  `]
})
export class NocZonaCriticoComponent {
  @Input() farmaciasCriticas: FarmaciaCriticaNoc[] = [];
  @Input() farmaciasDeTurnoEnRiesgo: FarmaciaCriticaNoc[] = [];
  @Input() grafanaUrl: string = '';
  @Input() estadoFarmacias: EstadoOperacionalFarmacia[] = [];
  @Output() seleccionarFarmacia = new EventEmitter<string>();

  badgeTurno(onDuty: boolean): Record<string, boolean> {
    return { 'badge-turno': onDuty, 'badge-riesgo': !onDuty };
  }

  urlGrafanaFarmacia(branchCode: string): string {
    return `${this.grafanaUrl}/d/farmacia-enlace?var-branch=${branchCode}&from=now-24h&to=now`;
  }

  estadoFarmacia(code: string): EstadoOperacionalFarmacia | undefined {
    return this.estadoFarmacias.find(f => f.codigoFarmacia === code);
  }
}
