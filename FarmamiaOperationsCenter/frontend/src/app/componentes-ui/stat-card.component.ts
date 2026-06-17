import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-stat-card',
  standalone: true,
  template: `
    <article class="stat-card">
      <span>{{ label }}</span>
      <strong>{{ value }}</strong>
      <small>{{ helper }}</small>
    </article>
  `,
  styles: [`
    .stat-card {
      background: #ffffff;
      border: 1px solid #e5e7eb;
      border-radius: 12px;
      box-shadow: 0 10px 28px rgb(15 23 42 / 5%);
      padding: 18px;
    }

    span,
    small {
      color: #64748b;
      display: block;
      font-size: 0.82rem;
      font-weight: 600;
    }

    strong {
      color: #0f172a;
      display: block;
      font-size: 1.8rem;
      line-height: 1;
      margin: 8px 0;
    }
  `]
})
export class StatCardComponent {
  @Input() label = '';
  @Input() value: string | number = 0;
  @Input() helper = '';
}
