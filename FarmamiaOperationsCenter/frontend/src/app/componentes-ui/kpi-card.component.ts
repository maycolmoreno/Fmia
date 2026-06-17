import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-kpi-card',
  standalone: true,
  imports: [CommonModule],
  template: `
    <article class="kpi-card" [ngClass]="tone">
      <div class="kpi-head">
        <span class="kpi-icon">{{ icon }}</span>
        <span class="kpi-percent">{{ percent }}</span>
      </div>
      <span class="kpi-label">{{ label }}</span>
      <strong>{{ value }}</strong>
      <small>{{ helper }}</small>
      <div class="sparkline" aria-hidden="true">
        <span *ngFor="let point of sparkline" [style.height.%]="point"></span>
      </div>
    </article>
  `,
  styles: [`
    .kpi-card {
      background: #ffffff;
      border: 1px solid #e5e7eb;
      border-radius: 12px;
      box-shadow: 0 10px 28px rgb(15 23 42 / 6%);
      display: grid;
      gap: 8px;
      min-height: 174px;
      padding: 20px;
    }

    .kpi-head {
      align-items: center;
      display: flex;
      justify-content: space-between;
    }

    .kpi-icon {
      align-items: center;
      background: #eff6ff;
      border-radius: 10px;
      color: #2563eb;
      display: inline-flex;
      font-size: 0.82rem;
      font-weight: 800;
      height: 34px;
      justify-content: center;
      width: 34px;
    }

    .kpi-percent {
      color: #64748b;
      font-size: 0.78rem;
      font-weight: 700;
    }

    .kpi-label,
    small {
      color: #64748b;
      font-size: 0.82rem;
      font-weight: 600;
    }

    strong {
      color: #0f172a;
      font-size: clamp(1.9rem, 4vw, 2.45rem);
      line-height: 1;
    }

    .sparkline {
      align-items: end;
      display: grid;
      gap: 4px;
      grid-template-columns: repeat(8, minmax(0, 1fr));
      height: 34px;
      margin-top: 4px;
    }

    .sparkline span {
      background: #bfdbfe;
      border-radius: 999px 999px 2px 2px;
      min-height: 8px;
    }

    .success .kpi-icon,
    .success .sparkline span {
      background: #dcfce7;
      color: #16a34a;
    }

    .warning .kpi-icon,
    .warning .sparkline span {
      background: #fef3c7;
      color: #f59e0b;
    }

    .danger .kpi-icon,
    .danger .sparkline span {
      background: #fee2e2;
      color: #dc2626;
    }

    .purple .kpi-icon,
    .purple .sparkline span {
      background: #ede9fe;
      color: #7c3aed;
    }
  `]
})
export class KpiCardComponent {
  @Input() label = '';
  @Input() value: string | number = 0;
  @Input() helper = '';
  @Input() percent = '0%';
  @Input() icon = 'N';
  @Input() tone: 'success' | 'warning' | 'danger' | 'purple' | 'info' = 'info';
  @Input() sparkline: number[] = [35, 42, 38, 58, 44, 62, 70, 64];
}
