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
  { path: 'reservar', component: Reservas, canActivate: [authGuard], data: { vista: 'misReservas' } },

  // Administrador (requiere sesion + rol admin).
  { path: 'admin/habitaciones', component: Habitaciones, canActivate: [adminGuard], data: { vista: 'lista' } },
  { path: 'admin/habitaciones/nueva', component: Habitaciones, canActivate: [adminGuard], data: { vista: 'formulario' } },
  { path: 'admin/servicios', component: Servicios, canActivate: [adminGuard], data: { vista: 'lista' } },
  { path: 'admin/servicios/nuevo', component: Servicios, canActivate: [adminGuard], data: { vista: 'formulario' } },
  { path: 'admin/perfiles', component: Perfiles, canActivate: [adminGuard], data: { vista: 'lista' } },
  { path: 'admin/perfiles/nuevo', component: Perfiles, canActivate: [adminGuard], data: { vista: 'crearManual' } },
  { path: 'admin/ofertas', component: Ofertas, canActivate: [adminGuard], data: { vista: 'lista' } },
  { path: 'admin/ofertas/nueva', component: Ofertas, canActivate: [adminGuard], data: { vista: 'formulario' } },
  { path: 'admin/reservas', component: Reservas, canActivate: [adminGuard], data: { vista: 'lista' } },
  { path: 'admin/reservas/nueva', component: Reservas, canActivate: [adminGuard], data: { vista: 'crearAdmin' } },

  // Raiz: a /reservar si hay sesion, si no a /login (se evalua en contexto de inyeccion).
  {
    path: '',
    pathMatch: 'full',
    redirectTo: () => {
      const sesion = inject(SessionManager);
      if (!sesion.estaAutenticado()) return '/login';
      return sesion.esAdministrador() ? '/admin/reservas' : '/reservar';
    },
  },

  // Comodin: cualquier ruta desconocida va a /login.
  { path: '**', redirectTo: 'login' },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRouterModule { }
