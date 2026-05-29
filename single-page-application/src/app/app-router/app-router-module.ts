import { inject, NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { adminGuard, authGuard } from './auth-guard';
import { SessionManager } from '../session-manager/session-manager';
import { Perfiles } from '../perfiles/perfiles';
import { Reservas } from '../reservas/reservas';
import { Habitaciones } from '../habitaciones/habitaciones';
import { Servicios } from '../servicios/servicios';
import { Ofertas } from '../ofertas/ofertas';

// Componente "App Router" (C4 Nivel 3): rutas de la SPA + guards de acceso.
// Por ahora los destinos son los componentes (placeholder) ya existentes.
const routes: Routes = [
  // Publicas (sin sesion).
  { path: 'login', component: Perfiles, data: { vista: 'login' } },
  { path: 'registro', component: Perfiles, data: { vista: 'registro' } },

  // Cliente (requiere sesion).
  { path: 'perfil', component: Perfiles, canActivate: [authGuard], data: { vista: 'perfil' } },
  { path: 'reservar', component: Reservas, canActivate: [authGuard] },

  // Administrador (requiere sesion + rol admin).
  { path: 'admin/habitaciones/nueva', component: Habitaciones, canActivate: [adminGuard] },
  { path: 'admin/servicios/nuevo', component: Servicios, canActivate: [adminGuard] },
  { path: 'admin/ofertas/nueva', component: Ofertas, canActivate: [adminGuard] },

  // Raiz: a /reservar si hay sesion, si no a /login (se evalua en contexto de inyeccion).
  {
    path: '',
    pathMatch: 'full',
    redirectTo: () => (inject(SessionManager).estaAutenticado() ? '/reservar' : '/login'),
  },

  // Comodin: cualquier ruta desconocida va a /login.
  { path: '**', redirectTo: 'login' },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRouterModule { }
