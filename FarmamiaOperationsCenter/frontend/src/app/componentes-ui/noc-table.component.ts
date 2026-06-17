import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-noc-table',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="noc-table">
      <header *ngIf="title || subtitle">
        <div>
          <h2 *ngIf="title">{{ title }}</h2>
          <small *ngIf="subtitle">{{ subtitle }}</small>
        </div>
        <ng-content select="[table-actions]"></ng-content>
      </header>
      <ng-content></ng-content>
    </section>
  `,
  styles: [`
    .noc-table {
      display: grid;
      gap: 14px;
    }

    header {
      align-items: center;
      display: flex;
      gap: 16px;
      justify-content: space-between;
    }

    h2 {
      color: #0f172a;
      font-size: 1rem;
      margin: 0;
    }

    small {
      color: #64748b;
      display: block;
      margin-top: 4px;
    }
  `]
})
export class NocTableComponent {
  @Input() title = '';
  @Input() subtitle = '';
}
