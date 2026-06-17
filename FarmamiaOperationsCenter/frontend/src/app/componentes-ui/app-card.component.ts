import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-card',
  standalone: true,
  imports: [CommonModule],
  template: `
    <article class="app-card" [class.app-card-accent]="accent">
      <header class="app-card-header" *ngIf="title || subtitle">
        <div>
          <h2 *ngIf="title">{{ title }}</h2>
          <small *ngIf="subtitle">{{ subtitle }}</small>
        </div>
        <ng-content select="[card-actions]"></ng-content>
      </header>
      <ng-content></ng-content>
    </article>
  `,
  styles: [`
    .app-card {
      background: #ffffff;
      border: 1px solid #e5e7eb;
      border-radius: 12px;
      box-shadow: 0 10px 28px rgb(15 23 42 / 6%);
      padding: 24px;
    }

    .app-card-accent {
      border-top: 3px solid #2563eb;
    }

    .app-card-header {
      align-items: flex-start;
      display: flex;
      gap: 16px;
      justify-content: space-between;
      margin-bottom: 16px;
    }

    h2 {
      color: #0f172a;
      font-size: 1rem;
      font-weight: 700;
      line-height: 1.3;
      margin: 0;
    }

    small {
      color: #64748b;
      display: block;
      font-size: 0.86rem;
      margin-top: 4px;
    }
  `]
})
export class AppCardComponent {
  @Input() title = '';
  @Input() subtitle = '';
  @Input() accent = false;
}
