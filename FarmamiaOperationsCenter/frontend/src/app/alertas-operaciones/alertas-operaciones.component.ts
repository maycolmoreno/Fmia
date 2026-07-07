import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import {
  ApexAxisChartSeries,
  ApexChart,
  ApexDataLabels,
  ApexStroke,
  ApexXAxis,
  ApexYAxis,
  NgApexchartsModule
} from 'ng-apexcharts';
import { AlertaOperativa, DetalleEquipoPos, MetricaEquipoPos } from '../modelos/modelos-operaciones';
import { OperacionesApiService } from '../servicios/operaciones-api.service';
import { SesionAdminService } from '../servicios/sesion-admin.service';
import { StatusBadgeComponent } from '../componentes-ui/status-badge.component';

type OpcionesGrafico = {
  series: ApexAxisChartSeries;
  chart: ApexChart;
  xaxis: ApexXAxis;
  yaxis: ApexYAxis;
  stroke: ApexStroke;
  dataLabels: ApexDataLabels;
  colors: string[];
};

type CajaAlertas = {
  id: string;
  nombre: string;
  ip?: string | null;
  alertas: AlertaOperativa[];
};

type IncidenteAlertas = {
  principal: AlertaOperativa;
  relacionadas: AlertaOperativa[];
};

type FarmaciaAlertas = {
  branchCode: string;
  alertas: AlertaOperativa[];
  cajas: CajaAlertas[];
};

@Component({
  selector: 'app-alertas-operaciones',
  standalone: true,
  imports: [CommonModule, FormsModule, NgApexchartsModule, StatusBadgeComponent],
  templateUrl: './alertas-operaciones.component.html',
  styleUrl: './alertas-operaciones.component.css'
})
export class AlertasOperacionesComponent implements OnInit {
  alertasCrudo: AlertaOperativa[] = [];
  filtros: { branchCode: string; severity: string; type: string } = { branchCode: '', severity: '', type: '' };
  abiertas = new Set<string>();
  incidentesAbiertos = new Set<string>();
  equipoSeleccionado?: CajaAlertas;
  detalleEquipo?: DetalleEquipoPos;
  historicoMetricas: MetricaEquipoPos[] = [];
  metricaReciente?: MetricaEquipoPos;
  chartOptionsLatencia?: OpcionesGrafico;
  chartOptionsTrafico?: OpcionesGrafico;
  cargando = false;
  cargandoDetalle = false;
  error = '';
  mensaje = '';
  modo: 'activas' | 'historial' = 'activas';

  constructor(
    private readonly api: OperacionesApiService,
    public readonly sesion: SesionAdminService
  ) {
  }

  ngOnInit(): void {
    this.cargarAlertas();
  }

  cargarAlertas(): void {
    this.modo = 'activas';
    this.cargando = true;
    this.error = '';
    this.api.listarAlertas(200, { status: 'OPEN', sort: 'openedAt,desc' }).subscribe({
      next: (alertas) => {
        this.alertasCrudo = alertas;
        this.cargando = false;
      },
      error: (respuesta) => {
        this.error = respuesta?.error?.message ?? 'No se pudieron cargar las alertas.';
        this.cargando = false;
      }
    });
  }

  cargarHistorialAlertas(): void {
    this.modo = 'historial';
    this.cerrarDetalle();
    this.cargando = true;
    this.error = '';
    this.api.listarAlertas(200, { status: 'CLOSED', sort: 'closedAt,desc' }).subscribe({
      next: (alertas) => {
        this.alertasCrudo = alertas;
        this.cargando = false;
      },
      error: (respuesta) => {
        this.error = respuesta?.error?.message ?? 'No se pudo cargar el historial de alertas.';
        this.cargando = false;
      }
    });
  }

  get farmacias(): FarmaciaAlertas[] {
    return this.agruparPorFarmacia(this.filtrarAlertas(this.alertasCrudo));
  }

  limpiarFiltros(): void {
    this.filtros = { branchCode: '', severity: '', type: '' };
  }

