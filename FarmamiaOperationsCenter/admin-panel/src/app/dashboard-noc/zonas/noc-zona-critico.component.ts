import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { FarmaciaCriticaNoc } from '../../modelos/modelos-operaciones';

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
        <div class="fila-turno fila-critica" *ngFor="let f of farmaciasCriticas">
          <div>
            <strong>{{ f.code }} — {{ f.name }}</strong>
            <small>{{ f.riskSummary }}</small>
          </div>
          <span class="badge badge-critico">CRITICA</span>
          <span><b>{{ f.criticalAlerts }}</b><small>alertas criticas</small></span>
          <span class="badge" [ngClass]="badgeTurno(f.onDuty)">{{ f.onDuty ? 'DE TURNO' : 'Fuera turno' }}</span>
        </div>
      </div>

      <div class="lista-turno" *ngIf="farmaciasDeTurnoEnRiesgo.length > 0">
        <p class="etiqueta-zona">DE TURNO EN RIESGO</p>
        <div class="fila-turno fila-riesgo" *ngFor="let f of farmaciasDeTurnoEnRiesgo">
          <div>
            <strong>{{ f.code }} — {{ f.name }}</strong>
            <small>{{ f.riskSummary }}</small>
          </div>
          <span class="badge badge-riesgo">EN RIESGO</span>
          <span><b>{{ f.criticalAlerts }}</b><small>alertas criticas</small></span>
          <span class="badge badge-turno">DE TURNO</span>
        </div>
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
    .fila-critica { border-left: 3px solid #e53e3e; }
    .fila-riesgo  { border-left: 3px solid #ed8936; }
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
  `]
})
export class NocZonaCriticoComponent {
  @Input() farmaciasCriticas: FarmaciaCriticaNoc[] = [];
  @Input() farmaciasDeTurnoEnRiesgo: FarmaciaCriticaNoc[] = [];

  badgeTurno(onDuty: boolean): Record<string, boolean> {
    return { 'badge-turno': onDuty, 'badge-riesgo': !onDuty };
  }
}
