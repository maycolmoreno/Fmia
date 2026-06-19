import { AsyncPipe, CommonModule } from '@angular/common';
import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ContadoresEnlaces, EstadoOperacionalFarmacia, ResumenNocDashboard } from '../modelos/modelos-operaciones';
import { NocDashboardService } from '../servicios/noc-dashboard.service';
import { NocZonaAlertasComponent } from './zonas/noc-zona-alertas.component';
import { NocZonaCampanaComponent } from './zonas/noc-zona-campana.component';
import { NocZonaCriticoComponent } from './zonas/noc-zona-critico.component';
import { NocZonaPosComponent } from './zonas/noc-zona-pos.component';
import { NocZonaRedComponent } from './zonas/noc-zona-red.component';
import { environment } from '../../environments/environment';
import { KpiCardComponent } from '../componentes-ui/kpi-card.component';

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
    NocZonaAlertasComponent,
    KpiCardComponent
  ],
  templateUrl: './dashboard-noc.component.html',
  styleUrl: './dashboard-noc.component.css'
})
export class DashboardNocComponent implements OnInit, OnDestroy {
  private readonly destroy$ = new Subject<void>();

  resumen: ResumenNocDashboard | null = null;
  contadoresEnlaces: ContadoresEnlaces = { total: 0, up: 0, down: 0 };
  cargando = true;
  error = false;
  readonly grafanaUrl = environment.grafanaUrl;

  @Input() estadoFarmacias: EstadoOperacionalFarmacia[] = [];

  farmaciaDetalleNoc?: EstadoOperacionalFarmacia;

  constructor(readonly nocService: NocDashboardService) {}

  ngOnInit(): void {
    this.nocService.iniciarRefresco();

    this.nocService.resumen$.pipe(takeUntil(this.destroy$)).subscribe(r => {
      this.resumen = r;
    });
    this.nocService.cargando$.pipe(takeUntil(this.destroy$)).subscribe(c => {
      this.cargando = c;
    });
    this.nocService.error$.pipe(takeUntil(this.destroy$)).subscribe(e => {
      this.error = e;
    });
    this.nocService.contadoresEnlaces$.pipe(takeUntil(this.destroy$)).subscribe(contadores => {
      this.contadoresEnlaces = contadores;
    });
    // El servicio refresca el estado operacional de farmacias en el mismo ciclo de 30 s;
    // mantiene fresco el panel de detalle, los badges de campana y el filtro de turno.
    this.nocService.estadoFarmacias$.pipe(takeUntil(this.destroy$)).subscribe(farmacias => {
      if (farmacias.length > 0) {
        this.estadoFarmacias = farmacias;
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.nocService.detenerRefresco();
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
