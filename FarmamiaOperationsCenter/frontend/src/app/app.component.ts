import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterOutlet } from '@angular/router';
import { finalize } from 'rxjs';
import {
  AlertaOperativa,
  AuditoriaAdministrativa,
  CampanaPos,
  DetalleEquipoPos,
  EquipoHuerfano,
  EquipoPos,
  EstadoOperacionalFarmacia,
  EstadoSaludApi,
  EventoAgente,
  GrupoTrx,
  RespuestaPagina,
  Farmacia,
  DetalleEquipo,
  Equipo,
  EventoActualizacion,
  UsuarioAdministrativo
} from './modelos/modelos-operaciones';
import { DashboardNocComponent } from './dashboard-noc/dashboard-noc.component';
import { FarmaciasTarjetasComponent } from './farmacias-tarjetas/farmacias-tarjetas.component';
import { AlertasOperacionesComponent } from './alertas-operaciones/alertas-operaciones.component';
import { ActualizacionesOperacionesComponent } from './actualizaciones-operaciones/actualizaciones-operaciones.component';
import { AlertListComponent } from './componentes-ui/alert-list.component';
import { AppCardComponent } from './componentes-ui/app-card.component';
import { KpiCardComponent } from './componentes-ui/kpi-card.component';
import { NocTableComponent } from './componentes-ui/noc-table.component';
import { StatCardComponent } from './componentes-ui/stat-card.component';
import { StatusBadgeComponent } from './componentes-ui/status-badge.component';
import { MapaEcuadorComponent } from './componentes-ui/mapa-ecuador.component';
import { OperacionesApiService } from './servicios/operaciones-api.service';
import { SesionAdminService } from './servicios/sesion-admin.service';

