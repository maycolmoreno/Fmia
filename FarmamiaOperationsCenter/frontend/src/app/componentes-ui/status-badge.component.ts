import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-status-badge',
  standalone: true,
  imports: [CommonModule],
  template: `<span class="status-badge" [ngClass]="badgeTone">{{ status || 'N/D' }}</span>`,
  styles: [`
    .status-badge {
      align-items: center;
      background: var(--color-primary-soft, #172340);
      border: 1px solid color-mix(in srgb, var(--color-primary, #58a6ff) 30%, transparent);
      border-radius: 999px;
      color: var(--color-primary, #58a6ff);
      display: inline-flex;
      font-size: 0.72rem;
      font-weight: 800;
      gap: 6px;
      justify-content: center;
      letter-spacing: 0;
      line-height: 1;
      min-height: 24px;
      padding: 5px 9px;
      text-transform: uppercase;
      white-space: nowrap;
      transition: background 0.2s ease, color 0.2s ease;
    }

    .success {
      background: var(--color-success-soft, #0d2818);
      border-color: color-mix(in srgb, var(--color-success, #3fb950) 30%, transparent);
      color: var(--color-success, #3fb950);
    }

    .warning {
      background: var(--color-warning-soft, #2b1d00);
      border-color: color-mix(in srgb, var(--color-warning, #e3b341) 30%, transparent);
      color: var(--color-warning, #e3b341);
    }

    .danger {
      background: var(--color-danger-soft, #2d0f0f);
      border-color: color-mix(in srgb, var(--color-danger, #f85149) 30%, transparent);
      color: var(--color-danger, #f85149);
    }

    .purple {
      background: var(--color-purple-soft, #1e0a3a);
      border-color: color-mix(in srgb, var(--color-purple, #a371f7) 30%, transparent);
      color: var(--color-purple, #a371f7);
    }
  `]
})
export class StatusBadgeComponent {
  @Input() status = '';
  @Input() tone: 'success' | 'warning' | 'danger' | 'purple' | 'info' | '' = '';

  get badgeTone(): string {
    if (this.tone) {
      return this.tone;
    }

    const normalized = (this.status || '').toUpperCase();
    if (['NORMAL', 'OK', 'ONLINE', 'COMPLETED', 'CONTINUAR', 'OPEN'].includes(normalized)) {
      return 'success';
    }
    if (['EN_RIESGO', 'WARNING', 'VIGILAR', 'STALE', 'PAUSED', 'HIGH'].includes(normalized)) {
      return 'warning';
    }
    if (['CRITICA', 'CRITICAL', 'ERROR', 'OFFLINE', 'FAILED', 'PAUSAR', 'CLOSED'].includes(normalized)) {
      return 'danger';
    }
    return 'info';
  }
}
