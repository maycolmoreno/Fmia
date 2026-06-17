import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { RespuestaLogin } from '../modelos/modelos-operaciones';

const CLAVE_SESION = 'farmamia_admin_session';

@Injectable({ providedIn: 'root' })
export class SesionAdminService {
  private readonly baseUrl = environment.apiBaseUrl.replace(/\/$/, '');
  private sesionActual?: RespuestaLogin;

  constructor(private readonly http: HttpClient) {
    const guardada = localStorage.getItem(CLAVE_SESION);
    this.sesionActual = guardada ? JSON.parse(guardada) as RespuestaLogin : undefined;
  }

  login(usuario: string, contrasena: string): Observable<RespuestaLogin> {
    return this.http.post<RespuestaLogin>(`${this.baseUrl}/api/auth/login`, {
      username: usuario,
      password: contrasena
    }).pipe(
      tap((sesion) => {
        this.sesionActual = sesion;
        localStorage.setItem(CLAVE_SESION, JSON.stringify(sesion));
      })
    );
  }

  cerrarSesion(): void {
    this.sesionActual = undefined;
    localStorage.removeItem(CLAVE_SESION);
  }

  token(): string | undefined {
    if (!this.sesionActual) {
      return undefined;
    }

    if (new Date(this.sesionActual.expiresAt).getTime() <= Date.now()) {
      this.cerrarSesion();
      return undefined;
    }

    return this.sesionActual.accessToken;
  }

  autenticado(): boolean {
    return !!this.token();
  }

  nombreUsuario(): string {
    return this.sesionActual?.fullName ?? this.sesionActual?.username ?? '';
  }

  rol(): string {
    return this.sesionActual?.role ?? '';
  }

  esAdmin(): boolean {
    return this.rol() === 'ADMIN';
  }

  puedeVerAuditoria(): boolean {
    return ['ADMIN', 'AUDITOR'].includes(this.rol());
  }

  puedeOperar(): boolean {
    return ['ADMIN', 'OPERATOR'].includes(this.rol());
  }

  puedeVerEventosYAlertas(): boolean {
    return ['ADMIN', 'OPERATOR', 'AUDITOR'].includes(this.rol());
  }
}
