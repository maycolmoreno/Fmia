import { CommonModule } from '@angular/common';
import { Component, Input, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import {
  Despliegue,
  EstadoDespliegue,
  OleadaOrquestacion,
  PaquetePos,
  PlanOrquestacion,
  SolicitudCrearDespliegue
} from '../modelos/modelos-operaciones';
import { StatusBadgeComponent } from '../componentes-ui/status-badge.component';
import { OperacionesApiService } from '../servicios/operaciones-api.service';
import { SesionAdminService } from '../servicios/sesion-admin.service';

@Component({
  selector: 'app-actualizaciones-operaciones',
  standalone: true,
  imports: [CommonModule, FormsModule, StatusBadgeComponent],
  templateUrl: './actualizaciones-operaciones.component.html',
  styleUrl: './actualizaciones-operaciones.component.css'
})
export class ActualizacionesOperacionesComponent implements OnInit {
  /** Mapa de progreso SSE keyed por idEquipo, compartido desde AppComponent. */
  @Input() progresoDescarga: Map<string, number> = new Map();

  tab: 'versiones' | 'monitor' = 'versiones';
  paquetes: PaquetePos[] = [];
  despliegues: Despliegue[] = [];
  despliegueSeleccionado?: Despliegue;
  estadoSeleccionado?: EstadoDespliegue;
  planOrquestacion?: PlanOrquestacion;
  guardandoPaquete = false;
  guardandoDespliegue = false;
  mensaje = '';
  error = '';

  paqueteFormulario = {
    version: '',
    archivo: undefined as File | undefined
  };

  despliegueFormulario = {
    packageId: '',
    name: '',
    description: '',
    scheduledAt: '',
    targetGroup: '',
    pilot: true,
    deviceIds: ''
  };

  orquestacionFormulario = {
    maxFailurePercent: 10,
    autoPauseEnabled: true,
    retryLimit: 2,
    maxParallelDevices: 25,
    maintenanceWindowStart: '',
    maintenanceWindowEnd: ''
  };

  /** Número de equipos reportando descarga activa (desde el estado de la campaña). */
  get equiposDescargando(): number {
    return this.estadoSeleccionado?.targetsByStatus?.['DOWNLOADING'] ?? 0;
  }

  /** Progreso promedio de todos los equipos que están descargando ahora mismo. */
  get progresoPromedioDescarga(): number {
    if (this.progresoDescarga.size === 0) return 0;
    const valores = [...this.progresoDescarga.values()];
    return valores.reduce((s, v) => s + v, 0) / valores.length;
  }

  constructor(
    private readonly api: OperacionesApiService,
    public readonly sesion: SesionAdminService
  ) {
  }

  ngOnInit(): void {
    this.recargar();
  }

  recargar(): void {
    this.cargarPaquetes();
    this.cargarDespliegues();
  }

  seleccionarArchivo(evento: Event): void {
    const entrada = evento.target as HTMLInputElement;
    this.paqueteFormulario.archivo = entrada.files?.[0];
  }

  cargarPaquete(): void {
    if (!this.paqueteFormulario.version || !this.paqueteFormulario.archivo) {
      this.error = 'Selecciona una version y un archivo ZIP.';
      return;
    }

    this.guardandoPaquete = true;
    this.error = '';
    this.api.cargarPaquete(this.paqueteFormulario.version, this.paqueteFormulario.archivo)
      .pipe(finalize(() => this.guardandoPaquete = false))
      .subscribe({
        next: () => {
          this.mensaje = 'Version POS cargada.';
          this.paqueteFormulario = { version: '', archivo: undefined };
          this.cargarPaquetes();
        },
        error: (respuesta) => this.error = respuesta?.error?.message ?? 'No se pudo cargar la version POS.'
      });
  }

  aprobarPaquete(paquete: PaquetePos): void {
    this.api.aprobarPaquete(paquete.id).subscribe({
      next: () => {
        this.mensaje = 'Version POS aprobada.';
        this.cargarPaquetes();
      },
      error: (respuesta) => this.error = respuesta?.error?.message ?? 'No se pudo aprobar la version POS.'
    });
  }

  retirarPaquete(paquete: PaquetePos): void {
    this.api.retirarPaquete(paquete.id).subscribe({
      next: () => {
        this.mensaje = 'Version POS retirada.';
        this.cargarPaquetes();
      },
      error: (respuesta) => this.error = respuesta?.error?.message ?? 'No se pudo retirar la version POS.'
    });
  }

  descargarPaquete(paquete: PaquetePos): void {
    this.api.descargarPaquete(paquete.id).subscribe({
      next: (contenido) => {
        const url = URL.createObjectURL(contenido);
        const enlace = document.createElement('a');
        enlace.href = url;
        enlace.download = paquete.fileName || `farmamia-pos-${paquete.version}.zip`;
        enlace.click();
        URL.revokeObjectURL(url);
      },
      error: (respuesta) => this.error = respuesta?.error?.message ?? 'No se pudo descargar la version POS.'
    });
  }

  crearDespliegue(): void {
    if (!this.despliegueFormulario.packageId || !this.despliegueFormulario.name) {
      this.error = 'Completa version y nombre de campana.';
      return;
    }

    const solicitud: SolicitudCrearDespliegue = {
      packageId: this.despliegueFormulario.packageId,
      name: this.despliegueFormulario.name,
      description: this.despliegueFormulario.description || null,
      scheduledAt: this.normalizarFecha(this.despliegueFormulario.scheduledAt),
      targetGroup: this.despliegueFormulario.targetGroup || null,
      pilot: this.despliegueFormulario.pilot,
      deviceIds: this.idsEquipos()
    };

    this.guardandoDespliegue = true;
    this.error = '';
    this.api.crearDespliegue(solicitud)
      .pipe(finalize(() => this.guardandoDespliegue = false))
      .subscribe({
        next: () => {
          this.mensaje = 'Campana POS creada.';
          this.despliegueFormulario.name = '';
          this.despliegueFormulario.description = '';
          this.despliegueFormulario.deviceIds = '';
          this.cargarDespliegues();
          this.tab = 'monitor';
        },
        error: (respuesta) => this.error = respuesta?.error?.message ?? 'No se pudo crear la campana POS.'
      });
  }

  seleccionarDespliegue(despliegue: Despliegue): void {
    this.despliegueSeleccionado = despliegue;
    this.estadoSeleccionado = undefined;
    this.planOrquestacion = undefined;
    this.api.obtenerEstadoDespliegue(despliegue.id).subscribe({
      next: (estado) => this.estadoSeleccionado = estado
    });
    this.api.obtenerPlanOrquestacion(despliegue.id).subscribe({
      next: (plan) => this.planOrquestacion = plan,
      error: () => this.planOrquestacion = undefined
    });
  }

  planificarOrquestacion(): void {
    if (!this.despliegueSeleccionado) {
      this.error = 'Selecciona una campana.';
      return;
    }
    this.api.planificarOrquestacion(this.despliegueSeleccionado.id, {
      maxFailurePercent: this.orquestacionFormulario.maxFailurePercent,
      autoPauseEnabled: this.orquestacionFormulario.autoPauseEnabled,
      retryLimit: this.orquestacionFormulario.retryLimit,
      maxParallelDevices: this.orquestacionFormulario.maxParallelDevices,
      maintenanceWindowStart: this.orquestacionFormulario.maintenanceWindowStart || null,
      maintenanceWindowEnd: this.orquestacionFormulario.maintenanceWindowEnd || null
    }).subscribe({
      next: (plan) => {
        this.planOrquestacion = plan;
        this.mensaje = 'Plan de orquestacion generado.';
      },
      error: (respuesta) => this.error = respuesta?.error?.message ?? 'No se pudo planificar la orquestacion.'
    });
  }

  evaluarOrquestacion(): void {
    if (!this.despliegueSeleccionado) {
      return;
    }
    this.api.evaluarOrquestacion(this.despliegueSeleccionado.id).subscribe({
      next: (plan) => this.planOrquestacion = plan,
      error: (respuesta) => this.error = respuesta?.error?.message ?? 'No se pudo evaluar la orquestacion.'
    });
  }

  lanzarCampana(despliegue: Despliegue): void {
    this.api.lanzarDespliegue(despliegue.id).subscribe({ next: () => this.cargarDespliegues() });
  }

  pausarDespliegue(despliegue: Despliegue): void {
    this.api.pausarDespliegue(despliegue.id).subscribe({ next: () => this.cargarDespliegues() });
  }

  reanudarDespliegue(despliegue: Despliegue): void {
    this.api.reanudarDespliegue(despliegue.id).subscribe({ next: () => this.cargarDespliegues() });
  }

  cancelarDespliegue(despliegue: Despliegue): void {
    this.api.cancelarDespliegue(despliegue.id).subscribe({ next: () => this.cargarDespliegues() });
  }

  aprobarDespliegue(despliegue: Despliegue): void {
    this.api.aprobarDespliegue(despliegue.id).subscribe({ next: () => this.cargarDespliegues() });
  }

  iniciarOleada(oleada: OleadaOrquestacion): void {
    this.operarOleada(oleada, 'start');
  }

  pausarOleada(oleada: OleadaOrquestacion): void {
    this.operarOleada(oleada, 'pause');
  }

  reanudarOleada(oleada: OleadaOrquestacion): void {
    this.operarOleada(oleada, 'resume');
  }

  progresoOleada(oleada: OleadaOrquestacion): number {
    if (!oleada.plannedTargets) {
      return 0;
    }
    return Math.round((oleada.completedTargets / oleada.plannedTargets) * 100);
  }

  formatoBytes(bytes: number): string {
    if (bytes < 1024) {
      return `${bytes} B`;
    }
    if (bytes < 1024 * 1024) {
      return `${(bytes / 1024).toFixed(1)} KB`;
    }
    return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
  }

  private cargarPaquetes(): void {
    this.api.listarPaquetes().subscribe({
      next: (paquetes) => {
        this.paquetes = paquetes;
        this.despliegueFormulario.packageId ||= paquetes[0]?.id ?? '';
      },
      error: (respuesta) => this.error = respuesta?.error?.message ?? 'No se pudieron cargar las versiones POS.'
    });
  }

  private cargarDespliegues(): void {
    this.api.listarDespliegues().subscribe({
      next: (despliegues) => {
        this.despliegues = despliegues;
        if (!this.despliegueSeleccionado && despliegues.length > 0) {
          this.seleccionarDespliegue(despliegues[0]);
        }
      },
      error: (respuesta) => this.error = respuesta?.error?.message ?? 'No se pudieron cargar las campanas POS.'
    });
  }

  private operarOleada(oleada: OleadaOrquestacion, accion: 'start' | 'pause' | 'resume'): void {
    if (!this.despliegueSeleccionado) {
      return;
    }
    const operacion = accion === 'pause'
      ? this.api.pausarOleada(this.despliegueSeleccionado.id, oleada.id)
      : accion === 'resume'
        ? this.api.reanudarOleada(this.despliegueSeleccionado.id, oleada.id)
        : this.api.iniciarOleada(this.despliegueSeleccionado.id, oleada.id);

    operacion.subscribe({
      next: (plan) => {
        this.planOrquestacion = plan;
        this.mensaje = 'Oleada actualizada.';
      },
      error: (respuesta) => this.error = respuesta?.error?.message ?? 'No se pudo operar la oleada.'
    });
  }

  private idsEquipos(): string[] {
    return this.despliegueFormulario.deviceIds
      .split(/[\n,;]/)
      .map((id) => id.trim())
      .filter(Boolean);
  }

  private normalizarFecha(valor: string): string | null {
    return valor ? new Date(valor).toISOString() : null;
  }
}