  private filtrarAlertas(alertas: AlertaOperativa[]): AlertaOperativa[] {
    const branchCode = this.filtros.branchCode.trim().toLowerCase();
    const severity = this.filtros.severity;
    const type = this.filtros.type.trim().toLowerCase();
    return alertas.filter((alerta) => {
      if (branchCode && !(alerta.branchCode || '').toLowerCase().includes(branchCode)) {
        return false;
      }
      if (severity && alerta.severity?.toUpperCase() !== severity) {
        return false;
      }
      if (type && !alerta.alertType.toLowerCase().includes(type)) {
        return false;
      }
      return true;
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
    this.incidentesAbiertos.clear();
    this.detalleEquipo = undefined;
    this.historicoMetricas = [];
    this.metricaReciente = undefined;
    this.chartOptionsLatencia = undefined;
    this.chartOptionsTrafico = undefined;

    const idDispositivo = caja.alertas.find((alerta) => alerta.deviceId)?.deviceId;
    if (!idDispositivo) {
      return;
    }

    this.cargandoDetalle = true;
    forkJoin({
      detalle: this.api.getDetalleEquipo(idDispositivo),
      historico: this.api.listarHistoricoMetricasEquipo(idDispositivo)
    }).subscribe({
      next: ({ detalle, historico }) => {
        this.detalleEquipo = detalle;
        this.historicoMetricas = historico;
        this.metricaReciente = historico[0] ?? detalle.lastMetric ?? undefined;
        this.inicializarGraficos(historico);
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
    this.historicoMetricas = [];
    this.metricaReciente = undefined;
    this.chartOptionsLatencia = undefined;
    this.chartOptionsTrafico = undefined;
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

  refrescar(): void {
    if (this.modo === 'historial') {
      this.cargarHistorialAlertas();
      return;
    }
    this.cargarAlertas();
  }

  toggleIncidente(idPrincipal: string): void {
    if (this.incidentesAbiertos.has(idPrincipal)) {
      this.incidentesAbiertos.delete(idPrincipal);
      return;
    }
    this.incidentesAbiertos.add(idPrincipal);
  }

  // Agrupa las alertas de un incidente correlacionado bajo la que abrio el incidente
  // (correlationId null => es la raiz; las demas apuntan a su id via correlationId), para que
  // el operador vea "enlace caido (+3 alertas relacionadas)" en vez de 4 filas sueltas.
  agruparPorIncidente(alertas: AlertaOperativa[]): IncidenteAlertas[] {
    const porRaiz = new Map<string, AlertaOperativa[]>();
    for (const alerta of alertas) {
      const raiz = alerta.correlationId || alerta.id;
      porRaiz.set(raiz, [...(porRaiz.get(raiz) ?? []), alerta]);
    }

    return Array.from(porRaiz.values()).map((grupo) => {
      const principal = grupo.find((alerta) => !alerta.correlationId)
        ?? [...grupo].sort((a, b) => new Date(a.openedAt).getTime() - new Date(b.openedAt).getTime())[0];
      return {
        principal,
        relacionadas: grupo.filter((alerta) => alerta.id !== principal.id)
      };
    });
  }

  severidadPrincipal(alertas: AlertaOperativa[]): string {
    return [...alertas].sort((a, b) => this.pesoSeveridad(b.severity) - this.pesoSeveridad(a.severity))[0]?.severity ?? 'INFO';
  }

  formatearUptime(ticks?: number | null): string {
    if (!ticks) {
      return 'N/D';
    }

    let segundos = Math.floor(ticks / 100);
    const dias = Math.floor(segundos / 86400);
    segundos %= 86400;
    const horas = Math.floor(segundos / 3600);
    segundos %= 3600;
    const minutos = Math.floor(segundos / 60);
    segundos %= 60;
    return `${dias} days, ${this.dosDigitos(horas)}:${this.dosDigitos(minutos)}:${this.dosDigitos(segundos)}`;
  }

  equipoOnline(): boolean {
    return this.detalleEquipo?.device?.status === 'ONLINE';
  }

  inicializarGraficos(historico: MetricaEquipoPos[]): void {
    const datos = [...historico].reverse();
    const categorias = datos.map((metrica) => new Date(metrica.collectedAt).toLocaleTimeString([], {
      hour: '2-digit',
      minute: '2-digit'
    }));

    this.chartOptionsLatencia = this.crearOpcionesGrafico(
      [{ name: 'LATENCIA', data: datos.map((metrica) => metrica.responseTimeMs ?? metrica.latencyMs ?? 0) }],
      categorias,
      ['#2563eb'],
      'ms'
    );

    this.chartOptionsTrafico = this.crearOpcionesGrafico(
      [
        { name: 'INBOUND', data: datos.map((metrica) => metrica.inboundTrafficKbps ?? 0) },
        { name: 'OUTBOUND', data: datos.map((metrica) => metrica.outboundTrafficKbps ?? 0) }
      ],
      categorias,
      ['#2563eb', '#dc2626'],
      'Kbps'
    );
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

  private crearOpcionesGrafico(
    series: ApexAxisChartSeries,
    categorias: string[],
    colors: string[],
    tituloY: string
  ): OpcionesGrafico {
    return {
      series,
      chart: {
        type: 'line',
        height: 220,
        animations: { enabled: false },
        toolbar: { show: false },
        zoom: { enabled: false }
      },
      colors,
      stroke: { curve: 'smooth', width: 2 },
      dataLabels: { enabled: false },
      xaxis: { categories: categorias, labels: { rotate: 0 } },
      yaxis: { title: { text: tituloY }, min: 0 }
    };
  }

  private dosDigitos(valor: number): string {
    return valor.toString().padStart(2, '0');
  }
}
