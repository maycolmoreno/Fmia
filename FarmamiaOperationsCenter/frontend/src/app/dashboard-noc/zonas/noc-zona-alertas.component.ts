import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AlertaResumenNoc } from '../../modelos/modelos-operaciones';

@Component({
  selector: 'app-noc-zona-alertas',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <article class="tarjeta bloque-noc">
      <div class="titulo-tarjeta">
        <h2>Alertas recientes</h2>
        <div class="titulo-acciones">
          <small>{{ alertasFiltradas.length }} alertas</small>
          <button type="button" class="boton-filtro-turno" [class.activo]="soloTurno" (click)="soloTurno = !soloTurno"
            title="Mostrar solo alertas de farmacias de turno">
            {{ soloTurno ? 'Todas las alertas' : 'Solo turno' }}
          </button>
        </div>
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
        <div class="tabla-fila" *ngFor="let a of alertasFiltradas"
          [class.fila-turno-activo]="soloTurno && esDeTurno(a.farmCode)">
          <span>
            {{ a.farmCode || 'N/D' }}
            <span class="badge-turno-inline" *ngIf="esDeTurno(a.farmCode)">T</span>
          </span>
          <span class="estado" [ngClass]="claseSeveridad(a.severity)">{{ a.severity }}</span>
          <span>
            {{ a.alertType }}
            <small *ngIf="a.networkEvent" class="badge-red">RED</small>
          </span>
          <span>{{ a.title }}</span>
          <span class="estado" [ngClass]="claseEstado(a.status)">{{ a.status }}</span>
          <span>{{ a.openedAt | date:'dd/MM HH:mm' }}</span>
        </div>
        <p class="vacio" *ngIf="alertasFiltradas.length === 0 && soloTurno">
          Sin alertas activas en farmacias de turno.
        </p>
        <p class="vacio" *ngIf="alertas.length === 0">Sin alertas recientes.</p>
      </div>
    </article>
  `,
  styles: [`
    .titulo-acciones { display: flex; align-items: center; gap: 8px; }
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
    .boton-filtro-turno {
      font-size: 0.72rem;
      padding: 2px 8px;
      border: 1px solid #2b6cb0;
      border-radius: 4px;
      background: none;
      color: #2b6cb0;
      cursor: pointer;
      white-space: nowrap;
    }
    .boton-filtro-turno.activo {
      background: #ebf8ff;
      font-weight: 700;
    }
    .badge-turno-inline {
      display: inline-block;
      margin-left: 4px;
      padding: 0 4px;
      background: #ebf8ff;
      color: #2b6cb0;
      border-radius: 3px;
      font-size: 0.65rem;
      font-weight: 700;
      vertical-align: middle;
    }
    .fila-turno-activo { background: #f0f9ff; }
  `]
})
export class NocZonaAlertasComponent {
  @Input() alertas: AlertaResumenNoc[] = [];
  @Input() codigosFarmaciasTurno: Set<string> = new Set();

  soloTurno = false;

  get alertasFiltradas(): AlertaResumenNoc[] {
    if (!this.soloTurno) {
      return this.alertas;
    }
    return this.alertas.filter(a => a.farmCode && this.codigosFarmaciasTurno.has(a.farmCode));
  }

  esDeTurno(farmCode: string | null): boolean {
    return !!farmCode && this.codigosFarmaciasTurno.has(farmCode);
  }

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
