import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { SesionAdminService } from '../servicios/sesion-admin.service';

export const guardiaAdmin: CanActivateFn = () => {
  const sesion = inject(SesionAdminService);
  const router = inject(Router);

  if (sesion.autenticado()) {
    return true;
  }

  return router.createUrlTree(['/login']);
};
