import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { AlertaOperativa, DetalleEquipoPos } from '../modelos/modelos-operaciones';
import { OperacionesApiService } from '../servicios/operaciones-api.service';
import { SesionAdminService } from '../servicios/sesion-admin.service';
import { StatusBadgeComponent } from '../componentes-ui/status-badge.component';

type CajaAlertas = {
  id: string;
  nombre: string;
  ip?: string | null;
  alertas: AlertaOperativa[];
};

type FarmaciaAlertas = {
  branchCode: string;
  alertas: AlertaOperativa[];
  cajas: CajaAlertas[];
};

@Component({
  selector: 'app-alertas-operaciones',
  standalone: true,
  imports: [CommonModule, StatusBadgeComponent],
  templateUrl: './alertas-operaciones.component.html',
  styleUrl: './alertas-operaciones.component.css'
})
export class AlertasOperacionesComponent implements OnInit {
  farmacias: FarmaciaAlertas[] = [];
  abiertas = new Set<string>();
  equipoSeleccionado?: CajaAlertas;
  detalleEquipo?: DetalleEquipoPos;
  cargando = false;
  cargandoDetalle = false;
  error = '';
  mensaje = '';

  constructor(
    private readonly api: OperacionesApiService,
    public readonly sesion: SesionAdminService
  ) {
  }

  ngOnInit(): void {
    this.cargarAlertas();
  }

  cargarAlertas(): void {
    this.cargando = true;
    this.error = '';
    this.api.listarAlertas(200, { status: 'OPEN', sort: 'openedAt,desc' }).subscribe({
      next: (alertas) => {
        this.farmacias = this.agruparPorFarmacia(alertas);
        this.cargando = false;
      },
      error: (respuesta) => {
        this.error = respuesta?.error?.message ?? 'No se pudieron cargar las alertas.';
        this.cargando = false;
      }
    });
  }

  toggleFarmacia(branchCode: string): void {
    if (this.abiertas.has(branchCode)) {
      this.abiertas.delete(branchCode);
      return;
    }
    this.abiertas.add(branchCode);
  }

  abrirDetalleEquipo(idEquipo: string): void {
    const caja = this.farmacias
      .flatMap((farmacia) => farmacia.cajas)
      .find((caja) => caja.id === idEquipo);
    if (!caja) {
      return;
    }

    this.equipoSeleccionado = caja;
    this.detalleEquipo = undefined;

    const idDispositivo = caja.alertas.find((alerta) => alerta.deviceId)?.deviceId;
    if (!idDispositivo) {
      return;
    }

    this.cargandoDetalle = true;
    this.api.getDetalleEquipo(idDispositivo).subscribe({
      next: (detalle) => {
        this.detalleEquipo = detalle;
        this.cargandoDetalle = false;
      },
      error: (respuesta) => {
        this.error = respuesta?.error?.message ?? 'No se pudieron cargar los datos tecnicos del equipo.';
        this.cargandoDetalle = false;
      }
    });
  }

  cerrarDetalle(): void {
    this.equipoSeleccionado = undefined;
    this.detalleEquipo = undefined;
    this.cargandoDetalle = false;
  }

  reconocer(alerta: AlertaOperativa): void {
    this.api.reconocerAlerta(alerta.id).subscribe({
      next: () => {
        this.mensaje = 'Alerta reconocida.';
        this.cargarAlertas();
      },
      error: (respuesta) => this.error = respuesta?.error?.message ?? 'No se pudo reconocer la alerta.'
    });
  }

  cerrar(alerta: AlertaOperativa): void {
    this.api.cerrarAlerta(alerta.id).subscribe({
      next: () => {
        this.mensaje = 'Alerta cerrada.';
        this.cargarAlertas();
        if (this.equipoSeleccionado?.alertas.length === 1) {
          this.cerrarDetalle();
        }
      },
      error: (respuesta) => this.error = respuesta?.error?.message ?? 'No se pudo cerrar la alerta.'
    });
  }

  severidadPrincipal(alertas: AlertaOperativa[]): string {
    return [...alertas].sort((a, b) => this.pesoSeveridad(b.severity) - this.pesoSeveridad(a.severity))[0]?.severity ?? 'INFO';
  }

  private agruparPorFarmacia(alertas: AlertaOperativa[]): FarmaciaAlertas[] {
    const porFarmacia = new Map<string, AlertaOperativa[]>();
    for (const alerta of alertas) {
      const branchCode = alerta.branchCode || 'SIN_FARMACIA';
      porFarmacia.set(branchCode, [...(porFarmacia.get(branchCode) ?? []), alerta]);
    }

    return Array.from(porFarmacia.entries())
      .map(([branchCode, alertasFarmacia]) => ({
        branchCode,
        alertas: alertasFarmacia,
        cajas: this.agruparPorCaja(branchCode, alertasFarmacia)
      }))
      .sort((a, b) => this.pesoSeveridad(this.severidadPrincipal(b.alertas)) - this.pesoSeveridad(this.severidadPrincipal(a.alertas)));
  }

  private agruparPorCaja(branchCode: string, alertas: AlertaOperativa[]): CajaAlertas[] {
    const porCaja = new Map<string, AlertaOperativa[]>();
    for (const alerta of alertas) {
      const id = alerta.deviceId || `red:${branchCode}`;
      porCaja.set(id, [...(porCaja.get(id) ?? []), alerta]);
    }

    return Array.from(porCaja.entries()).map(([id, alertasCaja]) => ({
      id,
      nombre: alertasCaja[0].hostname || 'Red de farmacia',
      ip: null,
      alertas: alertasCaja
    }));
  }

  private pesoSeveridad(severidad: string): number {
    return { CRITICAL: 5, HIGH: 4, WARNING: 3, MEDIUM: 2, LOW: 1 }[severidad?.toUpperCase()] ?? 0;
  }
}
