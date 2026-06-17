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
      background: #eff6ff;
      border: 1px solid #bfdbfe;
      border-radius: 999px;
      color: #1d4ed8;
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
    }

    .success {
      background: #ecfdf5;
      border-color: #bbf7d0;
      color: #15803d;
    }

    .warning {
      background: #fffbeb;
      border-color: #fde68a;
      color: #b45309;
    }

    .danger {
      background: #fef2f2;
      border-color: #fecaca;
      color: #b91c1c;
    }

    .purple {
      background: #f5f3ff;
      border-color: #ddd6fe;
      color: #6d28d9;
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
