import { HttpClient } from '@angular/common/http';
import { HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import {
  AlertaOperativa,
  AuditoriaAdministrativa,
  CampanaPos,
  CampanaGrupoTrx,
  DetalleEquipoPos,
  EquipoHuerfano,
  EquipoPos,
  EstadoOperacionalFarmacia,
  GrupoTrx,
  EstadoDespliegue,
  EstadoSaludApi,
  EventoAgente,
  VersionPos,
  PlanOrquestacion,
  ResumenCampanaGruposTrx,
  ResumenAsignacionMasiva,
  ResumenEstadoCampanaFarmacia,
  RespuestaPagina,
  ResumenNocDashboard,
  SolicitudCrearDespliegue,
  SolicitudPlanOrquestacion,
  Farmacia,
  Despliegue,
  DetalleEquipo,
  Equipo,
  EventoActualizacion,
  PaquetePos,
  Sucursal,
  UsuarioAdministrativo
} from '../modelos/modelos-operaciones';

@Injectable({ providedIn: 'root' })
export class OperacionesApiService {
  private readonly baseUrl = environment.apiBaseUrl.replace(/\/$/, '');

  constructor(private readonly http: HttpClient) {
  }

  obtenerSalud(): Observable<EstadoSaludApi> {
    return this.http.get<EstadoSaludApi>(`${this.baseUrl}/api/health`);
  }

  obtenerResumenNoc(): Observable<ResumenNocDashboard> {
    return this.http.get<ResumenNocDashboard>(`${this.baseUrl}/api/dashboard/resumen-noc`);
  }

  urlAbsoluta(ruta: string): string {
    if (!ruta) {
      return this.baseUrl || '/';
    }
    if (/^https?:\/\//i.test(ruta)) {
      return ruta;
    }
    return `${this.baseUrl}${ruta.startsWith('/') ? ruta : `/${ruta}`}`;
  }

  listarVersionesPos(): Observable<VersionPos[]> {
    return this.http.get<RespuestaPagina<VersionPos>>(`${this.baseUrl}/api/versiones-pos/page`, {
      params: new HttpParams().set('size', '100').set('sort', 'cargadoEn,desc')
    }).pipe(map((pagina) => pagina.content));
  }

  listarPaquetes(): Observable<PaquetePos[]> {
    return this.listarVersionesPos();
  }

  listarFarmacias(): Observable<Farmacia[]> {
    return this.http.get<RespuestaPagina<Farmacia>>(`${this.baseUrl}/api/farmacias/page`, {
      params: new HttpParams().set('size', '100').set('sort', 'code,asc')
    }).pipe(map((pagina) => pagina.content));
  }

  listarEstadoFarmacias(): Observable<EstadoOperacionalFarmacia[]> {
    return this.http.get<EstadoOperacionalFarmacia[]>(`${this.baseUrl}/api/farmacias/estado`);
  }

  listarGruposTrx(filtros: {
    codigo?: string;
    estado?: string;
    activo?: boolean | '';
    page?: number;
    size?: number;
    sort?: string;
  } = {}): Observable<RespuestaPagina<GrupoTrx>> {
    let parametros = new HttpParams();
    Object.entries(filtros).forEach(([clave, valor]) => {
      if (valor !== undefined && valor !== null && valor !== '') {
        parametros = parametros.set(clave, String(valor));
      }
    });
    return this.http.get<RespuestaPagina<GrupoTrx>>(`${this.baseUrl}/api/grupos-trx/page`, { params: parametros });
  }

  obtenerGrupoTrx(id: string): Observable<GrupoTrx> {
    return this.http.get<GrupoTrx>(`${this.baseUrl}/api/grupos-trx/${id}`);
  }

  crearGrupoTrx(datos: {
    codigo: string;
    nombre: string;
    descripcion?: string | null;
    maximoEquipos?: number | null;
    activo?: boolean | null;
  }): Observable<GrupoTrx> {
    return this.http.post<GrupoTrx>(`${this.baseUrl}/api/grupos-trx`, datos);
  }

  actualizarGrupoTrx(id: string, datos: {
    codigo: string;
    nombre: string;
    descripcion?: string | null;
    maximoEquipos?: number | null;
    activo?: boolean | null;
  }): Observable<GrupoTrx> {
    return this.http.put<GrupoTrx>(`${this.baseUrl}/api/grupos-trx/${id}`, datos);
  }

  pausarGrupoTrx(id: string, motivo = ''): Observable<GrupoTrx> {
    return this.http.post<GrupoTrx>(`${this.baseUrl}/api/grupos-trx/${id}/pausar`, { motivo });
  }

  reanudarGrupoTrx(id: string, motivo = ''): Observable<GrupoTrx> {
    return this.http.post<GrupoTrx>(`${this.baseUrl}/api/grupos-trx/${id}/reanudar`, { motivo });
  }

  retirarGrupoTrx(id: string, motivo = ''): Observable<GrupoTrx> {
    return this.http.post<GrupoTrx>(`${this.baseUrl}/api/grupos-trx/${id}/retirar`, { motivo });
  }

  asignarEquipoGrupoTrx(id: string, equipoId: string, motivo = ''): Observable<GrupoTrx> {
    return this.http.post<GrupoTrx>(`${this.baseUrl}/api/grupos-trx/${id}/equipos/${equipoId}`, { motivo });
  }

  quitarEquipoGrupoTrx(id: string, equipoId: string, motivo = ''): Observable<GrupoTrx> {
    return this.http.delete<GrupoTrx>(`${this.baseUrl}/api/grupos-trx/${id}/equipos/${equipoId}`, {
      body: { motivo }
    });
  }

  listarSucursales(): Observable<Sucursal[]> {
    return this.listarFarmacias();
  }

  listarEquiposPos(): Observable<EquipoPos[]> {
    return this.listarEquiposPosPaginados({ size: 100, sort: 'nombreEquipo,asc' })
      .pipe(map((pagina) => pagina.content));
  }

  listarEquipos(): Observable<Equipo[]> {
    return this.listarEquiposPos();
  }

  listarEquiposPosPaginados(
    filtros: {
      q?: string;
      status?: string;
      branchCode?: string;
      posVersion?: string;
      agentVersion?: string;
      lastHeartbeatFrom?: string;
      lastHeartbeatTo?: string;
      page?: number;
      size?: number;
      sort?: string;
    } = {}
  ): Observable<RespuestaPagina<EquipoPos>> {
    let parametros = new HttpParams();
    Object.entries(filtros).forEach(([clave, valor]) => {
      if (valor !== undefined && valor !== null && valor !== '') {
        parametros = parametros.set(clave, String(valor));
      }
    });
    return this.http.get<RespuestaPagina<EquipoPos>>(`${this.baseUrl}/api/equipos-pos/page`, {
      params: parametros
    });
  }

  listarEquiposPaginados(filtros: Parameters<OperacionesApiService['listarEquiposPosPaginados']>[0] = {}): Observable<RespuestaPagina<Equipo>> {
    return this.listarEquiposPosPaginados(filtros);
  }

  obtenerDetalleEquipoPos(id: string): Observable<DetalleEquipoPos> {
    return this.http.get<DetalleEquipoPos>(`${this.baseUrl}/api/equipos-pos/${id}`);
  }

  obtenerDetalleEquipo(id: string): Observable<DetalleEquipo> {
    return this.obtenerDetalleEquipoPos(id);
  }

  listarEquiposHuerfanos(): Observable<EquipoHuerfano[]> {
    return this.http.get<EquipoHuerfano[]>(`${this.baseUrl}/api/equipos-pos/huerfanos`);
  }

  asignarEquiposHuerfanos(asignaciones: Array<{ deviceId: string; branchId: string }>): Observable<ResumenAsignacionMasiva> {
    return this.http.post<ResumenAsignacionMasiva>(`${this.baseUrl}/api/equipos-pos/asignacion-masiva`, {
      assignments: asignaciones
    });
  }

  cargarVersionPos(version: string, archivo: File): Observable<VersionPos> {
    const datos = new FormData();
    datos.append('version', version);
    datos.append('file', archivo);
    return this.http.post<VersionPos>(`${this.baseUrl}/api/versiones-pos`, datos);
  }

  cargarPaquete(version: string, archivo: File): Observable<PaquetePos> {
    return this.cargarVersionPos(version, archivo);
  }

  aprobarVersionPos(id: string): Observable<VersionPos> {
    return this.http.post<VersionPos>(`${this.baseUrl}/api/versiones-pos/${id}/aprobar`, {});
  }

  aprobarPaquete(id: string): Observable<PaquetePos> {
    return this.aprobarVersionPos(id);
  }

  retirarVersionPos(id: string): Observable<VersionPos> {
    return this.http.post<VersionPos>(`${this.baseUrl}/api/versiones-pos/${id}/retirar`, {});
  }

  retirarPaquete(id: string): Observable<PaquetePos> {
    return this.retirarVersionPos(id);
  }

  descargarVersionPos(id: string): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/api/versiones-pos/${id}/descargar`, {
      responseType: 'blob'
    });
  }

  descargarPaquete(id: string): Observable<Blob> {
    return this.descargarVersionPos(id);
  }

  listarCampanasPos(): Observable<CampanaPos[]> {
    return this.http.get<RespuestaPagina<CampanaPos>>(`${this.baseUrl}/api/campanas-pos/page`, {
      params: new HttpParams().set('size', '100').set('sort', 'creadoEn,desc')
    }).pipe(map((pagina) => pagina.content));
  }

  listarDespliegues(): Observable<Despliegue[]> {
    return this.listarCampanasPos();
  }

  listarEventosAgente(limite = 100): Observable<EventoAgente[]> {
    return this.http.get<RespuestaPagina<EventoAgente>>(`${this.baseUrl}/api/eventos-agente/page`, {
      params: new HttpParams().set('size', String(limite)).set('sort', 'creadoEn,desc')
    }).pipe(map((pagina) => pagina.content));
  }

  listarEventos(limite = 100): Observable<EventoActualizacion[]> {
    return this.listarEventosAgente(limite);
  }

  listarAlertasPaginadas(
    filtros: {
      status?: string;
      severity?: string;
      type?: string;
      deviceId?: string;
      branchId?: string;
      branchCode?: string;
      hostname?: string;
      dateFrom?: string;
      dateTo?: string;
      page?: number;
      size?: number;
      sort?: string;
      networkEvent?: boolean;
    } = {}
  ): Observable<RespuestaPagina<AlertaOperativa>> {
    let parametros = new HttpParams();
    Object.entries(filtros).forEach(([clave, valor]) => {
      if (valor !== undefined && valor !== null && valor !== '') {
        parametros = parametros.set(clave, String(valor));
      }
    });
    return this.http.get<RespuestaPagina<AlertaOperativa>>(`${this.baseUrl}/api/alerts/page`, { params: parametros });
  }

  listarAlertas(
    limite = 100,
    filtros: {
      status?: string;
      severity?: string;
      type?: string;
      deviceId?: string;
      branchId?: string;
      branchCode?: string;
      hostname?: string;
      dateFrom?: string;
      dateTo?: string;
      page?: number;
      size?: number;
      sort?: string;
      networkEvent?: boolean;
    } = {}
  ): Observable<AlertaOperativa[]> {
    return this.listarAlertasPaginadas({
      ...filtros,
      size: filtros.size ?? limite
    }).pipe(map((pagina) => pagina.content));
  }

  reconocerAlerta(id: string): Observable<AlertaOperativa> {
    return this.http.post<AlertaOperativa>(`${this.baseUrl}/api/alerts/${id}/acknowledge`, {});
  }

  cerrarAlerta(id: string): Observable<AlertaOperativa> {
    return this.http.post<AlertaOperativa>(`${this.baseUrl}/api/alerts/${id}/close`, {});
  }

  listarAuditoria(
    limite = 100,
    filtros: {
      action?: string;
      entityType?: string;
      actorUsername?: string;
      from?: string;
      to?: string;
    } = {}
  ): Observable<AuditoriaAdministrativa[]> {
    return this.listarAuditoriaPaginada({
      ...filtros,
      size: limite
    }).pipe(map((pagina) => pagina.content));
  }

  listarAuditoriaPaginada(
    filtros: {
      action?: string;
      entityType?: string;
      actorUsername?: string;
      from?: string;
      to?: string;
      page?: number;
      size?: number;
      sort?: string;
    } = {}
  ): Observable<RespuestaPagina<AuditoriaAdministrativa>> {
    let parametros = new HttpParams();
    Object.entries(filtros).forEach(([clave, valor]) => {
      if (valor !== undefined && valor !== null && valor !== '') {
        parametros = parametros.set(clave, String(valor));
      }
    });
    return this.http.get<RespuestaPagina<AuditoriaAdministrativa>>(`${this.baseUrl}/api/audit-logs/page`, { params: parametros });
  }

  crearCampanaPos(solicitud: SolicitudCrearDespliegue): Observable<CampanaPos> {
    return this.http.post<CampanaPos>(`${this.baseUrl}/api/campanas-pos`, solicitud);
  }

  crearDespliegue(solicitud: SolicitudCrearDespliegue): Observable<Despliegue> {
    return this.crearCampanaPos(solicitud);
  }

  aprobarCampanaPos(id: string): Observable<CampanaPos> {
    return this.http.post<CampanaPos>(`${this.baseUrl}/api/campanas-pos/${id}/aprobar`, {});
  }

  lanzarCampanaPos(id: string): Observable<CampanaPos> {
    return this.http.post<CampanaPos>(`${this.baseUrl}/api/campanas-pos/${id}/lanzar`, {});
  }

  expandirCampanaPos(id: string): Observable<CampanaPos> {
    return this.http.post<CampanaPos>(`${this.baseUrl}/api/campanas-pos/${id}/expandir`, {});
  }

  aprobarDespliegue(id: string): Observable<Despliegue> {
    return this.aprobarCampanaPos(id);
  }

  lanzarDespliegue(id: string): Observable<Despliegue> {
    return this.lanzarCampanaPos(id);
  }

  expandirDespliegue(id: string): Observable<Despliegue> {
    return this.expandirCampanaPos(id);
  }

  pausarCampanaPos(id: string): Observable<CampanaPos> {
    return this.http.post<CampanaPos>(`${this.baseUrl}/api/campanas-pos/${id}/pausar`, {});
  }

  pausarDespliegue(id: string): Observable<Despliegue> {
    return this.pausarCampanaPos(id);
  }

  reanudarCampanaPos(id: string): Observable<CampanaPos> {
    return this.http.post<CampanaPos>(`${this.baseUrl}/api/campanas-pos/${id}/reanudar`, {});
  }

  reanudarDespliegue(id: string): Observable<Despliegue> {
    return this.reanudarCampanaPos(id);
  }

  cancelarCampanaPos(id: string): Observable<CampanaPos> {
    return this.http.post<CampanaPos>(`${this.baseUrl}/api/campanas-pos/${id}/cancelar`, {});
  }

  cancelarDespliegue(id: string): Observable<Despliegue> {
    return this.cancelarCampanaPos(id);
  }

  obtenerEstadoCampanaPos(id: string): Observable<EstadoDespliegue> {
    return this.http.get<EstadoDespliegue>(`${this.baseUrl}/api/campanas-pos/${id}/estado-por-equipo`);
  }

  obtenerEstadoDespliegue(id: string): Observable<EstadoDespliegue> {
    return this.obtenerEstadoCampanaPos(id);
  }

  obtenerEstadoCampanaPorFarmacia(
    id: string,
    filtros: {
      estadoTecnico?: string;
      estadoOperacional?: string;
      grupoTrx?: string;
      deTurno?: boolean | '';
      q?: string;
      page?: number;
      size?: number;
      sort?: string;
    } = {}
  ): Observable<ResumenEstadoCampanaFarmacia> {
    let parametros = new HttpParams();
    Object.entries(filtros).forEach(([clave, valor]) => {
      if (valor !== undefined && valor !== null && valor !== '') {
        parametros = parametros.set(clave, String(valor));
      }
    });
    return this.http.get<ResumenEstadoCampanaFarmacia>(`${this.baseUrl}/api/campanas-pos/${id}/estado-por-farmacia`, {
      params: parametros
    });
  }

  obtenerEstadoCampanaPorTrx(id: string): Observable<ResumenCampanaGruposTrx> {
    return this.http.get<ResumenCampanaGruposTrx>(`${this.baseUrl}/api/campanas-pos/${id}/estado-por-trx`);
  }

  asociarGrupoTrxCampana(id: string, grupoTrxId: string): Observable<CampanaGrupoTrx> {
    return this.http.post<CampanaGrupoTrx>(`${this.baseUrl}/api/campanas-pos/${id}/grupos-trx/${grupoTrxId}`, {});
  }

  quitarGrupoTrxCampana(id: string, grupoTrxId: string, motivo = ''): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/api/campanas-pos/${id}/grupos-trx/${grupoTrxId}`, {
      body: { motivo }
    });
  }

  pausarGrupoTrxCampana(id: string, grupoTrxId: string, motivo = ''): Observable<CampanaGrupoTrx> {
    return this.http.post<CampanaGrupoTrx>(`${this.baseUrl}/api/campanas-pos/${id}/grupos-trx/${grupoTrxId}/pausar`, { motivo });
  }

  reanudarGrupoTrxCampana(id: string, grupoTrxId: string, motivo = ''): Observable<CampanaGrupoTrx> {
    return this.http.post<CampanaGrupoTrx>(`${this.baseUrl}/api/campanas-pos/${id}/grupos-trx/${grupoTrxId}/reanudar`, { motivo });
  }

  planificarOrquestacion(id: string, solicitud: SolicitudPlanOrquestacion): Observable<PlanOrquestacion> {
    return this.http.post<PlanOrquestacion>(`${this.baseUrl}/api/orchestration/deployments/${id}/plan`, solicitud);
  }

  obtenerPlanOrquestacion(id: string): Observable<PlanOrquestacion> {
    return this.http.get<PlanOrquestacion>(`${this.baseUrl}/api/orchestration/deployments/${id}/plan`);
  }

  evaluarOrquestacion(id: string): Observable<PlanOrquestacion> {
    return this.http.post<PlanOrquestacion>(`${this.baseUrl}/api/orchestration/deployments/${id}/evaluate`, {});
  }

  iniciarOleada(idDespliegue: string, idOleada: string): Observable<PlanOrquestacion> {
    return this.http.post<PlanOrquestacion>(
      `${this.baseUrl}/api/orchestration/deployments/${idDespliegue}/waves/${idOleada}/start`,
      {}
    );
  }

  pausarOleada(idDespliegue: string, idOleada: string): Observable<PlanOrquestacion> {
    return this.http.post<PlanOrquestacion>(
      `${this.baseUrl}/api/orchestration/deployments/${idDespliegue}/waves/${idOleada}/pause`,
      {}
    );
  }

  reanudarOleada(idDespliegue: string, idOleada: string): Observable<PlanOrquestacion> {
    return this.http.post<PlanOrquestacion>(
      `${this.baseUrl}/api/orchestration/deployments/${idDespliegue}/waves/${idOleada}/resume`,
      {}
    );
  }

  cambiarContrasena(contrasenaActual: string, contrasenaNueva: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/api/admin/security/password`, {
      currentPassword: contrasenaActual,
      newPassword: contrasenaNueva
    });
  }

  listarUsuariosAdministrativos(): Observable<UsuarioAdministrativo[]> {
    return this.http.get<RespuestaPagina<UsuarioAdministrativo>>(`${this.baseUrl}/api/admin/users/page`, {
      params: new HttpParams().set('size', '100').set('sort', 'usuario,asc')
    }).pipe(map((pagina) => pagina.content));
  }

  crearUsuarioAdministrativo(datos: {
    username: string;
    password: string;
    fullName: string;
    email?: string | null;
    role: string;
  }): Observable<UsuarioAdministrativo> {
    return this.http.post<UsuarioAdministrativo>(`${this.baseUrl}/api/admin/users`, datos);
  }

  actualizarUsuarioAdministrativo(id: string, datos: { fullName: string; email?: string | null }): Observable<UsuarioAdministrativo> {
    return this.http.put<UsuarioAdministrativo>(`${this.baseUrl}/api/admin/users/${id}`, datos);
  }

  activarUsuarioAdministrativo(id: string): Observable<UsuarioAdministrativo> {
    return this.http.post<UsuarioAdministrativo>(`${this.baseUrl}/api/admin/users/${id}/activate`, {});
  }

  desactivarUsuarioAdministrativo(id: string): Observable<UsuarioAdministrativo> {
    return this.http.post<UsuarioAdministrativo>(`${this.baseUrl}/api/admin/users/${id}/deactivate`, {});
  }

  resetearContrasenaUsuarioAdministrativo(id: string, newPassword: string): Observable<UsuarioAdministrativo> {
    return this.http.post<UsuarioAdministrativo>(`${this.baseUrl}/api/admin/users/${id}/reset-password`, { newPassword });
  }

  cambiarRolUsuarioAdministrativo(id: string, role: string): Observable<UsuarioAdministrativo> {
    return this.http.post<UsuarioAdministrativo>(`${this.baseUrl}/api/admin/users/${id}/change-role`, { role });
  }
}
