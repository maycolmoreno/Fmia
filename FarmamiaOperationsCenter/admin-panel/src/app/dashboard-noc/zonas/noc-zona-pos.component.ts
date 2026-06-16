import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { EstadoPosNoc } from '../../modelos/modelos-operaciones';

@Component({
  selector: 'app-noc-zona-pos',
  standalone: true,
  imports: [CommonModule],
  template: `
    <article class="tarjeta bloque-noc">
      <div class="titulo-tarjeta">
        <h2>POS</h2>
        <small>Estado de terminales</small>
      </div>
      <div class="metricas-zona">
        <div class="metrica-fila">
          <span class="metrica-valor">{{ pos.total }}</span>
          <span class="metrica-label">Total POS</span>
        </div>
        <div class="metrica-fila metrica-ok" *ngIf="pos.online > 0">
          <span class="metrica-valor">{{ pos.online }}</span>
          <span class="metrica-label">Online</span>
        </div>
        <div class="metrica-fila metrica-alerta" *ngIf="pos.offline > 0">
          <span class="metrica-valor">{{ pos.offline }}</span>
          <span class="metrica-label">Offline</span>
        </div>
        <div class="metrica-fila metrica-aviso" *ngIf="pos.atRisk > 0">
          <span class="metrica-valor">{{ pos.atRisk }}</span>
          <span class="metrica-label">Sin estado conocido</span>
        </div>
        <div class="version-actual" *ngIf="pos.currentVersion">
          <span class="metrica-label">Version dominante: </span>
          <strong>{{ pos.currentVersion }}</strong>
        </div>
      </div>
    </article>
  `,
  styles: [`
    .metricas-zona { display: grid; gap: 8px; padding-top: 8px; }
    .metrica-fila {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 8px 10px;
      border-radius: 6px;
      background: #f8fafc;
    }
    .metrica-ok     { background: #f0fff4; }
    .metrica-alerta { background: #fef2f2; }
    .metrica-aviso  { background: #fffaf0; }
    .metrica-valor { font-size: 1.5rem; font-weight: 700; min-width: 32px; text-align: right; }
    .metrica-label { font-size: 0.85rem; color: #607089; }
    .version-actual { padding: 6px 10px; font-size: 0.85rem; }
  `]
})
export class NocZonaPosComponent {
  @Input() pos: EstadoPosNoc = { total: 0, online: 0, offline: 0, atRisk: 0, currentVersion: null };
}
