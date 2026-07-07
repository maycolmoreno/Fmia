import { AsyncPipe, CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { EstadoOperacionalFarmacia, ProblemaAbiertoNoc, ResumenNocDashboard } from '../modelos/modelos-operaciones';
import { NocDashboardService } from '../servicios/noc-dashboard.service';
import { NocZonaCampanaComponent } from './zonas/noc-zona-campana.component';
import { NocZonaPosComponent } from './zonas/noc-zona-pos.component';
import { NocZonaRedComponent } from './zonas/noc-zona-red.component';
import { StatCardComponent } from '../componentes-ui/stat-card.component';
import { environment } from '../../environments/environment';

const PESO_SEVERIDAD: Record<string, number> = {
  CRITICAL: 5,
  HIGH: 4,
  WARNING: 3,
  MEDIUM: 2,
  LOW: 1
};

@Component({
  selector: 'app-dashboard-noc',
  standalone: true,
  imports: [
    CommonModule,
    AsyncPipe,
    NocZonaRedComponent,
    NocZonaPosComponent,
    NocZonaCampanaComponent,
    StatCardComponent
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
  @Output() verEquipo = new EventEmitter<string>();

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

  get problemasAbiertos(): ProblemaAbiertoNoc[] {
    if (!this.resumen) {
      return [];
    }

    const deAlertas: ProblemaAbiertoNoc[] = this.resumen.recentAlerts.map(a => ({
      codigo: a.deviceCode || a.farmCode || 'N/D',
      tipo: a.alertType,
      estado: a.status,
      severidad: a.severity?.toUpperCase() ?? 'INFO',
      descripcion: a.title || a.alertType,
      iniciadoEn: a.openedAt,
      accion: a.networkEvent ? 'grafana' : null,
      branchCode: a.farmCode,
      deviceId: null
    }));

    const dePos: ProblemaAbiertoNoc[] = this.resumen.pos.pendingDevices.map(d => ({
      codigo: d.deviceName || 'N/D',
      tipo: 'POS_NO_ACTUALIZADO',
      estado: 'ABIERTO',
      severidad: d.targetStatus === 'FAILED' || d.targetStatus === 'ROLLBACK_FAILED' ? 'HIGH' : 'WARNING',
      descripcion: d.targetVersion ? `No recibió la versión ${d.targetVersion}` : 'No recibió la actualización de POS',
      iniciadoEn: d.lastUpdatedAt || this.resumen!.generatedAt,
      accion: 'equipo',
      branchCode: d.branchCode,
      deviceId: d.deviceId
    }));

    return [...deAlertas, ...dePos].sort((x, y) => {
      const pesoX = PESO_SEVERIDAD[x.severidad] ?? 0;
      const pesoY = PESO_SEVERIDAD[y.severidad] ?? 0;
      if (pesoY !== pesoX) {
        return pesoY - pesoX;
      }
      return new Date(x.iniciadoEn).getTime() - new Date(y.iniciadoEn).getTime();
    });
  }

  get totalProblemasAbiertos(): number {
    return this.problemasAbiertos.length;
  }

  urlGrafanaFarmacia(branchCode: string | null): string {
    return `${this.grafanaUrl}/d/farmacia-enlace?var-branch=${branchCode || ''}&from=now-24h&to=now`;
  }

  onAccion(problema: ProblemaAbiertoNoc): void {
    if (problema.accion === 'equipo' && problema.deviceId) {
      this.verEquipo.emit(problema.deviceId);
    }
  }

  edadProblema(fecha: string): string {
    const ms = Date.now() - new Date(fecha).getTime();
    const minutos = Math.max(1, Math.floor(ms / 60_000));
    if (minutos < 60) {
      return `${minutos} min`;
    }
    const horas = Math.floor(minutos / 60);
    if (horas < 24) {
      return `${horas} h`;
    }
    return `${Math.floor(horas / 24)} d`;
  }

  claseSeveridad(severidad: string): Record<string, boolean> {
    const valor = severidad?.toUpperCase();
    return {
      'sev-disaster': valor === 'CRITICAL' || valor === 'DISASTER',
      'sev-alta': valor === 'HIGH',
      'sev-warning': valor === 'WARNING',
      'sev-info': valor !== 'CRITICAL' && valor !== 'DISASTER' && valor !== 'HIGH' && valor !== 'WARNING'
    };
  }
}
