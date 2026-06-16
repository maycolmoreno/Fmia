import { AsyncPipe, CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ResumenNocDashboard } from '../modelos/modelos-operaciones';
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
}
