import { AsyncPipe, CommonModule } from '@angular/common';
import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { EstadoOperacionalFarmacia, ResumenNocDashboard } from '../modelos/modelos-operaciones';
import { NocDashboardService } from '../servicios/noc-dashboard.service';
import { NocZonaAlertasComponent } from './zonas/noc-zona-alertas.component';
import { NocZonaCampanaComponent } from './zonas/noc-zona-campana.component';
import { NocZonaCriticoComponent } from './zonas/noc-zona-critico.component';
import { NocZonaPosComponent } from './zonas/noc-zona-pos.component';
import { NocZonaRedComponent } from './zonas/noc-zona-red.component';
import { environment } from '../../environments/environment';

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
    NocZonaAlertasComponent
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
    this.nocService.iniciar();

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
  }

  seleccionarFarmaciaDetalleNoc(codigo: string): void {
    const encontrado = this.estadoFarmacias.find(f => f.codigoFarmacia === codigo);
    this.farmaciaDetalleNoc = this.farmaciaDetalleNoc?.codigoFarmacia === codigo ? undefined : encontrado;
  }

  cerrarDetalleNoc(): void {
    this.farmaciaDetalleNoc = undefined;
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
}
