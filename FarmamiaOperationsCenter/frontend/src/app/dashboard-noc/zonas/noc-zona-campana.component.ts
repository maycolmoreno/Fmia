import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { CampanaActivaNoc } from '../../modelos/modelos-operaciones';

@Component({
  selector: 'app-noc-zona-campana',
  standalone: true,
  imports: [CommonModule],
  template: `
    <article class="tarjeta bloque-noc" *ngIf="campana">
      <div class="titulo-tarjeta">
        <h2>Campana activa</h2>
        <small>{{ campana!.name }}</small>
      </div>
      <div class="metricas-zona">
        <div class="version-pos">
          <span class="metrica-label">Version POS</span>
          <strong>{{ campana!.posVersion || 'N/D' }}</strong>
        </div>
        <div class="barra-progreso-contenedor">
          <div class="barra-progreso-label">
            <span>Progreso</span>
            <span>{{ campana!.progressPercent }}%</span>
          </div>
          <div class="barra-progreso">
            <div class="barra-progreso-fill" [style.width.%]="campana!.progressPercent"></div>
          </div>
        </div>
        <div class="metrica-fila">
          <span class="metrica-valor">{{ campana!.totalDevices }}</span>
          <span class="metrica-label">Total POS</span>
        </div>
        <div class="metrica-fila metrica-ok">
          <span class="metrica-valor">{{ campana!.completed }}</span>
          <span class="metrica-label">Completados</span>
        </div>
        <div class="metrica-fila metrica-alerta" *ngIf="campana!.failed > 0">
          <span class="metrica-valor">{{ campana!.failed }}</span>
          <span class="metrica-label">Fallidos</span>
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
      background: var(--color-card-raised, #1c2128);
      transition: background 0.2s ease;
    }
    .metrica-ok     { background: var(--color-success-soft, #0d2818); }
    .metrica-alerta { background: var(--color-danger-soft, #2d0f0f); }
    .metrica-valor { font-size: 1.5rem; font-weight: 700; min-width: 32px; text-align: right; color: var(--color-text, #e6edf3); }
    .metrica-label { font-size: 0.85rem; color: var(--color-muted, #8b949e); }
    .version-pos { padding: 4px 10px; font-size: 0.9rem; color: var(--color-text, #e6edf3); }
    .barra-progreso-contenedor { padding: 4px 10px; }
    .barra-progreso-label {
      display: flex;
      justify-content: space-between;
      font-size: 0.8rem;
      color: var(--color-muted, #8b949e);
      margin-bottom: 4px;
    }
    .barra-progreso {
      height: 8px;
      background: var(--color-border, #21262d);
      border-radius: 4px;
      overflow: hidden;
    }
    .barra-progreso-fill {
      height: 100%;
      background: var(--color-success, #3fb950);
      border-radius: 4px;
      transition: width 0.4s ease;
    }
  `]
})
export class NocZonaCampanaComponent {
  @Input() campana: CampanaActivaNoc | null = null;
}
