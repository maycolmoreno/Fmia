import { HttpClient } from '@angular/common/http';
import { HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
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
    return this.http.get<PaquetePos[]>(`${this.baseUrl}/api/packages`);
  }

  listarSucursales(): Observable<Sucursal[]> {
    return this.http.get<Sucursal[]>(`${this.baseUrl}/api/branches`);
  }

  listarEquipos(): Observable<Equipo[]> {
    return this.http.get<Equipo[]>(`${this.baseUrl}/api/devices`);
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
    return this.http.get<Despliegue[]>(`${this.baseUrl}/api/deployments`);
  }

  listarEventos(limite = 100): Observable<EventoActualizacion[]> {
    return this.http.get<EventoActualizacion[]>(`${this.baseUrl}/api/update-events?limit=${limite}`);
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
    let parametros = new HttpParams().set('limit', String(limite));
    Object.entries(filtros).forEach(([clave, valor]) => {
      if (valor !== undefined && valor !== null && valor !== '') {
        parametros = parametros.set(clave, String(valor));
      }
    });
    return this.http.get<AlertaOperativa[]>(`${this.baseUrl}/api/alerts`, { params: parametros });
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
    let parametros = new HttpParams().set('limit', String(limite));
    Object.entries(filtros).forEach(([clave, valor]) => {
      if (valor) {
        parametros = parametros.set(clave, valor);
      }
    });
    return this.http.get<AuditoriaAdministrativa[]>(`${this.baseUrl}/api/audit-logs`, { params: parametros });
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
    return this.http.get<UsuarioAdministrativo[]>(`${this.baseUrl}/api/admin/users`);
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
