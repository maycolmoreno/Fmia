import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterOutlet } from '@angular/router';
import { finalize } from 'rxjs';
import {
  AlertaOperativa,
  AuditoriaAdministrativa,
  CampanaGrupoTrx,
  CampanaPos,
  DetalleEquipoPos,
  EquipoPos,
  EstadoDespliegue,
  EstadoCampanaFarmacia,
  EstadoOperacionalFarmacia,
  EstadoSaludApi,
  EventoAgente,
  GrupoTrx,
  OleadaOrquestacion,
  PlanOrquestacion,
  RespuestaPagina,
  ResumenCampanaGruposTrx,
  ResumenEstadoCampanaFarmacia,
  SolicitudCrearDespliegue,
  Farmacia,
  Despliegue,
  DetalleEquipo,
  Equipo,
  EventoActualizacion,
  PaquetePos,
  Sucursal,
  VersionPos,
  UsuarioAdministrativo
} from './modelos/modelos-operaciones';
import { DashboardNocComponent } from './dashboard-noc/dashboard-noc.component';
import { AlertListComponent } from './componentes-ui/alert-list.component';
import { AppCardComponent } from './componentes-ui/app-card.component';
import { KpiCardComponent } from './componentes-ui/kpi-card.component';
import { NocTableComponent } from './componentes-ui/noc-table.component';
import { StatCardComponent } from './componentes-ui/stat-card.component';
import { StatusBadgeComponent } from './componentes-ui/status-badge.component';
import { OperacionesApiService } from './servicios/operaciones-api.service';
import { SesionAdminService } from './servicios/sesion-admin.service';

