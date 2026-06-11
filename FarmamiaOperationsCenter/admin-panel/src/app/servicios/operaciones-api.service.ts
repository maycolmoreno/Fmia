import { HttpClient } from '@angular/common/http';
import { HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../environments/environment';
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
  RespuestaPagina,
  ResumenDashboard,
  SolicitudCrearDespliegue,
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

  obtenerResumenDashboard(): Observable<ResumenDashboard> {
    return this.http.get<ResumenDashboard>(`${this.baseUrl}/api/dashboard/summary`);
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

  listarPaquetes(): Observable<PaquetePos[]> {
    return this.http.get<RespuestaPagina<PaquetePos>>(`${this.baseUrl}/api/packages/page`, {
      params: new HttpParams().set('size', '100').set('sort', 'uploadedAt,desc')
    }).pipe(map((pagina) => pagina.content));
  }

  listarSucursales(): Observable<Sucursal[]> {
    return this.http.get<Sucursal[]>(`${this.baseUrl}/api/branches`);
  }

  listarEquipos(): Observable<Equipo[]> {
    return this.http.get<RespuestaPagina<Equipo>>(`${this.baseUrl}/api/devices/page`, {
      params: new HttpParams().set('size', '100').set('sort', 'nombreEquipo,asc')
    }).pipe(map((pagina) => pagina.content));
  }

  obtenerDetalleEquipo(id: string): Observable<DetalleEquipo> {
    return this.http.get<DetalleEquipo>(`${this.baseUrl}/api/devices/${id}`);
  }

  cargarPaquete(version: string, archivo: File): Observable<PaquetePos> {
    const datos = new FormData();
    datos.append('version', version);
    datos.append('file', archivo);
    return this.http.post<PaquetePos>(`${this.baseUrl}/api/packages`, datos);
  }

  aprobarPaquete(id: string): Observable<PaquetePos> {
    return this.http.post<PaquetePos>(`${this.baseUrl}/api/packages/${id}/approve`, {});
  }

  retirarPaquete(id: string): Observable<PaquetePos> {
    return this.http.post<PaquetePos>(`${this.baseUrl}/api/packages/${id}/retire`, {});
  }

  descargarPaquete(id: string): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/api/packages/${id}/download`, {
      responseType: 'blob'
    });
  }

  listarDespliegues(): Observable<Despliegue[]> {
    return this.http.get<RespuestaPagina<Despliegue>>(`${this.baseUrl}/api/deployments/page`, {
      params: new HttpParams().set('size', '100').set('sort', 'createdAt,desc')
    }).pipe(map((pagina) => pagina.content));
  }

  listarEventos(limite = 100): Observable<EventoActualizacion[]> {
    return this.http.get<RespuestaPagina<EventoActualizacion>>(`${this.baseUrl}/api/update-events/page`, {
      params: new HttpParams().set('size', String(limite)).set('sort', 'createdAt,desc')
    }).pipe(map((pagina) => pagina.content));
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

  crearDespliegue(solicitud: SolicitudCrearDespliegue): Observable<Despliegue> {
    return this.http.post<Despliegue>(`${this.baseUrl}/api/deployments`, solicitud);
  }

  pausarDespliegue(id: string): Observable<Despliegue> {
    return this.http.post<Despliegue>(`${this.baseUrl}/api/deployments/${id}/pause`, {});
  }

  reanudarDespliegue(id: string): Observable<Despliegue> {
    return this.http.post<Despliegue>(`${this.baseUrl}/api/deployments/${id}/resume`, {});
  }

  cancelarDespliegue(id: string): Observable<Despliegue> {
    return this.http.post<Despliegue>(`${this.baseUrl}/api/deployments/${id}/cancel`, {});
  }

  obtenerEstadoDespliegue(id: string): Observable<EstadoDespliegue> {
    return this.http.get<EstadoDespliegue>(`${this.baseUrl}/api/deployments/${id}/status`);
  }

  cambiarContrasena(contrasenaActual: string, contrasenaNueva: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/api/admin/security/password`, {
      currentPassword: contrasenaActual,
      newPassword: contrasenaNueva
    });
  }

  listarUsuariosAdministrativos(): Observable<UsuarioAdministrativo[]> {
    return this.http.get<RespuestaPagina<UsuarioAdministrativo>>(`${this.baseUrl}/api/admin/users/page`, {
      params: new HttpParams().set('size', '100').set('sort', 'username,asc')
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
