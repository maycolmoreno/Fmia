import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { SesionAdminService } from './sesion-admin.service';

export const interceptorAuthAdmin: HttpInterceptorFn = (solicitud, siguiente) => {
  const sesion = inject(SesionAdminService);
  const token = sesion.token();

  if (!token || solicitud.url.includes('/api/auth/login')) {
    return siguiente(solicitud);
  }

  return siguiente(solicitud.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`
    }
  }));
};
