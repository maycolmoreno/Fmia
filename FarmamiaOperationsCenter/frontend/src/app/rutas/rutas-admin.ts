import { Routes } from '@angular/router';
import { guardiaAdmin } from './guardia-admin';
import { RutaVaciaComponent } from './ruta-vacia.component';

export const rutasAdmin: Routes = [
  {
    path: 'login',
    component: RutaVaciaComponent
  },
  {
    path: 'operaciones',
    component: RutaVaciaComponent,
    canActivate: [guardiaAdmin]
  },
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'operaciones'
  },
  {
    path: '**',
    redirectTo: 'operaciones'
  }
];
