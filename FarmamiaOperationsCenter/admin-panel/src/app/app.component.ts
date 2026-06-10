import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterOutlet } from '@angular/router';
import { finalize } from 'rxjs';
import {
  AlertaOperativa,
  AuditoriaAdministrativa,
  Despliegue,
  DetalleEquipo,
  Equipo,
  EstadoDespliegue,
  EstadoSaludApi,
  EventoActualizacion,
  PaquetePos,
  SolicitudCrearDespliegue,
  Sucursal,
  UsuarioAdministrativo
} from './modelos/modelos-operaciones';
import { OperacionesApiService } from './servicios/operaciones-api.service';
import { SesionAdminService } from './servicios/sesion-admin.service';

type Vista = 'dashboard' | 'equipos' | 'paquetes' | 'despliegues' | 'eventos' | 'alertas' | 'auditoria' | 'seguridad' | 'usuarios';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
  vistaActiva: Vista = 'dashboard';
  salud?: EstadoSaludApi;
  sucursales: Sucursal[] = [];
  equipos: Equipo[] = [];
  paquetes: PaquetePos[] = [];
  despliegues: Despliegue[] = [];
  eventos: EventoActualizacion[] = [];
  alertas: AlertaOperativa[] = [];
  alertasDashboard: AlertaOperativa[] = [];
  auditoria: AuditoriaAdministrativa[] = [];
  usuariosAdministrativos: UsuarioAdministrativo[] = [];
  detalleEquipo?: DetalleEquipo;
  estadoSeleccionado?: EstadoDespliegue;
  cargando = false;
  guardandoPaquete = false;
  guardandoDespliegue = false;
  guardandoSeguridad = false;
  guardandoUsuario = false;
  usuarioEditandoId?: string;
  usuarioResetId?: string;
  mensaje = '';
  error = '';
  autenticando = false;

  loginFormulario = {
    usuario: 'admin',
    contrasena: ''
  };

  paqueteFormulario = {
    version: '',
    archivo: undefined as File | undefined
  };

  despliegueFormulario = {
    packageId: '',
    name: '',
    description: '',
    scheduledAt: '',
    targetGroup: 'GENERAL',
    pilot: true,
    deviceIds: ''
  };

  seguridadFormulario = {
    contrasenaActual: '',
    contrasenaNueva: '',
    confirmarContrasena: ''
  };

  auditoriaFiltros = {
    action: '',
    entityType: '',
    actorUsername: '',
    from: '',
    to: ''
  };

  alertasFiltros = {
    status: '',
    severity: '',
    type: '',
    hostname: '',
    branchCode: '',
    dateFrom: '',
    dateTo: '',
    page: 0,
    size: 20,
    sort: 'openedAt,desc'
  };

  estadosAlerta = ['', 'OPEN', 'ACKNOWLEDGED', 'RESOLVED', 'CLOSED'];
  severidadesAlerta = ['', 'CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'WARNING', 'INFO'];
  tiposAlerta = ['', 'UPDATE_FAILED', 'ROLLBACK_FAILED', 'DEVICE_OFFLINE', 'DISK_LOW', 'AGENT_STOPPED'];

  usuarioFormulario = {
    username: '',
    password: '',
    fullName: '',
    email: '',
    role: 'VIEWER'
  };

  usuarioResetFormulario = {
    newPassword: ''
  };

  rolesAdministrativos = ['ADMIN', 'OPERATOR', 'AUDITOR', 'VIEWER'];

  equiposSeleccionados = new Set<string>();

  constructor(
    private readonly api: OperacionesApiService,
    public readonly sesion: SesionAdminService,
    private readonly router: Router
  ) {
  }

  get paquetesAprobados(): number {
    return this.paquetes.filter((paquete) => paquete.status === 'APPROVED').length;
  }

  get desplieguesActivos(): number {
    return this.despliegues.filter((despliegue) =>
      ['SCHEDULED', 'PILOT_RUNNING', 'APPROVED', 'RUNNING'].includes(despliegue.status)
    ).length;
  }

  get equiposOnline(): number {
    return this.equipos.filter((equipo) => equipo.status === 'ONLINE').length;
  }

  get eventosCriticos(): number {
    return this.eventos.filter((evento) =>
      ['FAILED', 'VALIDATION_FAILED', 'ROLLBACK_STARTED', 'ROLLBACK_COMPLETED'].includes(evento.eventType)
    ).length;
  }

  get alertasAbiertas(): number {
    return this.alertasDashboard.filter((alerta) => alerta.status === 'OPEN').length;
  }

  get alertasCriticasDashboard(): AlertaOperativa[] {
    return this.alertasDashboard
      .filter((alerta) => alerta.severity === 'CRITICAL')
      .slice(0, 5);
  }

  ngOnInit(): void {
    if (this.sesion.autenticado()) {
      this.router.navigate(['/operaciones']);
      this.recargarTodo();
      return;
    }
    this.router.navigate(['/login']);
  }

  iniciarSesion(): void {
    const usuario = this.loginFormulario.usuario.trim();
    const contrasena = this.loginFormulario.contrasena.trim();

    if (!usuario || !contrasena) {
      this.error = 'Ingresa usuario y contrasena.';
      return;
    }

    this.autenticando = true;
    this.error = '';
    this.sesion.login(usuario, contrasena)
      .pipe(finalize(() => this.autenticando = false))
      .subscribe({
        next: () => {
          this.mensaje = 'Sesion iniciada.';
          this.router.navigate(['/operaciones']);
          this.recargarTodo();
        },
        error: (respuesta) => {
          const estado = respuesta?.status ? ` HTTP ${respuesta.status}` : '';
          this.error = `Credenciales administrativas invalidas.${estado}`;
        }
      });
  }

  cerrarSesion(): void {
    this.sesion.cerrarSesion();
    this.paquetes = [];
    this.despliegues = [];
    this.equipos = [];
    this.sucursales = [];
    this.eventos = [];
    this.alertas = [];
    this.alertasDashboard = [];
    this.auditoria = [];
    this.usuariosAdministrativos = [];
    this.mensaje = '';
    this.error = '';
    this.router.navigate(['/login']);
  }

  cambiarVista(vista: Vista): void {
    if (vista === 'usuarios' && !this.sesion.esAdmin()) {
      this.error = 'No tienes permiso para administrar usuarios.';
      return;
    }
    if ((vista === 'eventos' || vista === 'alertas') && !this.sesion.puedeVerEventosYAlertas()) {
      this.error = 'No tienes permiso para consultar eventos o alertas.';
      return;
    }
    if (vista === 'auditoria' && !this.sesion.puedeVerAuditoria()) {
      this.error = 'No tienes permiso para consultar auditoria.';
      return;
    }
    this.vistaActiva = vista;
    this.error = '';
    this.mensaje = '';
  }

  verDetalleEquipo(equipo: Equipo): void {
    this.cargando = true;
    this.error = '';
    this.api.obtenerDetalleEquipo(equipo.id)
      .pipe(finalize(() => this.cargando = false))
      .subscribe({
        next: (detalle) => {
          this.detalleEquipo = detalle;
          this.vistaActiva = 'equipos';
        },
        error: () => this.error = 'No se pudo cargar el detalle del equipo.'
      });
  }

  cerrarDetalleEquipo(): void {
    this.detalleEquipo = undefined;
  }

  recargarTodo(): void {
    this.cargando = true;
    this.error = '';
    this.api.obtenerSalud().subscribe({
      next: (salud) => this.salud = salud,
      error: () => this.error = 'No se pudo consultar la salud de la API.'
    });

    this.api.listarPaquetes().subscribe({
      next: (paquetes) => {
        this.paquetes = paquetes;
        if (!this.despliegueFormulario.packageId && paquetes.length > 0) {
          this.despliegueFormulario.packageId = paquetes[0].id;
        }
      },
      error: () => this.error = 'No se pudo cargar el listado de paquetes.'
    });

    this.api.listarSucursales().subscribe({
      next: (sucursales) => this.sucursales = sucursales,
      error: () => this.error = 'No se pudo cargar el listado de sucursales.'
    });

    this.api.listarEquipos().subscribe({
      next: (equipos) => this.equipos = equipos,
      error: () => this.error = 'No se pudo cargar el listado de equipos.'
    });

    this.api.listarDespliegues()
      .pipe(finalize(() => this.cargando = false))
      .subscribe({
        next: (despliegues) => this.despliegues = despliegues,
        error: () => this.error = 'No se pudo cargar el listado de despliegues.'
      });

    if (this.sesion.puedeVerEventosYAlertas()) {
      this.api.listarEventos().subscribe({
        next: (eventos) => this.eventos = eventos,
        error: () => this.error = 'No se pudo cargar el listado de eventos.'
      });

      this.cargarAlertas();
      this.cargarAlertasDashboard();
    } else {
      this.eventos = [];
      this.alertas = [];
      this.alertasDashboard = [];
    }

    if (this.sesion.puedeVerAuditoria()) {
      this.cargarAuditoria();
    } else {
      this.auditoria = [];
    }

    if (this.sesion.esAdmin()) {
      this.cargarUsuariosAdministrativos();
    }
  }

  cargarUsuariosAdministrativos(): void {
    if (!this.sesion.esAdmin()) {
      return;
    }
    this.api.listarUsuariosAdministrativos().subscribe({
      next: (usuarios) => this.usuariosAdministrativos = usuarios,
      error: (respuesta) => this.error = respuesta?.error?.message ?? 'No se pudo cargar el listado de usuarios.'
    });
  }

  cargarAlertas(): void {
    if (!this.sesion.puedeVerEventosYAlertas()) {
      this.alertas = [];
      return;
    }
    this.api.listarAlertas(100, {
      status: this.alertasFiltros.status,
      severity: this.alertasFiltros.severity,
      type: this.alertasFiltros.type,
      hostname: this.alertasFiltros.hostname.trim(),
      branchCode: this.alertasFiltros.branchCode.trim(),
      dateFrom: this.normalizarFecha(this.alertasFiltros.dateFrom) ?? undefined,
      dateTo: this.normalizarFecha(this.alertasFiltros.dateTo) ?? undefined,
      page: this.alertasFiltros.page,
      size: this.alertasFiltros.size,
      sort: this.alertasFiltros.sort
    }).subscribe({
      next: (alertas) => this.alertas = alertas,
      error: (respuesta) => this.error = respuesta?.error?.message ?? 'No se pudo cargar el listado de alertas.'
    });
  }

  cargarAlertasDashboard(): void {
    if (!this.sesion.puedeVerEventosYAlertas()) {
      this.alertasDashboard = [];
      return;
    }
    this.api.listarAlertas(100, { sort: 'openedAt,desc' }).subscribe({
      next: (alertas) => this.alertasDashboard = alertas,
      error: () => this.alertasDashboard = []
    });
  }

  filtrarAlertas(): void {
    this.alertasFiltros.page = 0;
    this.cargarAlertas();
  }

  limpiarFiltrosAlertas(): void {
    this.alertasFiltros = {
      status: '',
      severity: '',
      type: '',
      hostname: '',
      branchCode: '',
      dateFrom: '',
      dateTo: '',
      page: 0,
      size: 20,
      sort: 'openedAt,desc'
    };
    this.cargarAlertas();
  }

  paginaAlertas(delta: number): void {
    const siguiente = this.alertasFiltros.page + delta;
    if (siguiente < 0) {
      return;
    }
    if (delta > 0 && this.alertas.length < this.alertasFiltros.size) {
      return;
    }
    this.alertasFiltros.page = siguiente;
    this.cargarAlertas();
  }

  cargarAuditoria(): void {
    if (!this.sesion.puedeVerAuditoria()) {
      this.error = 'No tienes permiso para consultar auditoria.';
      return;
    }
    this.api.listarAuditoria(100, {
      action: this.auditoriaFiltros.action.trim(),
      entityType: this.auditoriaFiltros.entityType.trim(),
      actorUsername: this.auditoriaFiltros.actorUsername.trim(),
      from: this.normalizarFecha(this.auditoriaFiltros.from) ?? undefined,
      to: this.normalizarFecha(this.auditoriaFiltros.to) ?? undefined
    }).subscribe({
      next: (auditoria) => this.auditoria = auditoria,
      error: (respuesta) => this.error = respuesta?.error?.message ?? 'No se pudo cargar el listado de auditoria.'
    });
  }

  limpiarFiltrosAuditoria(): void {
    this.auditoriaFiltros = {
      action: '',
      entityType: '',
      actorUsername: '',
      from: '',
      to: ''
    };
    this.cargarAuditoria();
  }

  seleccionarArchivo(evento: Event): void {
    const entrada = evento.target as HTMLInputElement;
    this.paqueteFormulario.archivo = entrada.files?.[0];
  }

  cargarPaquete(): void {
    if (!this.sesion.puedeOperar()) {
      this.error = 'No tienes permiso para operar paquetes POS.';
      return;
    }
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
          this.mensaje = 'Paquete cargado y validado.';
          this.paqueteFormulario = { version: '', archivo: undefined };
          this.recargarTodo();
        },
        error: (respuesta) => this.error = respuesta?.error?.message ?? 'No se pudo cargar el paquete.'
      });
  }

  aprobarPaquete(paquete: PaquetePos): void {
    if (!this.sesion.puedeOperar()) {
      this.error = 'No tienes permiso para aprobar paquetes POS.';
      return;
    }
    this.api.aprobarPaquete(paquete.id).subscribe({
      next: () => {
        this.mensaje = 'Paquete aprobado.';
        this.recargarTodo();
      },
      error: () => this.error = 'No se pudo aprobar el paquete.'
    });
  }

  retirarPaquete(paquete: PaquetePos): void {
    if (!this.sesion.puedeOperar()) {
      this.error = 'No tienes permiso para retirar paquetes POS.';
      return;
    }
    this.api.retirarPaquete(paquete.id).subscribe({
      next: () => {
        this.mensaje = 'Paquete retirado.';
        this.recargarTodo();
      },
      error: () => this.error = 'No se pudo retirar el paquete.'
    });
  }

  descargarPaquete(paquete: PaquetePos): void {
    this.error = '';
    this.api.descargarPaquete(paquete.id).subscribe({
      next: (contenido) => {
        const url = URL.createObjectURL(contenido);
        const enlace = document.createElement('a');
        enlace.href = url;
        enlace.download = paquete.fileName || `farmamia-pos-${paquete.version}.zip`;
        document.body.appendChild(enlace);
        enlace.click();
        enlace.remove();
        URL.revokeObjectURL(url);
      },
      error: (respuesta) => this.error = respuesta?.error?.message ?? 'No se pudo descargar el paquete.'
    });
  }

  crearDespliegue(): void {
    if (!this.sesion.puedeOperar()) {
      this.error = 'No tienes permiso para crear despliegues.';
      return;
    }
    const idsManuales = this.despliegueFormulario.deviceIds
      .split(/[\n,;]/)
      .map((id) => id.trim())
      .filter(Boolean);
    const deviceIds = Array.from(new Set([...Array.from(this.equiposSeleccionados), ...idsManuales]));

    if (!this.despliegueFormulario.packageId || !this.despliegueFormulario.name || deviceIds.length === 0) {
      this.error = 'Completa paquete, nombre y al menos un equipo.';
      return;
    }

    const solicitud: SolicitudCrearDespliegue = {
      packageId: this.despliegueFormulario.packageId,
      name: this.despliegueFormulario.name,
      description: this.despliegueFormulario.description || null,
      scheduledAt: this.normalizarFecha(this.despliegueFormulario.scheduledAt),
      targetGroup: this.despliegueFormulario.targetGroup || null,
      pilot: this.despliegueFormulario.pilot,
      deviceIds
    };

    this.guardandoDespliegue = true;
    this.error = '';
    this.api.crearDespliegue(solicitud)
      .pipe(finalize(() => this.guardandoDespliegue = false))
      .subscribe({
        next: () => {
          this.mensaje = 'Despliegue creado.';
          this.despliegueFormulario.name = '';
          this.despliegueFormulario.description = '';
          this.despliegueFormulario.deviceIds = '';
          this.equiposSeleccionados.clear();
          this.recargarTodo();
        },
        error: (respuesta) => this.error = respuesta?.error?.message ?? 'No se pudo crear el despliegue.'
      });
  }

  alternarEquipo(equipo: Equipo, seleccionado: boolean): void {
    if (seleccionado) {
      this.equiposSeleccionados.add(equipo.id);
      return;
    }
    this.equiposSeleccionados.delete(equipo.id);
  }

  estaSeleccionado(equipo: Equipo): boolean {
    return this.equiposSeleccionados.has(equipo.id);
  }

  pausar(despliegue: Despliegue): void {
    if (!this.sesion.puedeOperar()) {
      this.error = 'No tienes permiso para pausar despliegues.';
      return;
    }
    this.api.pausarDespliegue(despliegue.id).subscribe({ next: () => this.recargarTodo() });
  }

  reanudar(despliegue: Despliegue): void {
    if (!this.sesion.puedeOperar()) {
      this.error = 'No tienes permiso para reanudar despliegues.';
      return;
    }
    this.api.reanudarDespliegue(despliegue.id).subscribe({ next: () => this.recargarTodo() });
  }

  cancelar(despliegue: Despliegue): void {
    if (!this.sesion.puedeOperar()) {
      this.error = 'No tienes permiso para cancelar despliegues.';
      return;
    }
    this.api.cancelarDespliegue(despliegue.id).subscribe({ next: () => this.recargarTodo() });
  }

  cambiarContrasena(): void {
    if (!this.seguridadFormulario.contrasenaActual || !this.seguridadFormulario.contrasenaNueva) {
      this.error = 'Completa la contrasena actual y la nueva.';
      return;
    }
    if (this.seguridadFormulario.contrasenaNueva !== this.seguridadFormulario.confirmarContrasena) {
      this.error = 'La confirmacion no coincide con la contrasena nueva.';
      return;
    }

    this.guardandoSeguridad = true;
    this.error = '';
    this.api.cambiarContrasena(
      this.seguridadFormulario.contrasenaActual,
      this.seguridadFormulario.contrasenaNueva
    )
      .pipe(finalize(() => this.guardandoSeguridad = false))
      .subscribe({
        next: () => {
          this.mensaje = 'Contrasena actualizada. Inicia sesion nuevamente.';
          this.seguridadFormulario = {
            contrasenaActual: '',
            contrasenaNueva: '',
            confirmarContrasena: ''
          };
          this.cerrarSesion();
        },
        error: (respuesta) => this.error = respuesta?.error?.message ?? 'No se pudo cambiar la contrasena.'
      });
  }

  guardarUsuarioAdministrativo(): void {
    if (!this.usuarioFormulario.username && !this.usuarioEditandoId) {
      this.error = 'Completa el usuario.';
      return;
    }
    if (!this.usuarioFormulario.fullName || !this.usuarioFormulario.role) {
      this.error = 'Completa nombre y rol.';
      return;
    }
    if (!this.usuarioEditandoId && !this.usuarioFormulario.password) {
      this.error = 'Completa la contrasena inicial.';
      return;
    }

    this.guardandoUsuario = true;
    this.error = '';
    const correo = this.usuarioFormulario.email || null;
    const operacion = this.usuarioEditandoId
      ? this.api.actualizarUsuarioAdministrativo(this.usuarioEditandoId, {
          fullName: this.usuarioFormulario.fullName,
          email: correo
        })
      : this.api.crearUsuarioAdministrativo({
          username: this.usuarioFormulario.username,
          password: this.usuarioFormulario.password,
          fullName: this.usuarioFormulario.fullName,
          email: correo,
          role: this.usuarioFormulario.role
        });

    operacion
      .pipe(finalize(() => this.guardandoUsuario = false))
      .subscribe({
        next: () => {
          this.mensaje = this.usuarioEditandoId ? 'Usuario actualizado.' : 'Usuario creado.';
          this.limpiarFormularioUsuario();
          this.cargarUsuariosAdministrativos();
        },
        error: (respuesta) => this.error = respuesta?.error?.message ?? 'No se pudo guardar el usuario.'
      });
  }

  editarUsuarioAdministrativo(usuario: UsuarioAdministrativo): void {
    this.usuarioEditandoId = usuario.id;
    this.usuarioFormulario = {
      username: usuario.username,
      password: '',
      fullName: usuario.fullName,
      email: usuario.email ?? '',
      role: usuario.role
    };
    this.error = '';
    this.mensaje = '';
  }

  limpiarFormularioUsuario(): void {
    this.usuarioEditandoId = undefined;
    this.usuarioFormulario = {
      username: '',
      password: '',
      fullName: '',
      email: '',
      role: 'VIEWER'
    };
  }

  activarUsuarioAdministrativo(usuario: UsuarioAdministrativo): void {
    this.api.activarUsuarioAdministrativo(usuario.id).subscribe({
      next: () => {
        this.mensaje = 'Usuario activado.';
        this.cargarUsuariosAdministrativos();
      },
      error: (respuesta) => this.error = respuesta?.error?.message ?? 'No se pudo activar el usuario.'
    });
  }

  desactivarUsuarioAdministrativo(usuario: UsuarioAdministrativo): void {
    if (!confirm(`Desactivar usuario ${usuario.username}?`)) {
      return;
    }
    this.api.desactivarUsuarioAdministrativo(usuario.id).subscribe({
      next: () => {
        this.mensaje = 'Usuario desactivado.';
        this.cargarUsuariosAdministrativos();
      },
      error: (respuesta) => this.error = respuesta?.error?.message ?? 'No se pudo desactivar el usuario.'
    });
  }

  cambiarRolUsuarioAdministrativo(usuario: UsuarioAdministrativo, role: string): void {
    this.api.cambiarRolUsuarioAdministrativo(usuario.id, role).subscribe({
      next: () => {
        this.mensaje = 'Rol actualizado.';
        this.cargarUsuariosAdministrativos();
      },
      error: (respuesta) => this.error = respuesta?.error?.message ?? 'No se pudo cambiar el rol.'
    });
  }

  iniciarResetContrasena(usuario: UsuarioAdministrativo): void {
    this.usuarioResetId = usuario.id;
    this.usuarioResetFormulario.newPassword = '';
    this.error = '';
    this.mensaje = '';
  }

  cancelarResetContrasena(): void {
    this.usuarioResetId = undefined;
    this.usuarioResetFormulario.newPassword = '';
  }

  resetearContrasenaUsuarioAdministrativo(): void {
    if (!this.usuarioResetId || !this.usuarioResetFormulario.newPassword) {
      this.error = 'Completa la nueva contrasena.';
      return;
    }
    this.api.resetearContrasenaUsuarioAdministrativo(this.usuarioResetId, this.usuarioResetFormulario.newPassword)
      .subscribe({
        next: () => {
          this.mensaje = 'Contrasena reiniciada.';
          this.cancelarResetContrasena();
          this.cargarUsuariosAdministrativos();
        },
        error: (respuesta) => this.error = respuesta?.error?.message ?? 'No se pudo reiniciar la contrasena.'
      });
  }

  reconocerAlerta(alerta: AlertaOperativa): void {
    if (!this.sesion.puedeOperar()) {
      this.error = 'No tienes permiso para reconocer alertas.';
      return;
    }
    this.api.reconocerAlerta(alerta.id).subscribe({
      next: () => {
        this.mensaje = 'Alerta reconocida.';
        this.cargarAlertas();
        this.cargarAlertasDashboard();
        if (this.sesion.puedeVerAuditoria()) {
          this.cargarAuditoria();
        }
      },
      error: (respuesta) => this.error = respuesta?.error?.message ?? 'No se pudo reconocer la alerta.'
    });
  }

  cerrarAlerta(alerta: AlertaOperativa): void {
    if (!this.sesion.puedeOperar()) {
      this.error = 'No tienes permiso para cerrar alertas.';
      return;
    }
    if (!confirm(`Cerrar alerta ${alerta.title}?`)) {
      return;
    }
    this.api.cerrarAlerta(alerta.id).subscribe({
      next: () => {
        this.mensaje = 'Alerta cerrada.';
        this.cargarAlertas();
        this.cargarAlertasDashboard();
        if (this.sesion.puedeVerAuditoria()) {
          this.cargarAuditoria();
        }
      },
      error: (respuesta) => this.error = respuesta?.error?.message ?? 'No se pudo cerrar la alerta.'
    });
  }

  verEstado(despliegue: Despliegue): void {
    this.api.obtenerEstadoDespliegue(despliegue.id).subscribe({
      next: (estado) => this.estadoSeleccionado = estado,
      error: () => this.error = 'No se pudo consultar el estado del despliegue.'
    });
  }

  descargar(paquete: PaquetePos): string {
    return this.api.urlAbsoluta(paquete.downloadUrl);
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

  porcentajeDisco(metrica?: { diskFreeMb?: number | null; diskTotalMb?: number | null } | null): number {
    if (!metrica?.diskFreeMb || !metrica.diskTotalMb) {
      return 0;
    }
    const usado = metrica.diskTotalMb - metrica.diskFreeMb;
    return Math.round((usado / metrica.diskTotalMb) * 100);
  }

  metadatosComoTexto(evento: EventoActualizacion): string {
    const entradas = Object.entries(evento.metadata ?? {});
    if (entradas.length === 0) {
      return 'Sin metadatos';
    }
    return entradas
      .map(([clave, valor]) => `${clave}: ${String(valor)}`)
      .join(' | ');
  }

  valoresComoTexto(valores: Record<string, unknown> | null | undefined): string {
    const entradas = Object.entries(valores ?? {});
    if (entradas.length === 0) {
      return 'Sin valores';
    }
    return entradas
      .map(([clave, valor]) => `${clave}: ${String(valor)}`)
      .join(' | ');
  }

  objetivoEstados(): Array<[string, number]> {
    return Object.entries(this.estadoSeleccionado?.targetsByStatus ?? {});
  }

  private normalizarFecha(valor: string): string | null {
    if (!valor) {
      return null;
    }
    return new Date(valor).toISOString();
  }
}
