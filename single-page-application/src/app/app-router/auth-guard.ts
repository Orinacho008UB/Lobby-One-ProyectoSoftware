import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';

import { SessionManager } from '../session-manager/session-manager';

/**
 * Guard de autenticacion: exige una sesion iniciada. Sin sesion redirige a
 * /login. (Componente "App Router" del C4: consulta el Session Manager.)
 */
export const authGuard: CanActivateFn = (): boolean | UrlTree => {
  const sesion = inject(SessionManager);
  const router = inject(Router);
  return sesion.estaAutenticado() ? true : router.parseUrl('/login');
};

/**
 * Guard de administrador: exige sesion + rol ADMINISTRADOR. Sin sesion redirige
 * a /login; con sesion de Cliente bloquea y redirige a /reservar.
 */
export const adminGuard: CanActivateFn = (): boolean | UrlTree => {
  const sesion = inject(SessionManager);
  const router = inject(Router);
  if (!sesion.estaAutenticado()) {
    return router.parseUrl('/login');
  }
  return sesion.esAdministrador() ? true : router.parseUrl('/reservar');
};
