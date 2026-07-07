import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

export type EstadoCruz = 'normal' | 'riesgo' | 'critico' | 'inactivo';

// Glifo de estado del sistema: una cruz de farmacia abstraida, no un punto/circulo
// generico. Rellena (solida) cuando hay algo que atender, contorneada cuando todo
// esta en orden -- el relleno es una segunda senal ademas del color, para que el
// estado se distinga incluso sin percibir el color (daltonismo).
@Component({
  selector: 'app-cruz-glifo',
  standalone: true,
  imports: [CommonModule],
  template: `
    <svg
      [attr.width]="tamano"
      [attr.height]="tamano"
      viewBox="0 0 24 24"
      [ngClass]="estado"
      [class.pulso]="estado === 'critico'"
      role="img"
      [attr.aria-label]="etiquetaAria"
    >
      <rect x="9.3" y="1.5" width="5.4" height="21" rx="1.6" />
      <rect x="1.5" y="9.3" width="21" height="5.4" rx="1.6" />
    </svg>
  `,
  styles: [`
    svg {
      flex-shrink: 0;
      transition: fill 0.2s ease, stroke 0.2s ease;
    }

    .normal rect {
      fill: none;
      stroke: var(--color-muted, #8a9a90);
      stroke-width: 1.4;
    }

    .riesgo rect {
      fill: var(--color-warning, #e8a33d);
    }

    .critico rect {
      fill: var(--color-danger, #e1473b);
    }

    .inactivo rect {
      fill: none;
      stroke: var(--color-border, #24352c);
      stroke-width: 1.4;
    }

    @media (prefers-reduced-motion: no-preference) {
      .pulso {
        animation: cruz-pulso 2.4s ease-in-out infinite;
      }
    }

    @keyframes cruz-pulso {
      0%, 100% { opacity: 1; }
      50% { opacity: 0.72; }
    }
  `]
})
export class CruzGlifoComponent {
  @Input() estado: EstadoCruz = 'normal';
  @Input() tamano = 24;

  get etiquetaAria(): string {
    const textos: Record<EstadoCruz, string> = {
      normal: 'Estado normal',
      riesgo: 'Estado en riesgo',
      critico: 'Estado critico',
      inactivo: 'Sin datos'
    };
    return textos[this.estado];
  }
}
