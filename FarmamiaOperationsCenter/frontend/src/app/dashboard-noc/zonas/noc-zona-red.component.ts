import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { EstadoRedNoc } from '../../modelos/modelos-operaciones';

@Component({
  selector: 'app-noc-zona-red',
  standalone: true,
  imports: [CommonModule],
  template: `
    <article class="tarjeta bloque-noc">
      <div class="titulo-tarjeta">
        <h2>Red</h2>
        <div class="titulo-acciones">
          <small>Alertas de red abiertas</small>
          <a *ngIf="grafanaUrl" [href]="urlGrafanaRed()" target="_blank" rel="noopener" class="enlace-grafana" title="Ver dashboards de red en Grafana">Grafana</a>
        </div>
      </div>
      <div class="metricas-zona">
        <div class="metrica-fila" [class.metrica-alerta]="red.linkDown > 0">
          <span class="metrica-valor">{{ red.linkDown }}</span>
          <span class="metrica-label">Enlace(s) caido(s)</span>
        </div>
        <div class="metrica-fila" [class.metrica-aviso]="red.highLatency > 0">
          <span class="metrica-valor">{{ red.highLatency }}</span>
          <span class="metrica-label">Latencia alta</span>
        </div>
        <div class="metrica-fila" [class.metrica-alerta]="red.vpnDown > 0">
          <span class="metrica-valor">{{ red.vpnDown }}</span>
          <span class="metrica-label">VPN caida(s)</span>
        </div>
      </div>
    </article>
  `,
  styles: [`
    .titulo-acciones { display: flex; align-items: center; gap: 8px; }
    .metricas-zona { display: grid; gap: 8px; padding-top: 8px; }
    .metrica-fila {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 8px 10px;
      border-radius: 6px;
      background: #f8fafc;
    }
    .metrica-alerta { background: #fef2f2; }
    .metrica-aviso  { background: #fffaf0; }
    .metrica-valor { font-size: 1.5rem; font-weight: 700; min-width: 32px; text-align: right; }
    .metrica-label { font-size: 0.85rem; color: #607089; }
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
  `]
})
export class NocZonaRedComponent {
  @Input() red: EstadoRedNoc = { linkDown: 0, highLatency: 0, vpnDown: 0 };
  @Input() grafanaUrl: string = '';

  urlGrafanaRed(): string {
    return `${this.grafanaUrl}/d/red-farmacias?from=now-3h&to=now`;
  }
}
