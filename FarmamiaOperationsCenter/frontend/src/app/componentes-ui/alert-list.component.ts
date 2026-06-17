import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { StatusBadgeComponent } from './status-badge.component';

type AlertListItem = {
  title: string;
  meta?: string;
  severity?: string;
};

@Component({
  selector: 'app-alert-list',
  standalone: true,
  imports: [CommonModule, StatusBadgeComponent],
  template: `
    <div class="alert-list">
      <div class="alert-row" *ngFor="let item of items">
        <div>
          <strong>{{ item.title }}</strong>
          <small>{{ item.meta }}</small>
        </div>
        <app-status-badge [status]="item.severity || 'INFO'"></app-status-badge>
      </div>
      <p class="empty" *ngIf="items.length === 0">{{ emptyText }}</p>
    </div>
  `,
  styles: [`
    .alert-list {
      display: grid;
      gap: 10px;
    }

    .alert-row {
      align-items: center;
      border-bottom: 1px solid #e5e7eb;
      display: grid;
      gap: 12px;
      grid-template-columns: minmax(0, 1fr) auto;
      padding: 10px 0;
    }

    strong {
      color: #0f172a;
      display: block;
      font-size: 0.92rem;
    }

    small,
    .empty {
      color: #64748b;
    }
  `]
})
export class AlertListComponent {
  @Input() items: AlertListItem[] = [];
  @Input() emptyText = 'Sin alertas.';
}