type Vista = 'dashboard' | 'turno' | 'incidentes' | 'operaciones' | 'equipos' | 'paquetes' | 'despliegues' | 'gruposTrx' | 'agentes' | 'red' | 'eventos' | 'alertas' | 'auditoria' | 'seguridad' | 'usuarios';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterOutlet,
    DashboardNocComponent,
    AlertListComponent,
    AppCardComponent,
    KpiCardComponent,
    NocTableComponent,
    StatCardComponent,
    StatusBadgeComponent
  ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
  vistaActiva: Vista = 'dashboard';
  salud?: EstadoSaludApi;
  farmacias: Farmacia[] = [];
  estadoFarmacias: EstadoOperacionalFarmacia[] = [];
  equiposPos: EquipoPos[] = [];
  equiposPosPagina?: RespuestaPagina<EquipoPos>;
  versionesPos: VersionPos[] = [];
  campanasPos: CampanaPos[] = [];
  gruposTrx: GrupoTrx[] = [];
  gruposTrxPagina?: RespuestaPagina<GrupoTrx>;
  grupoTrxSeleccionado?: GrupoTrx;
  eventosAgente: EventoAgente[] = [];
  alertas: AlertaOperativa[] = [];
  alertasDashboard: AlertaOperativa[] = [];
  alertasPagina?: RespuestaPagina<AlertaOperativa>;
  auditoria: AuditoriaAdministrativa[] = [];
  auditoriaPagina?: RespuestaPagina<AuditoriaAdministrativa>;
  usuariosAdministrativos: UsuarioAdministrativo[] = [];
  detalleEquipoPos?: DetalleEquipoPos;
  estadoSeleccionado?: EstadoDespliegue;
  estadoCampanaFarmacia?: ResumenEstadoCampanaFarmacia;
  campanaEstadoFarmacia?: CampanaPos;
  farmaciaCampanaSeleccionada?: EstadoCampanaFarmacia;
  estadoCampanaGruposTrx?: ResumenCampanaGruposTrx;
  campanaEstadoTrx?: CampanaPos;
  grupoTrxCampanaSeleccionado?: CampanaGrupoTrx;
  campanaOrquestacion?: CampanaPos;
  planOrquestacion?: PlanOrquestacion;
  cargando = false;
  guardandoPaquete = false;
  guardandoDespliegue = false;
  guardandoGrupoTrx = false;
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

  estadoCampanaFarmaciaFiltros = {
    estadoTecnico: '',
    estadoOperacional: '',
    grupoTrx: '',
    deTurno: '' as '' | boolean,
    q: '',
    page: 0,
    size: 20,
    sort: 'prioridad,asc'
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

  grupoTrxCampanaFormulario = {
    grupoTrxId: '',
    motivo: ''
  };

  orquestacionFormulario = {
    maxFailurePercent: 10,
    autoPauseEnabled: true,
    retryLimit: 2,
    maxParallelDevices: 25,
    maintenanceWindowStart: '',
    maintenanceWindowEnd: ''
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

  alertasFiltros = {
    status: '',
    severity: '',
    type: '',
    hostname: '',
    branchCode: '',
    dateFrom: '',
    dateTo: '',
    networkEvent: undefined as boolean | undefined,
    page: 0,
    size: 20,
    sort: 'openedAt,desc'
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
  estadosTecnicosCampanaFarmacia = ['', 'PENDIENTE', 'EN_PROGRESO', 'COMPLETADA', 'COMPLETADA_CON_FALLOS', 'FALLIDA'];
  estadosOperacionalesCampanaFarmacia = ['', 'NORMAL', 'EN_RIESGO', 'CRITICA'];

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
  campanaAutorizadaTurno = false;

  get sucursales(): Sucursal[] {
    return this.farmacias;
  }

  set sucursales(valor: Sucursal[]) {
    this.farmacias = valor;
  }

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

  get paquetes(): PaquetePos[] {
    return this.versionesPos;
  }

  set paquetes(valor: PaquetePos[]) {
    this.versionesPos = valor;
  }

  get despliegues(): Despliegue[] {
    return this.campanasPos;
  }

  set despliegues(valor: Despliegue[]) {
    this.campanasPos = valor;
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

  get despliegueOrquestacion(): Despliegue | undefined {
    return this.campanaOrquestacion;
  }

  set despliegueOrquestacion(valor: Despliegue | undefined) {
    this.campanaOrquestacion = valor;
  }

  get desplieguesOperativos(): Despliegue[] {
    return this.campanasOperativas;
  }

  constructor(
    private readonly api: OperacionesApiService,
    public readonly sesion: SesionAdminService,
    private readonly router: Router
  ) {
  }

  get paquetesAprobados(): number {
    return this.versionesPos.filter((version) => version.status === 'APPROVED').length;
  }

  get desplieguesActivos(): number {
    return this.campanasPos.filter((campana) =>
      ['SCHEDULED', 'PILOT_RUNNING', 'APPROVED', 'RUNNING'].includes(campana.status)
    ).length;
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

  get eventosCriticos(): number {
    return this.eventosAgente.filter((evento) =>
      ['FAILED', 'VALIDATION_FAILED', 'ROLLBACK_STARTED', 'ROLLBACK_COMPLETED'].includes(evento.eventType)
    ).length;
  }

  get alertasAbiertas(): number {
    return this.alertasDashboard.filter((alerta) => alerta.status === 'OPEN').length;
  }

  get totalEquiposDashboard(): number {
    return this.equiposPos.length;
  }

  get totalPaquetesDashboard(): number {
    return this.versionesPos.length;
  }

  get totalEventosDashboard(): number {
    return this.eventosAgente.length;
  }

  get totalAlertasDashboard(): number {
    return this.alertasDashboard.length;
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

  get farmaciasTurnoSeleccionadasCampana(): Farmacia[] {
    const idsEquipos = this.idsEquiposFormularioCampana();
    if (idsEquipos.size === 0) {
      return [];
    }
    const farmaciasPorId = new Map(this.farmacias.map((farmacia) => [farmacia.id, farmacia]));
    const idsFarmacia = new Set(
      this.equipos
        .filter((equipo) => idsEquipos.has(equipo.id))
        .map((equipo) => equipo.branchId)
    );
    return Array.from(idsFarmacia)
      .map((idFarmacia) => farmaciasPorId.get(idFarmacia))
      .filter((farmacia): farmacia is Farmacia => !!farmacia && farmacia.onDuty)
      .sort((a, b) => a.code.localeCompare(b.code));
  }

  get advertenciaCampanaTurno(): string {
    const cantidad = this.farmaciasTurnoSeleccionadasCampana.length;
    if (cantidad === 0) {
      return '';
    }
    return `La campana incluye ${cantidad} farmacias de turno. Revise impacto antes de continuar.`;
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

  private idsEquiposFormularioCampana(): Set<string> {
    const idsManuales = this.despliegueFormulario.deviceIds
      .split(/[\n,;]/)
      .map((id) => id.trim())
      .filter(Boolean);
    return new Set([...Array.from(this.equiposSeleccionados), ...idsManuales]);
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

  get campanasActivasNoc(): CampanaPos[] {
    return this.campanasOperativas.slice(0, 8);
  }

  get gruposTrxRiesgoNoc(): Array<{
    codigo: string;
    nombre: string;
    farmaciasAfectadas: number;
    farmaciasCriticas: number;
    farmaciasTurno: number;
    fallos: number;
    rollbacks: number;
    recomendacion: string;
  }> {
    const gruposDesdeCampana = this.estadoCampanaGruposTrx?.grupos ?? [];
    if (gruposDesdeCampana.length > 0) {
      return gruposDesdeCampana
        .map((grupo) => ({
          codigo: grupo.codigoGrupoTrx,
          nombre: grupo.nombreGrupoTrx,
          farmaciasAfectadas: grupo.farmaciasAfectadas,
          farmaciasCriticas: grupo.farmaciasCriticas,
          farmaciasTurno: grupo.farmaciasTurnoAfectadas,
          fallos: grupo.equiposPosFallidos,
          rollbacks: grupo.rollbacks,
          recomendacion: this.recomendacionGrupoTrxCampana(grupo)
        }))
        .filter((grupo) => grupo.recomendacion !== 'CONTINUAR')
        .slice(0, 8);
    }

    return this.gruposTrx
      .filter((grupo) => grupo.status === 'PAUSADO')
      .map((grupo) => ({
        codigo: grupo.code,
        nombre: grupo.name,
        farmaciasAfectadas: grupo.involvedBranches,
        farmaciasCriticas: 0,
        farmaciasTurno: 0,
        fallos: 0,
        rollbacks: 0,
        recomendacion: 'VIGILAR'
      }))
      .slice(0, 8);
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

  get estadoCampanaFarmaciaTieneSiguiente(): boolean {
    return !!this.estadoCampanaFarmacia?.hasNext;
  }

  get oleadasActivas(): number {
    return this.planOrquestacion?.waves.filter((oleada) => ['RUNNING', 'PAUSED'].includes(oleada.status)).length ?? 0;
  }

  get progresoOrquestacion(): number {
    if (!this.planOrquestacion || this.planOrquestacion.waves.length === 0) {
      return 0;
    }
    const total = this.planOrquestacion.waves.reduce((acumulado, oleada) => acumulado + oleada.plannedTargets, 0);
    if (total === 0) {
      return 0;
    }
    const completados = this.planOrquestacion.waves.reduce((acumulado, oleada) => acumulado + oleada.completedTargets, 0);
    return Math.round((completados / total) * 100);
  }

  get alertasTieneSiguiente(): boolean {
    return this.alertasPagina?.hasNext ?? false;
  }

  get auditoriaTieneSiguiente(): boolean {
    return this.auditoriaPagina?.hasNext ?? false;
  }

  get equiposTieneSiguiente(): boolean {
    return this.equiposPosPagina?.hasNext ?? false;
  }

  ngOnInit(): void {
    if (this.sesion.autenticado()) {
      this.router.navigate(['/operaciones']);
      this.vistaActiva = 'dashboard';
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
    this.paquetes = [];
    this.despliegues = [];
    this.equipos = [];
    this.sucursales = [];
    this.estadoFarmacias = [];
    this.gruposTrx = [];
    this.gruposTrxPagina = undefined;
    this.grupoTrxSeleccionado = undefined;
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
    if (vista === 'operaciones' && !this.despliegueOrquestacion && this.desplieguesOperativos.length > 0) {
      this.seleccionarOrquestacion(this.desplieguesOperativos[0]);
    }
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

    this.api.listarPaquetes().subscribe({
      next: (paquetes) => {
        this.paquetes = paquetes;
        if (!this.despliegueFormulario.packageId && paquetes.length > 0) {
          this.despliegueFormulario.packageId = paquetes[0].id;
        }
      },
      error: () => this.error = 'No se pudo cargar el listado de versiones POS.'
    });

    this.api.listarSucursales().subscribe({
      next: (sucursales) => this.sucursales = sucursales,
      error: () => this.error = 'No se pudo cargar el listado de farmacias.'
    });

    this.api.listarEstadoFarmacias().subscribe({
      next: (estadoFarmacias) => this.estadoFarmacias = estadoFarmacias,
      error: () => this.error = 'No se pudo cargar el estado operacional de farmacias.'
    });

    this.cargarGruposTrx();

    this.cargarEquipos();

    this.api.listarDespliegues()
      .pipe(finalize(() => this.cargando = false))
      .subscribe({
        next: (despliegues) => this.despliegues = despliegues,
        error: () => this.error = 'No se pudo cargar el listado de campanas POS.'
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
        this.equipos = pagina.content;
      },
      error: () => this.error = 'No se pudo cargar el listado de equipos.'
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
      codigo: this.grupoTrxFormulario.codigo.trim().toLowerCase(),
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

  cargarAlertas(): void {
    if (!this.sesion.puedeVerEventosYAlertas()) {
      this.alertas = [];
      return;
    }
    this.api.listarAlertasPaginadas({
      status: this.alertasFiltros.status,
      severity: this.alertasFiltros.severity,
      type: this.alertasFiltros.type,
      hostname: this.alertasFiltros.hostname.trim(),
      branchCode: this.alertasFiltros.branchCode.trim(),
      dateFrom: this.normalizarFecha(this.alertasFiltros.dateFrom) ?? undefined,
      dateTo: this.normalizarFecha(this.alertasFiltros.dateTo) ?? undefined,
      networkEvent: this.alertasFiltros.networkEvent,
      page: this.alertasFiltros.page,
      size: this.alertasFiltros.size,
      sort: this.alertasFiltros.sort
    }).subscribe({
      next: (pagina) => {
        this.alertasPagina = pagina;
        this.alertas = pagina.content;
      },
      error: (respuesta) => this.error = respuesta?.error?.message ?? 'No se pudo cargar el listado de alertas.'
    });
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
      networkEvent: undefined,
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
    if (delta > 0 && !this.alertasTieneSiguiente) {
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

  seleccionarArchivo(evento: Event): void {
    const entrada = evento.target as HTMLInputElement;
    this.paqueteFormulario.archivo = entrada.files?.[0];
  }

  cargarPaquete(): void {
    if (!this.sesion.puedeOperar()) {
      this.error = 'No tienes permiso para operar versiones POS.';
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
          this.mensaje = 'Version POS cargada y validada.';
          this.paqueteFormulario = { version: '', archivo: undefined };
          this.recargarTodo();
        },
        error: (respuesta) => this.error = respuesta?.error?.message ?? 'No se pudo cargar la version POS.'
      });
  }

  aprobarPaquete(paquete: PaquetePos): void {
    if (!this.sesion.puedeOperar()) {
      this.error = 'No tienes permiso para aprobar versiones POS.';
      return;
    }
    this.api.aprobarPaquete(paquete.id).subscribe({
      next: () => {
        this.mensaje = 'Version POS aprobada.';
        this.recargarTodo();
      },
      error: () => this.error = 'No se pudo aprobar la version POS.'
    });
  }

  retirarPaquete(paquete: PaquetePos): void {
    if (!this.sesion.puedeOperar()) {
      this.error = 'No tienes permiso para retirar versiones POS.';
      return;
    }
    this.api.retirarPaquete(paquete.id).subscribe({
      next: () => {
        this.mensaje = 'Version POS retirada.';
        this.recargarTodo();
      },
      error: () => this.error = 'No se pudo retirar la version POS.'
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
      error: (respuesta) => this.error = respuesta?.error?.message ?? 'No se pudo descargar la version POS.'
    });
  }

  crearDespliegue(): void {
    if (!this.sesion.puedeOperar()) {
      this.error = 'No tienes permiso para crear campanas POS.';
      return;
    }
    const deviceIds = Array.from(this.idsEquiposFormularioCampana());

    if (!this.despliegueFormulario.packageId || !this.despliegueFormulario.name || deviceIds.length === 0) {
      this.error = 'Completa version POS, nombre y al menos un equipo POS.';
      return;
    }
    if (this.farmaciasTurnoSeleccionadasCampana.length > 0 && !this.campanaAutorizadaTurno) {
      this.error = 'Debes autorizar explicitamente la campana sobre farmacias de turno.';
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
          this.mensaje = 'Campana POS creada.';
          this.despliegueFormulario.name = '';
          this.despliegueFormulario.description = '';
          this.despliegueFormulario.deviceIds = '';
          this.equiposSeleccionados.clear();
          this.campanaAutorizadaTurno = false;
          this.recargarTodo();
        },
        error: (respuesta) => this.error = respuesta?.error?.message ?? 'No se pudo crear la campana POS.'
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

  seleccionarOrquestacion(despliegue: Despliegue): void {
    this.despliegueOrquestacion = despliegue;
    this.planOrquestacion = undefined;
    this.error = '';
    this.api.obtenerPlanOrquestacion(despliegue.id).subscribe({
      next: (plan) => this.planOrquestacion = plan,
      error: (respuesta) => {
        if (respuesta?.status === 404) {
          this.mensaje = 'La campana aun no tiene plan de orquestacion.';
          return;
        }
        this.error = respuesta?.error?.message ?? 'No se pudo cargar el plan de orquestacion.';
      }
    });
  }

  planificarOrquestacion(): void {
    if (!this.despliegueOrquestacion) {
      this.error = 'Selecciona una campana para planificar.';
      return;
    }
    if (!this.sesion.puedeOperar()) {
      this.error = 'No tienes permiso para planificar orquestacion.';
      return;
    }
    this.api.planificarOrquestacion(this.despliegueOrquestacion.id, {
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
    if (!this.despliegueOrquestacion) {
      return;
    }
    this.api.evaluarOrquestacion(this.despliegueOrquestacion.id).subscribe({
      next: (plan) => this.planOrquestacion = plan,
      error: (respuesta) => this.error = respuesta?.error?.message ?? 'No se pudo evaluar la orquestacion.'
    });
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

  private operarOleada(oleada: OleadaOrquestacion, accion: 'start' | 'pause' | 'resume'): void {
    if (!this.despliegueOrquestacion || !this.sesion.puedeOperar()) {
      this.error = 'No tienes permiso para operar oleadas.';
      return;
    }
    const operacion = accion === 'pause'
      ? this.api.pausarOleada(this.despliegueOrquestacion.id, oleada.id)
      : accion === 'resume'
        ? this.api.reanudarOleada(this.despliegueOrquestacion.id, oleada.id)
        : this.api.iniciarOleada(this.despliegueOrquestacion.id, oleada.id);

    operacion.subscribe({
      next: (plan) => {
        this.planOrquestacion = plan;
        this.mensaje = 'Oleada actualizada.';
      },
      error: (respuesta) => this.error = respuesta?.error?.message ?? 'No se pudo operar la oleada.'
    });
  }

  pausar(despliegue: Despliegue): void {
    if (!this.sesion.puedeOperar()) {
      this.error = 'No tienes permiso para pausar campanas POS.';
      return;
    }
    this.api.pausarDespliegue(despliegue.id).subscribe({ next: () => this.recargarTodo() });
  }

  reanudar(despliegue: Despliegue): void {
    if (!this.sesion.puedeOperar()) {
      this.error = 'No tienes permiso para reanudar campanas POS.';
      return;
    }
    this.api.reanudarDespliegue(despliegue.id).subscribe({ next: () => this.recargarTodo() });
  }

  cancelar(despliegue: Despliegue): void {
    if (!this.sesion.puedeOperar()) {
      this.error = 'No tienes permiso para cancelar campanas POS.';
      return;
    }
    this.api.cancelarDespliegue(despliegue.id).subscribe({ next: () => this.recargarTodo() });
  }

  aprobar(despliegue: Despliegue): void {
    if (!this.sesion.puedeOperar()) {
      this.error = 'No tienes permiso para aprobar campanas POS.';
      return;
    }
    this.api.aprobarDespliegue(despliegue.id).subscribe({
      next: () => { this.mensaje = 'Campana aprobada.'; this.recargarTodo(); },
      error: (r) => this.error = r?.error?.message ?? 'No se pudo aprobar la campana.'
    });
  }

  lanzar(despliegue: Despliegue): void {
    if (!this.sesion.puedeOperar()) {
      this.error = 'No tienes permiso para lanzar campanas POS.';
      return;
    }
    this.api.lanzarDespliegue(despliegue.id).subscribe({
      next: () => { this.mensaje = 'Campana lanzada.'; this.recargarTodo(); },
      error: (r) => this.error = r?.error?.message ?? 'No se pudo lanzar la campana.'
    });
  }

  expandir(despliegue: Despliegue): void {
    if (!this.sesion.puedeOperar()) {
      this.error = 'No tienes permiso para expandir campanas POS.';
      return;
    }
    this.api.expandirDespliegue(despliegue.id).subscribe({
      next: () => { this.mensaje = 'Campana expandida al total de equipos.'; this.recargarTodo(); },
      error: (r) => this.error = r?.error?.message ?? 'No se pudo expandir la campana.'
    });
  }

  puedeAprobar(despliegue: Despliegue): boolean {
    return this.sesion.puedeOperar() && ['DRAFT', 'SCHEDULED'].includes(despliegue.status);
  }

  puedeLanzar(despliegue: Despliegue): boolean {
    return this.sesion.puedeOperar() && despliegue.status === 'APPROVED';
  }

  puedeExpandir(despliegue: Despliegue): boolean {
    return this.sesion.puedeOperar() && despliegue.status === 'PILOT_RUNNING';
  }

  puedePausar(despliegue: Despliegue): boolean {
    return this.sesion.puedeOperar()
      && ['SCHEDULED', 'APPROVED', 'PILOT_RUNNING', 'RUNNING'].includes(despliegue.status);
  }

  puedeReanudar(despliegue: Despliegue): boolean {
    return this.sesion.puedeOperar() && despliegue.status === 'PAUSED';
  }

  puedeCancelar(despliegue: Despliegue): boolean {
    return this.sesion.puedeOperar()
      && !['COMPLETED', 'FAILED', 'CANCELLED'].includes(despliegue.status);
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
      error: () => this.error = 'No se pudo consultar el estado de la campana POS.'
    });
  }

  verEstadoPorFarmacia(despliegue: Despliegue): void {
    this.campanaEstadoFarmacia = despliegue;
    this.farmaciaCampanaSeleccionada = undefined;
    this.estadoCampanaFarmaciaFiltros.page = 0;
    this.cargarEstadoCampanaPorFarmacia();
  }

  verEquiposSinActualizar(despliegue: Despliegue): void {
    this.campanaEstadoFarmacia = despliegue;
    this.farmaciaCampanaSeleccionada = undefined;
    this.estadoCampanaFarmaciaFiltros.estadoTecnico = 'FALLIDA';
    this.estadoCampanaFarmaciaFiltros.page = 0;
    this.cargarEstadoCampanaPorFarmacia();
  }

  verEstadoPorTrx(despliegue: Despliegue): void {
    this.campanaEstadoTrx = despliegue;
    this.grupoTrxCampanaSeleccionado = undefined;
    this.cargarEstadoCampanaPorTrx();
  }

  cargarEstadoCampanaPorTrx(): void {
    if (!this.campanaEstadoTrx) {
      this.error = 'Selecciona una campana POS.';
      return;
    }
    this.api.obtenerEstadoCampanaPorTrx(this.campanaEstadoTrx.id).subscribe({
      next: (estado) => {
        this.estadoCampanaGruposTrx = estado;
        this.grupoTrxCampanaSeleccionado = estado.grupos[0];
      },
      error: (respuesta) => this.error = respuesta?.error?.message ?? 'No se pudo consultar el estado por Grupo TRX.'
    });
  }

  asociarGrupoTrxCampana(): void {
    if (!this.campanaEstadoTrx) {
      this.error = 'Selecciona una campana POS.';
      return;
    }
    if (!this.grupoTrxCampanaFormulario.grupoTrxId) {
      this.error = 'Selecciona un Grupo TRX activo.';
      return;
    }
    this.api.asociarGrupoTrxCampana(this.campanaEstadoTrx.id, this.grupoTrxCampanaFormulario.grupoTrxId).subscribe({
      next: () => {
        this.mensaje = 'Grupo TRX asociado a la campana POS.';
        this.grupoTrxCampanaFormulario.grupoTrxId = '';
        this.cargarEstadoCampanaPorTrx();
      },
      error: (respuesta) => this.error = respuesta?.error?.message ?? 'No se pudo asociar el Grupo TRX a la campana.'
    });
  }

  seleccionarGrupoTrxCampana(grupo: CampanaGrupoTrx): void {
    this.grupoTrxCampanaSeleccionado = grupo;
  }

  pausarGrupoTrxCampana(grupo: CampanaGrupoTrx): void {
    if (!this.campanaEstadoTrx || !this.sesion.puedeOperar()) {
      this.error = 'No tienes permiso para pausar Grupo TRX dentro de campana.';
      return;
    }
    this.api.pausarGrupoTrxCampana(this.campanaEstadoTrx.id, grupo.grupoTrxId, this.grupoTrxCampanaFormulario.motivo).subscribe({
      next: () => {
        this.mensaje = 'Grupo TRX pausado dentro de la campana.';
        this.cargarEstadoCampanaPorTrx();
      },
      error: (respuesta) => this.error = respuesta?.error?.message ?? 'No se pudo pausar el Grupo TRX de la campana.'
    });
  }

  reanudarGrupoTrxCampana(grupo: CampanaGrupoTrx): void {
    if (!this.campanaEstadoTrx || !this.sesion.puedeOperar()) {
      this.error = 'No tienes permiso para reanudar Grupo TRX dentro de campana.';
      return;
    }
    this.api.reanudarGrupoTrxCampana(this.campanaEstadoTrx.id, grupo.grupoTrxId, this.grupoTrxCampanaFormulario.motivo).subscribe({
      next: () => {
        this.mensaje = 'Grupo TRX reanudado dentro de la campana.';
        this.cargarEstadoCampanaPorTrx();
      },
      error: (respuesta) => this.error = respuesta?.error?.message ?? 'No se pudo reanudar el Grupo TRX de la campana.'
    });
  }

  quitarGrupoTrxCampana(grupo: CampanaGrupoTrx): void {
    if (!this.campanaEstadoTrx || !this.sesion.puedeOperar()) {
      this.error = 'No tienes permiso para quitar Grupo TRX de la campana.';
      return;
    }
    this.api.quitarGrupoTrxCampana(this.campanaEstadoTrx.id, grupo.grupoTrxId, this.grupoTrxCampanaFormulario.motivo).subscribe({
      next: () => {
        this.mensaje = 'Grupo TRX quitado de la campana POS.';
        this.cargarEstadoCampanaPorTrx();
      },
      error: (respuesta) => this.error = respuesta?.error?.message ?? 'No se pudo quitar el Grupo TRX de la campana.'
    });
  }

  recomendacionGrupoTrxCampana(grupo: CampanaGrupoTrx): string {
    if (grupo.estado === 'PAUSADO') {
      return 'VIGILAR';
    }
    if (grupo.farmaciasCriticas > 0 || grupo.farmaciasTurnoAfectadas > 0 || grupo.equiposPosFallidos > 0) {
      return 'PAUSAR';
    }
    if (grupo.farmaciasPendientes > 0 || grupo.rollbacks > 0) {
      return 'MONITOREAR';
    }
    return 'CONTINUAR';
  }

  farmaciasAfectadasCampana(campana: CampanaPos): number {
    const porNombre = this.estadoFarmacias.filter((farmacia) => farmacia.campanaActivaPrincipal === campana.name).length;
    return porNombre || Math.max(0, Math.ceil((campana.targetCount || 0) / 3));
  }

  farmaciasCriticasCampana(campana: CampanaPos): number {
    return this.estadoFarmacias.filter((farmacia) =>
      farmacia.campanaActivaPrincipal === campana.name && farmacia.critica
    ).length;
  }

  farmaciasTurnoCampana(campana: CampanaPos): number {
    return this.estadoFarmacias.filter((farmacia) =>
      farmacia.campanaActivaPrincipal === campana.name && farmacia.deTurno
    ).length;
  }

  grupoTrxCampana(campana: CampanaPos): string {
    const grupos = this.estadoFarmacias
      .filter((farmacia) => farmacia.campanaActivaPrincipal === campana.name && !!farmacia.grupoTrxPrincipal)
      .map((farmacia) => farmacia.grupoTrxPrincipal as string);
    return Array.from(new Set(grupos)).slice(0, 2).join(', ') || 'GENERAL';
  }

  cargarEstadoCampanaPorFarmacia(): void {
    if (!this.campanaEstadoFarmacia) {
      this.error = 'Selecciona una campana POS.';
      return;
    }
    this.api.obtenerEstadoCampanaPorFarmacia(this.campanaEstadoFarmacia.id, {
      estadoTecnico: this.estadoCampanaFarmaciaFiltros.estadoTecnico,
      estadoOperacional: this.estadoCampanaFarmaciaFiltros.estadoOperacional,
      grupoTrx: this.estadoCampanaFarmaciaFiltros.grupoTrx.trim(),
      deTurno: this.estadoCampanaFarmaciaFiltros.deTurno,
      q: this.estadoCampanaFarmaciaFiltros.q.trim(),
      page: this.estadoCampanaFarmaciaFiltros.page,
      size: this.estadoCampanaFarmaciaFiltros.size,
      sort: this.estadoCampanaFarmaciaFiltros.sort
    }).subscribe({
      next: (estado) => {
        this.estadoCampanaFarmacia = estado;
        this.farmaciaCampanaSeleccionada = estado.farmacias[0];
      },
      error: (respuesta) => this.error = respuesta?.error?.message ?? 'No se pudo consultar el estado por farmacia.'
    });
  }

  filtrarEstadoCampanaPorFarmacia(): void {
    this.estadoCampanaFarmaciaFiltros.page = 0;
    this.cargarEstadoCampanaPorFarmacia();
  }

  limpiarFiltrosEstadoCampanaPorFarmacia(): void {
    this.estadoCampanaFarmaciaFiltros = {
      estadoTecnico: '',
      estadoOperacional: '',
      grupoTrx: '',
      deTurno: '',
      q: '',
      page: 0,
      size: 20,
      sort: 'prioridad,asc'
    };
    this.cargarEstadoCampanaPorFarmacia();
  }

  paginaEstadoCampanaPorFarmacia(delta: number): void {
    const siguiente = this.estadoCampanaFarmaciaFiltros.page + delta;
    if (siguiente < 0) {
      return;
    }
    if (delta > 0 && !this.estadoCampanaFarmaciaTieneSiguiente) {
      return;
    }
    this.estadoCampanaFarmaciaFiltros.page = siguiente;
    this.cargarEstadoCampanaPorFarmacia();
  }

  seleccionarFarmaciaCampana(farmacia: EstadoCampanaFarmacia): void {
    this.farmaciaCampanaSeleccionada = farmacia;
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
