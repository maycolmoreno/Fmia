import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { AlertaResumenNoc } from '../../modelos/modelos-operaciones';

@Component({
  selector: 'app-noc-zona-alertas',
  standalone: true,
  imports: [CommonModule],
  template: `
    <article class="tarjeta bloque-noc">
      <div class="titulo-tarjeta">
        <h2>Alertas recientes</h2>
        <small>Ultimas {{ alertas.length }} alertas registradas</small>
      </div>
      <div class="tabla tabla-noc">
        <div class="tabla-fila cabecera">
          <span>Farmacia</span>
          <span>Severidad</span>
          <span>Tipo</span>
          <span>Titulo</span>
          <span>Estado</span>
          <span>Cuando</span>
        </div>
        <div class="tabla-fila" *ngFor="let a of alertas">
          <span>{{ a.farmCode || 'N/D' }}</span>
          <span class="estado" [ngClass]="claseSeveridad(a.severity)">{{ a.severity }}</span>
          <span>
            {{ a.alertType }}
            <small *ngIf="a.networkEvent" class="badge-red">RED</small>
          </span>
          <span>{{ a.title }}</span>
          <span class="estado" [ngClass]="claseEstado(a.status)">{{ a.status }}</span>
          <span>{{ a.openedAt | date:'dd/MM HH:mm' }}</span>
        </div>
        <p class="vacio" *ngIf="alertas.length === 0">Sin alertas recientes.</p>
      </div>
    </article>
  `,
  styles: [`
    .badge-red {
      display: inline-block;
      margin-left: 6px;
      padding: 1px 5px;
      background: #e6f0ff;
      color: #2b5fa8;
      border-radius: 3px;
      font-size: 0.68rem;
      font-weight: 700;
      vertical-align: middle;
    }
  `]
})
export class NocZonaAlertasComponent {
  @Input() alertas: AlertaResumenNoc[] = [];

  claseSeveridad(severity: string): Record<string, boolean> {
    return {
      critica: severity === 'CRITICAL',
      riesgo: severity === 'HIGH',
      aviso: severity === 'WARNING'
    };
  }

  claseEstado(status: string): Record<string, boolean> {
    return {
      critica: status === 'OPEN',
      riesgo: status === 'ACKNOWLEDGED'
    };
  }
}