type Vista = 'dashboard' | 'turno' | 'incidentes' | 'operaciones' | 'equipos' | 'actualizaciones' | 'gruposTrx' | 'agentes' | 'red' | 'eventos' | 'alertas' | 'auditoria' | 'seguridad' | 'usuarios';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterOutlet,
    AlertasOperacionesComponent,
    ActualizacionesOperacionesComponent,
    DashboardNocComponent,
    FarmaciasTarjetasComponent,
    AlertListComponent,
    AppCardComponent,
    KpiCardComponent,
    NocTableComponent,
    StatCardComponent,
    StatusBadgeComponent,
    MapaEcuadorComponent
  ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit, OnDestroy {
  vistaActiva: Vista = 'dashboard';
  tema: 'oscuro' | 'claro' = 'oscuro';
  horaActual = '';
  panelDerecho = false;
  private intervalReloj?: ReturnType<typeof setInterval>;
  subTabFarmacias: 'tarjetas' | 'todas' | 'turno' | 'huerfanos' = 'tarjetas';
  subTabAlertas: 'activas' | 'incidentes' | 'red' = 'activas';
  subTabAgentes: 'equipos' | 'eventos' = 'equipos';
  salud?: EstadoSaludApi;
  farmacias: Farmacia[] = [];
  estadoFarmacias: EstadoOperacionalFarmacia[] = [];
  equiposPos: Equipo[] = [];
  equiposRed: Equipo[] = [];
  equiposPosPagina?: RespuestaPagina<EquipoPos>;
  equiposHuerfanos: EquipoHuerfano[] = [];
  campanasPos: CampanaPos[] = [];
  gruposTrx: GrupoTrx[] = [];
  gruposTrxPagina?: RespuestaPagina<GrupoTrx>;
  grupoTrxSeleccionado?: GrupoTrx;
  eventosAgente: EventoAgente[] = [];
  alertasDashboard: AlertaOperativa[] = [];
  auditoria: AuditoriaAdministrativa[] = [];
  auditoriaPagina?: RespuestaPagina<AuditoriaAdministrativa>;
  usuariosAdministrativos: UsuarioAdministrativo[] = [];
  detalleEquipoPos?: DetalleEquipoPos;
  cargando = false;
  guardandoGrupoTrx = false;
  guardandoSeguridad = false;
  guardandoUsuario = false;
  procesandoHuerfanos = false;
  usuarioEditandoId?: string;
  usuarioResetId?: string;
  mensaje = '';
  error = '';
  autenticando = false;

  loginFormulario = {
    usuario: 'admin',
    contrasena: ''
  };

  grupoTrxFormulario = {
    id: '',
    codigo: '',
    nombre: '',
    descripcion: '',
    maximoEquipos: 100,
    activo: true
  };

  grupoTrxFiltros = {
    codigo: '',
    estado: '',
    activo: '' as '' | boolean,
    page: 0,
    size: 20,
    sort: 'codigo,asc'
  };

  grupoTrxAsignacion = {
    equipoId: '',
    motivo: ''
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
    to: '',
    page: 0,
    size: 20,
    sort: 'creadoEn,desc'
  };

  equiposFiltros = {
    q: '',
    status: '',
    branchCode: '',
    posVersion: '',
    agentVersion: '',
    page: 0,
    size: 20,
    sort: 'nombreEquipo,asc'
  };

  estadosEquipo = ['', 'ONLINE', 'OFFLINE', 'STALE', 'ERROR'];
  estadosGrupoTrx = ['', 'ACTIVO', 'PAUSADO', 'RETIRADO'];

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

  huerfanosSeleccionados = new Set<string>();
  asignacionManualHuerfanos: Record<string, string> = {};

  get equipos(): Equipo[] {
    return this.equiposPos;
  }

  set equipos(valor: Equipo[]) {
    this.equiposPos = valor;
  }

  get equiposPagina(): RespuestaPagina<Equipo> | undefined {
    return this.equiposPosPagina;
  }

  set equiposPagina(valor: RespuestaPagina<Equipo> | undefined) {
    this.equiposPosPagina = valor;
  }

  get eventos(): EventoActualizacion[] {
    return this.eventosAgente;
  }

  set eventos(valor: EventoActualizacion[]) {
    this.eventosAgente = valor;
  }

  get detalleEquipo(): DetalleEquipo | undefined {
    return this.detalleEquipoPos;
  }

  set detalleEquipo(valor: DetalleEquipo | undefined) {
    this.detalleEquipoPos = valor;
  }

  constructor(
    private readonly api: OperacionesApiService,
    public readonly sesion: SesionAdminService,
    private readonly router: Router
  ) {
  }

  get equiposOnline(): number {
    return this.equiposPos.filter((equipo) => equipo.status === 'ONLINE').length;
  }

  get equiposFueraLinea(): number {
    return this.equiposPos.filter((equipo) => ['OFFLINE', 'STALE', 'ERROR'].includes(equipo.status)).length;
  }

  get equiposSinLatido(): EquipoPos[] {
    return this.equiposPos
      .filter((equipo) => equipo.status !== 'ONLINE')
      .slice(0, 8);
  }

  get huerfanosConSugerenciaValida(): number {
    return this.equiposHuerfanos.filter((equipo) => equipo.suggestionStatus === 'SUGERENCIA_VALIDA').length;
  }

  get huerfanosPendientesManual(): number {
    return this.equiposHuerfanos.filter((equipo) => equipo.suggestionStatus !== 'SUGERENCIA_VALIDA').length;
  }

  get asignacionesHuerfanosResueltas(): Array<{ deviceId: string; branchId: string }> {
    return this.equiposHuerfanos
      .filter((equipo) => this.huerfanosSeleccionados.has(equipo.deviceId))
      .map((equipo) => ({
        deviceId: equipo.deviceId,
        branchId: this.branchIdResueltoHuerfano(equipo)
      }))
      .filter((item): item is { deviceId: string; branchId: string } => !!item.branchId);
  }

  get puedeProcesarHuerfanos(): boolean {
    return this.asignacionesHuerfanosResueltas.length > 0 && !this.procesandoHuerfanos;
  }

  get alertasAbiertas(): number {
    return this.alertasDashboard.filter((alerta) => alerta.status === 'OPEN').length;
  }

  get totalEquiposDashboard(): number {
    return this.equiposPos.length;
  }

  get alertasCriticasDashboard(): AlertaOperativa[] {
    return this.alertasDashboard
      .filter((alerta) => alerta.severity === 'CRITICAL')
      .slice(0, 5);
  }

  get farmaciasCriticas(): EstadoOperacionalFarmacia[] {
    return this.estadoFarmacias
      .filter((farmacia) => farmacia.critica)
      .sort((a, b) => {
        const turno = Number(b.deTurno) - Number(a.deTurno);
        if (turno !== 0) {
          return turno;
        }
        const criticidad = (b.alertasCriticas + b.equiposOffline + b.objetivosCampanaFallidos) -
          (a.alertasCriticas + a.equiposOffline + a.objetivosCampanaFallidos);
        if (criticidad !== 0) {
          return criticidad;
        }
        return a.codigoFarmacia.localeCompare(b.codigoFarmacia);
      });
  }

  get farmaciasTurnoEnRiesgo(): EstadoOperacionalFarmacia[] {
    return this.estadoFarmacias
      .filter((farmacia) => farmacia.turnoEnRiesgo)
      .sort((a, b) => {
        const prioridad = this.prioridadEstadoOperacional(a.estadoOperacional) - this.prioridadEstadoOperacional(b.estadoOperacional);
        if (prioridad !== 0) {
          return prioridad;
        }
        const criticidad = (b.alertasCriticas + b.equiposOffline) - (a.alertasCriticas + a.equiposOffline);
        if (criticidad !== 0) {
          return criticidad;
        }
        return a.codigoFarmacia.localeCompare(b.codigoFarmacia);
      });
  }

  private prioridadEstadoOperacional(estado: string): number {
    if (estado === 'CRITICA') {
      return 0;
    }
    if (estado === 'EN_RIESGO' || estado === 'TURNO_EN_RIESGO') {
      return 1;
    }
    return 2;
  }

  private prioridadSeveridad(severidad: string): number {
    if (severidad === 'CRITICAL') {
      return 4;
    }
    if (severidad === 'HIGH') {
      return 3;
    }
    if (severidad === 'MEDIUM') {
      return 2;
    }
    if (severidad === 'LOW') {
      return 1;
    }
    return 0;
  }

  get totalFarmaciasDashboard(): number {
    return this.estadoFarmacias.length || this.farmacias.length;
  }

  get totalEquiposPosFarmacias(): number {
    return this.estadoFarmacias.reduce((total, farmacia) => total + farmacia.totalEquiposPos, 0);
  }

  get equiposPosOfflineFarmacias(): number {
    return this.estadoFarmacias.reduce((total, farmacia) => total + farmacia.equiposOffline, 0);
  }

  get campanasOperativas(): CampanaPos[] {
    return this.campanasPos.filter((campana) =>
      ['SCHEDULED', 'APPROVED', 'PILOT_RUNNING', 'RUNNING', 'PAUSED'].includes(campana.status)
    );
  }

  get farmaciasTurnoRiesgoNoc(): EstadoOperacionalFarmacia[] {
    return this.farmaciasTurnoEnRiesgo.slice(0, 10);
  }

  get farmaciasDeTurno(): EstadoOperacionalFarmacia[] {
    return this.estadoFarmacias
      .filter((farmacia) => farmacia.deTurno)
      .sort((a, b) => {
        const prioridad = this.prioridadEstadoOperacional(a.estadoOperacional) - this.prioridadEstadoOperacional(b.estadoOperacional);
        if (prioridad !== 0) {
          return prioridad;
        }
        return a.codigoFarmacia.localeCompare(b.codigoFarmacia);
      });
  }

  get farmaciasCriticasNoc(): EstadoOperacionalFarmacia[] {
    return this.farmaciasCriticas.slice(0, 8);
  }

  get farmaciasOk(): number {
    return this.estadoFarmacias.filter((farmacia) => farmacia.estadoOperacional === 'NORMAL' && !farmacia.critica && !farmacia.turnoEnRiesgo).length;
  }

  get farmaciasEnRiesgo(): number {
    return this.estadoFarmacias.filter((farmacia) =>
      farmacia.estadoOperacional === 'EN_RIESGO' || farmacia.turnoEnRiesgo
    ).length;
  }

  get totalAlertasCriticas(): number {
    return this.alertasDashboard.filter((alerta) =>
      alerta.severity === 'CRITICAL' && alerta.status !== 'CLOSED'
    ).length;
  }

  get porcentajeFarmaciasOk(): string {
    return this.porcentajeTexto(this.farmaciasOk, this.totalFarmaciasDashboard);
  }

  get porcentajeFarmaciasEnRiesgo(): string {
    return this.porcentajeTexto(this.farmaciasEnRiesgo, this.totalFarmaciasDashboard);
  }

  get porcentajeFarmaciasCriticas(): string {
    return this.porcentajeTexto(this.farmaciasCriticas.length, this.totalFarmaciasDashboard);
  }

  get porcentajePosOnline(): string {
    return this.porcentajeTexto(this.equiposOnline, this.totalEquiposDashboard);
  }

  get porcentajePosOffline(): string {
    return this.porcentajeTexto(this.equiposFueraLinea, this.totalEquiposDashboard);
  }

  get porcentajeAlertasCriticas(): string {
    return `${this.totalAlertasCriticas} abiertas`;
  }

  get sparklineEstable(): number[] {
    return [34, 42, 46, 55, 58, 64, 70, 76];
  }

  get sparklineRiesgo(): number[] {
    return [22, 35, 30, 52, 46, 62, 55, 68];
  }

  get sparklineCritico(): number[] {
    return [18, 28, 42, 34, 62, 48, 72, 60];
  }

  private porcentajeTexto(valor: number, total: number): string {
    if (!total) {
      return '0%';
    }
    return `${Math.round((valor / total) * 100)}%`;
  }

  get alertasCriticasNoc(): AlertaOperativa[] {
    return this.alertasDashboard
      .filter((alerta) => alerta.severity === 'CRITICAL' && alerta.status !== 'CLOSED')
      .sort((a, b) => new Date(a.openedAt).getTime() - new Date(b.openedAt).getTime())
      .slice(0, 8);
  }

  get alertasRedCriticas(): number {
    return this.alertasDashboard.filter((alerta) =>
      !!alerta.networkEvent && alerta.status !== 'CLOSED' && ['CRITICAL', 'HIGH'].includes(alerta.severity)
    ).length;
  }

  get alertasLatenciaAlta(): number {
    return this.alertasDashboard.filter((alerta) =>
      alerta.status !== 'CLOSED' && (alerta.alertType.includes('LATENCY') || alerta.alertType.includes('HIGH_LATENCY'))
    ).length;
  }

  abrirGrafana(): void {
    window.open('http://localhost:3000', '_blank', 'noopener,noreferrer');
  }

  get gruposTrxActivos(): number {
    return this.gruposTrx.filter((grupo) => grupo.status === 'ACTIVO').length;
  }

  get gruposTrxPausados(): number {
    return this.gruposTrx.filter((grupo) => grupo.status === 'PAUSADO').length;
  }

  get gruposTrxTieneSiguiente(): boolean {
    return !!this.gruposTrxPagina?.hasNext;
  }

  get auditoriaTieneSiguiente(): boolean {
    return this.auditoriaPagina?.hasNext ?? false;
  }

  get equiposTieneSiguiente(): boolean {
    return this.equiposPosPagina?.hasNext ?? false;
  }

  private actualizarReloj(): void {
    const ahora = new Date();
    this.horaActual = ahora.toLocaleTimeString('es-EC', { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false });
  }

  ngOnInit(): void {
    this.actualizarReloj();
    this.intervalReloj = setInterval(() => this.actualizarReloj(), 1000);

    const temaGuardado = localStorage.getItem('noc-tema');
    if (temaGuardado === 'claro' || temaGuardado === 'oscuro') {
      this.tema = temaGuardado;
    }

    if (this.sesion.autenticado()) {
      this.router.navigate(['/operaciones']);
      this.vistaActiva = 'dashboard';
      this.recargarTodo();
      return;
    }
    this.router.navigate(['/login']);
  }

  ngOnDestroy(): void {
    if (this.intervalReloj) clearInterval(this.intervalReloj);
  }

  togglePanel(): void {
    this.panelDerecho = !this.panelDerecho;
  }

  toggleTema(): void {
    this.tema = this.tema === 'oscuro' ? 'claro' : 'oscuro';
    localStorage.setItem('noc-tema', this.tema);
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
          this.vistaActiva = 'dashboard';
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
    this.equipos = [];
    this.farmacias = [];
    this.estadoFarmacias = [];
    this.campanasPos = [];
    this.gruposTrx = [];
    this.gruposTrxPagina = undefined;
    this.grupoTrxSeleccionado = undefined;
    this.eventos = [];
    this.alertasDashboard = [];
    this.auditoria = [];
    this.usuariosAdministrativos = [];
    this.mensaje = '';
    this.error = '';
    this.router.navigate(['/login']);
  }

  cambiarVista(vista: Vista): void {
    // Sub-vistas redirigidas a sub-tabs del padre
    if (vista === 'turno') {
      this.cambiarVista('equipos');
      this.subTabFarmacias = 'turno';
      return;
    }
    if (vista === 'incidentes') {
      this.cambiarVista('alertas');
      this.subTabAlertas = 'incidentes';
      return;
    }
    if (vista === 'red') {
      this.cambiarVista('alertas');
      this.subTabAlertas = 'red';
      return;
    }
    if (vista === 'eventos') {
      this.cambiarVista('agentes');
      this.subTabAgentes = 'eventos';
      return;
    }
    if (vista === 'operaciones') {
      this.cambiarVista('actualizaciones');
      return;
    }

    if (vista === 'usuarios' && !this.sesion.esAdmin()) {
      this.error = 'No tienes permiso para administrar usuarios.';
      return;
    }
    if (vista === 'alertas' && !this.sesion.puedeVerEventosYAlertas()) {
      this.error = 'No tienes permiso para consultar alertas.';
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

  abrirRed(): void {
    if (!this.sesion.puedeVerEventosYAlertas()) {
      this.error = 'No tienes permiso para consultar alertas de red.';
      return;
    }
    this.subTabAlertas = 'red';
    this.cambiarVista('alertas');
  }

  verDetalleEquipoPorId(idEquipo: string): void {
    this.verDetalleEquipo({ id: idEquipo } as Equipo);
  }

  recargarTodo(): void {
    this.cargando = true;
    this.error = '';
    this.api.obtenerSalud().subscribe({
      next: (salud) => this.salud = salud,
      error: () => this.error = 'No se pudo consultar la salud de la API.'
    });

    this.api.listarSucursales().subscribe({
      next: (sucursales) => this.farmacias = sucursales,
      error: () => this.error = 'No se pudo cargar el listado de farmacias.'
    });

    this.api.listarEstadoFarmacias().subscribe({
      next: (estadoFarmacias) => this.estadoFarmacias = estadoFarmacias,
      error: () => this.error = 'No se pudo cargar el estado operacional de farmacias.'
    });

    this.cargarGruposTrx();

    this.cargarEquipos();
    this.cargarEquiposHuerfanos();

    this.api.listarDespliegues()
      .pipe(finalize(() => this.cargando = false))
      .subscribe({
        next: (campanas) => this.campanasPos = campanas,
        error: () => this.error = 'No se pudo cargar el listado de campanas POS.'
      });

    if (this.sesion.puedeVerEventosYAlertas()) {
      this.api.listarEventos().subscribe({
        next: (eventos) => this.eventosAgente = eventos,
        error: () => this.error = 'No se pudo cargar el listado de eventos.'
      });

      this.cargarAlertasDashboard();
    } else {
      this.eventos = [];
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

  cargarEquipos(): void {
    this.api.listarEquiposPaginados({
      q: this.equiposFiltros.q.trim(),
      status: this.equiposFiltros.status,
      branchCode: this.equiposFiltros.branchCode.trim(),
      posVersion: this.equiposFiltros.posVersion.trim(),
      agentVersion: this.equiposFiltros.agentVersion.trim(),
      page: this.equiposFiltros.page,
      size: this.equiposFiltros.size,
      sort: this.equiposFiltros.sort
    }).subscribe({
      next: (pagina) => {
        this.equiposPagina = pagina;
        const equipos = [...pagina.content];
        this.equiposPos = equipos.filter((equipo) => (equipo.tipo ?? 'POS_TERMINAL') === 'POS_TERMINAL');
        this.equiposRed = equipos.filter((equipo) => equipo.tipo === 'NETWORK_LINK');
      },
      error: () => this.error = 'No se pudo cargar el listado de equipos.'
    });
  }

  cargarEquiposHuerfanos(): void {
    if (!this.sesion.puedeVerEventosYAlertas()) {
      this.equiposHuerfanos = [];
      this.huerfanosSeleccionados.clear();
      this.asignacionManualHuerfanos = {};
      return;
    }

    this.api.listarEquiposHuerfanos().subscribe({
      next: (equipos) => {
        this.equiposHuerfanos = equipos;
        this.huerfanosSeleccionados = new Set(
          equipos
            .filter((equipo) => equipo.suggestionStatus === 'SUGERENCIA_VALIDA' && !!equipo.suggestedBranchId)
            .map((equipo) => equipo.deviceId)
        );
        this.asignacionManualHuerfanos = {};
      },
      error: (respuesta) => this.error = respuesta?.error?.message ?? 'No se pudo cargar agentes sin asignar.'
    });
  }

  branchIdResueltoHuerfano(equipo: EquipoHuerfano): string {
    return equipo.suggestedBranchId || this.asignacionManualHuerfanos[equipo.deviceId] || '';
  }

  huerfanoSeleccionable(equipo: EquipoHuerfano): boolean {
    return !!this.branchIdResueltoHuerfano(equipo);
  }

  huerfanoSeleccionado(equipo: EquipoHuerfano): boolean {
    return this.huerfanosSeleccionados.has(equipo.deviceId);
  }

  alternarHuerfano(equipo: EquipoHuerfano, seleccionado: boolean): void {
    if (!this.huerfanoSeleccionable(equipo)) {
      this.huerfanosSeleccionados.delete(equipo.deviceId);
      return;
    }
    seleccionado
      ? this.huerfanosSeleccionados.add(equipo.deviceId)
      : this.huerfanosSeleccionados.delete(equipo.deviceId);
  }

  seleccionarSucursalManualHuerfano(equipo: EquipoHuerfano, branchId: string): void {
    this.asignacionManualHuerfanos[equipo.deviceId] = branchId;
    if (branchId) {
      this.huerfanosSeleccionados.add(equipo.deviceId);
    } else {
      this.huerfanosSeleccionados.delete(equipo.deviceId);
    }
  }

  procesarAsignacionHuerfanos(): void {
    const asignaciones = this.asignacionesHuerfanosResueltas;
    if (asignaciones.length === 0) {
      return;
    }

    this.procesandoHuerfanos = true;
    this.error = '';
    this.mensaje = '';
    this.api.asignarEquiposHuerfanos(asignaciones)
      .pipe(finalize(() => this.procesandoHuerfanos = false))
      .subscribe({
        next: (resumen) => {
          const procesados = new Set(asignaciones.map((item) => item.deviceId));
          this.equiposHuerfanos = this.equiposHuerfanos.filter((equipo) => !procesados.has(equipo.deviceId));
          procesados.forEach((id) => {
            this.huerfanosSeleccionados.delete(id);
            delete this.asignacionManualHuerfanos[id];
          });
          this.mensaje = `${resumen.assigned} equipos aprovisionados correctamente.`;
          this.cargarEquipos();
          this.api.listarEstadoFarmacias().subscribe({
            next: (estadoFarmacias) => this.estadoFarmacias = estadoFarmacias
          });
        },
        error: (respuesta) => this.error = respuesta?.error?.message ?? 'No se pudo procesar la asignacion masiva.'
      });
  }

  cargarGruposTrx(): void {
    this.api.listarGruposTrx({
      codigo: this.grupoTrxFiltros.codigo.trim(),
      estado: this.grupoTrxFiltros.estado,
      activo: this.grupoTrxFiltros.activo,
      page: this.grupoTrxFiltros.page,
      size: this.grupoTrxFiltros.size,
      sort: this.grupoTrxFiltros.sort
    }).subscribe({
      next: (pagina) => {
        this.gruposTrxPagina = pagina;
        this.gruposTrx = pagina.content;
      },
      error: (respuesta) => this.error = respuesta?.error?.message ?? 'No se pudo cargar Grupos TRX.'
    });
  }

  filtrarGruposTrx(): void {
    this.grupoTrxFiltros.page = 0;
    this.cargarGruposTrx();
  }

  limpiarFiltrosGruposTrx(): void {
    this.grupoTrxFiltros = {
      codigo: '',
      estado: '',
      activo: '',
      page: 0,
      size: 20,
      sort: 'codigo,asc'
    };
    this.cargarGruposTrx();
  }

  paginaGruposTrx(delta: number): void {
    const siguiente = this.grupoTrxFiltros.page + delta;
    if (siguiente < 0) {
      return;
    }
    if (delta > 0 && !this.gruposTrxTieneSiguiente) {
      return;
    }
    this.grupoTrxFiltros.page = siguiente;
    this.cargarGruposTrx();
  }

  seleccionarGrupoTrx(grupo: GrupoTrx): void {
    this.api.obtenerGrupoTrx(grupo.id).subscribe({
      next: (detalle) => {
        this.grupoTrxSeleccionado = detalle;
        this.grupoTrxFormulario = {
          id: detalle.id,
          codigo: detalle.code,
          nombre: detalle.name,
          descripcion: detalle.description ?? '',
          maximoEquipos: detalle.maxDevices,
          activo: detalle.active
        };
        this.grupoTrxAsignacion = { equipoId: '', motivo: '' };
      },
      error: (respuesta) => this.error = respuesta?.error?.message ?? 'No se pudo cargar el Grupo TRX.'
    });
  }

  nuevoGrupoTrx(): void {
    this.grupoTrxSeleccionado = undefined;
    this.grupoTrxFormulario = {
      id: '',
      codigo: '',
      nombre: '',
      descripcion: '',
      maximoEquipos: 100,
      activo: true
    };
    this.grupoTrxAsignacion = { equipoId: '', motivo: '' };
  }

  guardarGrupoTrx(): void {
    if (!this.sesion.puedeOperar()) {
      this.error = 'No tienes permiso para operar Grupos TRX.';
      return;
    }
    if (!this.grupoTrxFormulario.codigo || !this.grupoTrxFormulario.nombre) {
      this.error = 'Completa codigo y nombre del Grupo TRX.';
      return;
    }

    const datos = {
      codigo: this.grupoTrxFormulario.codigo.trim().toUpperCase(),
      nombre: this.grupoTrxFormulario.nombre.trim(),
      descripcion: this.grupoTrxFormulario.descripcion || null,
      maximoEquipos: this.grupoTrxFormulario.maximoEquipos,
      activo: this.grupoTrxFormulario.activo
    };

    const operacion = this.grupoTrxFormulario.id
      ? this.api.actualizarGrupoTrx(this.grupoTrxFormulario.id, datos)
      : this.api.crearGrupoTrx(datos);

    this.guardandoGrupoTrx = true;
    this.error = '';
    operacion
      .pipe(finalize(() => this.guardandoGrupoTrx = false))
      .subscribe({
        next: (grupo) => {
          this.mensaje = this.grupoTrxFormulario.id ? 'Grupo TRX actualizado.' : 'Grupo TRX creado.';
          this.cargarGruposTrx();
          this.seleccionarGrupoTrx(grupo);
        },
        error: (respuesta) => this.error = respuesta?.error?.message ?? 'No se pudo guardar el Grupo TRX.'
      });
  }

  pausarGrupoTrx(grupo: GrupoTrx): void {
    this.operarGrupoTrx(grupo, 'pausar');
  }

  reanudarGrupoTrx(grupo: GrupoTrx): void {
    this.operarGrupoTrx(grupo, 'reanudar');
  }

  retirarGrupoTrx(grupo: GrupoTrx): void {
    this.operarGrupoTrx(grupo, 'retirar');
  }

  asignarEquipoGrupoTrx(): void {
    if (!this.grupoTrxSeleccionado) {
      this.error = 'Selecciona un Grupo TRX.';
      return;
    }
    if (!this.sesion.puedeOperar()) {
      this.error = 'No tienes permiso para asignar equipos POS a TRX.';
      return;
    }
    if (!this.grupoTrxAsignacion.equipoId) {
      this.error = 'Selecciona un Equipo POS.';
      return;
    }
    this.api.asignarEquipoGrupoTrx(
      this.grupoTrxSeleccionado.id,
      this.grupoTrxAsignacion.equipoId,
      this.grupoTrxAsignacion.motivo
    ).subscribe({
      next: (grupo) => {
        this.mensaje = 'Equipo POS asignado al Grupo TRX.';
        this.grupoTrxAsignacion = { equipoId: '', motivo: '' };
        this.cargarGruposTrx();
        this.seleccionarGrupoTrx(grupo);
      },
      error: (respuesta) => this.error = respuesta?.error?.message ?? 'No se pudo asignar el Equipo POS.'
    });
  }

  quitarEquipoGrupoTrx(equipoId: string): void {
    if (!this.grupoTrxSeleccionado || !this.sesion.puedeOperar()) {
      this.error = 'No tienes permiso para quitar equipos POS del Grupo TRX.';
      return;
    }
    this.api.quitarEquipoGrupoTrx(this.grupoTrxSeleccionado.id, equipoId, this.grupoTrxAsignacion.motivo).subscribe({
      next: (grupo) => {
        this.mensaje = 'Equipo POS quitado del Grupo TRX.';
        this.cargarGruposTrx();
        this.seleccionarGrupoTrx(grupo);
      },
      error: (respuesta) => this.error = respuesta?.error?.message ?? 'No se pudo quitar el Equipo POS.'
    });
  }

  private operarGrupoTrx(grupo: GrupoTrx, accion: 'pausar' | 'reanudar' | 'retirar'): void {
    if (!this.sesion.puedeOperar()) {
      this.error = 'No tienes permiso para operar Grupos TRX.';
      return;
    }
    const operacion = accion === 'pausar'
      ? this.api.pausarGrupoTrx(grupo.id)
      : accion === 'reanudar'
        ? this.api.reanudarGrupoTrx(grupo.id)
        : this.api.retirarGrupoTrx(grupo.id);
    operacion.subscribe({
      next: (actualizado) => {
        this.mensaje = 'Grupo TRX actualizado.';
        this.cargarGruposTrx();
        this.seleccionarGrupoTrx(actualizado);
      },
      error: (respuesta) => this.error = respuesta?.error?.message ?? 'No se pudo operar el Grupo TRX.'
    });
  }

  filtrarEquipos(): void {
    this.equiposFiltros.page = 0;
    this.cargarEquipos();
  }

  limpiarFiltrosEquipos(): void {
    this.equiposFiltros = {
      q: '',
      status: '',
      branchCode: '',
      posVersion: '',
      agentVersion: '',
      page: 0,
      size: 20,
      sort: 'nombreEquipo,asc'
    };
    this.cargarEquipos();
  }

  paginaEquipos(delta: number): void {
    const siguiente = this.equiposFiltros.page + delta;
    if (siguiente < 0) {
      return;
    }
    if (delta > 0 && !this.equiposTieneSiguiente) {
      return;
    }
    this.equiposFiltros.page = siguiente;
    this.cargarEquipos();
  }

  cargarAlertasDashboard(): void {
    if (!this.sesion.puedeVerEventosYAlertas()) {
      this.alertasDashboard = [];
      return;
    }
    this.api.listarAlertasPaginadas({ size: 100, sort: 'openedAt,desc' }).subscribe({
      next: (pagina) => this.alertasDashboard = pagina.content,
      error: () => this.alertasDashboard = []
    });
  }

  cargarAuditoria(): void {
    if (!this.sesion.puedeVerAuditoria()) {
      this.error = 'No tienes permiso para consultar auditoria.';
      return;
    }
    this.api.listarAuditoriaPaginada({
      action: this.auditoriaFiltros.action.trim(),
      entityType: this.auditoriaFiltros.entityType.trim(),
      actorUsername: this.auditoriaFiltros.actorUsername.trim(),
      from: this.normalizarFecha(this.auditoriaFiltros.from) ?? undefined,
      to: this.normalizarFecha(this.auditoriaFiltros.to) ?? undefined,
      page: this.auditoriaFiltros.page,
      size: this.auditoriaFiltros.size,
      sort: this.auditoriaFiltros.sort
    }).subscribe({
      next: (pagina) => {
        this.auditoriaPagina = pagina;
        this.auditoria = pagina.content;
      },
      error: (respuesta) => this.error = respuesta?.error?.message ?? 'No se pudo cargar el listado de auditoria.'
    });
  }

  limpiarFiltrosAuditoria(): void {
    this.auditoriaFiltros = {
      action: '',
      entityType: '',
      actorUsername: '',
      from: '',
      to: '',
      page: 0,
      size: 20,
      sort: 'creadoEn,desc'
    };
    this.cargarAuditoria();
  }

  filtrarAuditoria(): void {
    this.auditoriaFiltros.page = 0;
    this.cargarAuditoria();
  }

  paginaAuditoria(delta: number): void {
    const siguiente = this.auditoriaFiltros.page + delta;
    if (siguiente < 0) {
      return;
    }
    if (delta > 0 && !this.auditoriaTieneSiguiente) {
      return;
    }
    this.auditoriaFiltros.page = siguiente;
    this.cargarAuditoria();
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

  private normalizarFecha(valor: string): string | null {
    if (!valor) {
      return null;
    }
    return new Date(valor).toISOString();
  }
}
