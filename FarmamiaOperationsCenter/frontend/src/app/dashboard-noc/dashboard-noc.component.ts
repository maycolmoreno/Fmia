import { AsyncPipe, CommonModule } from '@angular/common';
import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { EstadoOperacionalFarmacia, ResumenNocDashboard } from '../modelos/modelos-operaciones';
import { NocDashboardService } from '../servicios/noc-dashboard.service';
import { NocZonaCampanaComponent } from './zonas/noc-zona-campana.component';
import { NocZonaCriticoComponent } from './zonas/noc-zona-critico.component';
import { NocZonaPosComponent } from './zonas/noc-zona-pos.component';
import { NocZonaRedComponent } from './zonas/noc-zona-red.component';
import { environment } from '../../environments/environment';
import { CruzGlifoComponent, EstadoCruz } from '../componentes-ui/cruz-glifo.component';

@Component({
  selector: 'app-dashboard-noc',
  standalone: true,
  imports: [
    CommonModule,
    AsyncPipe,
    NocZonaCriticoComponent,
    NocZonaRedComponent,
    NocZonaPosComponent,
    NocZonaCampanaComponent,
    CruzGlifoComponent
  ],
  templateUrl: './dashboard-noc.component.html',
  styleUrl: './dashboard-noc.component.css'
})
export class DashboardNocComponent implements OnInit, OnDestroy {
  private readonly destroy$ = new Subject<void>();

  resumen: ResumenNocDashboard | null = null;
  cargando = true;
  error = false;
  readonly grafanaUrl = environment.grafanaUrl;

  @Input() estadoFarmacias: EstadoOperacionalFarmacia[] = [];

  farmaciaDetalleNoc?: EstadoOperacionalFarmacia;

  constructor(readonly nocService: NocDashboardService) {}

  ngOnInit(): void {
    this.nocService.resumen$.pipe(takeUntil(this.destroy$)).subscribe(r => {
      this.resumen = r;
    });
    this.nocService.cargando$.pipe(takeUntil(this.destroy$)).subscribe(c => {
      this.cargando = c;
    });
    this.nocService.error$.pipe(takeUntil(this.destroy$)).subscribe(e => {
      this.error = e;
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    // El ciclo de polling lo gestiona app.component; no lo detenemos aquí.
  }

  seleccionarFarmaciaDetalleNoc(codigo: string): void {
    const encontrado = this.estadoFarmacias.find(f => f.codigoFarmacia === codigo);
    this.farmaciaDetalleNoc = this.farmaciaDetalleNoc?.codigoFarmacia === codigo ? undefined : encontrado;
  }

  cerrarDetalleNoc(): void {
    this.farmaciaDetalleNoc = undefined;
  }

  get alertasAbiertas(): number {
    return this.resumen?.recentAlerts.filter(a => a.status !== 'CLOSED').length ?? 0;
  }

  get alertasRed(): number {
    return this.resumen?.recentAlerts.filter(a => a.networkEvent && a.status !== 'CLOSED').length ?? 0;
  }

  get alertasCriticas(): number {
    return this.resumen?.recentAlerts.filter(a => a.severity === 'CRITICAL' && a.status !== 'CLOSED').length ?? 0;
  }

  get codigosFarmaciasTurno(): Set<string> {
    return new Set(this.estadoFarmacias.filter(f => f.deTurno).map(f => f.codigoFarmacia));
  }

  // Veredicto de flota: el titular real de la pantalla — la pregunta que un operador
  // necesita responder en segundos al llegar a su turno, no una grilla de KPIs iguales.
  get totalFarmacias(): number {
    return this.estadoFarmacias.length;
  }

  get farmaciasOk(): number {
    if (!this.resumen) {
      return 0;
    }
    return Math.max(0, this.totalFarmacias - this.resumen.criticFarms.length - this.resumen.atRiskFarms.length);
  }

  get estadoVeredicto(): EstadoCruz {
    if (!this.resumen) {
      return 'inactivo';
    }
    if (this.resumen.criticFarms.length > 0) {
      return 'critico';
    }
    if (this.resumen.atRiskFarms.length > 0) {
      return 'riesgo';
    }
    return 'normal';
  }

  get tituloVeredicto(): string {
    if (!this.resumen) {
      return 'Sin datos';
    }
    if (this.resumen.criticFarms.length > 0) {
      const cantidad = this.resumen.criticFarms.length;
      return `${cantidad} farmacia${cantidad === 1 ? '' : 's'} crítica${cantidad === 1 ? '' : 's'}`;
    }
    if (this.resumen.atRiskFarms.length > 0) {
      const cantidad = this.resumen.atRiskFarms.length;
      return `${cantidad} farmacia${cantidad === 1 ? '' : 's'} en riesgo`;
    }
    return 'Todo normal';
  }

  urlGrafanaFarmacia(branchCode: string): string {
    return `${this.grafanaUrl}/d/farmacia-enlace?var-branch=${branchCode}&from=now-24h&to=now`;
  }

  claseEstadoNoc(estado: string): Record<string, boolean> {
    return {
      'detalle-noc-critico': estado === 'CRITICA',
      'detalle-noc-riesgo': estado === 'EN_RIESGO' || estado === 'TURNO_EN_RIESGO',
      'detalle-noc-normal': estado === 'NORMAL'
    };
  }

  edadAlerta(fecha: string): string {
    const ms = Date.now() - new Date(fecha).getTime();
    const minutos = Math.max(1, Math.floor(ms / 60_000));
    if (minutos < 60) {
      return `${minutos} minutes`;
    }
    const horas = Math.floor(minutos / 60);
    if (horas < 24) {
      return `${horas} hours`;
    }
    return `${Math.floor(horas / 24)} days`;
  }

  claseSeveridad(severidad: string): Record<string, boolean> {
    const valor = severidad?.toUpperCase();
    return {
      'sev-disaster': valor === 'CRITICAL' || valor === 'DISASTER',
      'sev-warning': valor === 'WARNING',
      'sev-info': valor !== 'CRITICAL' && valor !== 'DISASTER' && valor !== 'WARNING'
    };
  }
}
