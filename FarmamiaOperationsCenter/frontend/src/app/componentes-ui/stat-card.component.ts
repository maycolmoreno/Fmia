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
    :host {
      display: block;
    }
    .stat-card {
      background: var(--color-card, #161b22);
      border: 1px solid var(--color-border, #30363d);
      border-radius: 12px;
      box-shadow: var(--shadow-card, 0 8px 24px rgb(0 0 0 / 40%));
      padding: 18px;
      transition: background 0.2s ease, border-color 0.2s ease;
    }

    span,
    small {
      color: var(--color-muted, #8b949e);
      display: block;
      font-size: 0.82rem;
      font-weight: 600;
    }

    strong {
      color: var(--color-text, #e6edf3);
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
