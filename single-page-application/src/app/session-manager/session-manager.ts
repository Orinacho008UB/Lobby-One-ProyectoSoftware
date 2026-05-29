import { Injectable, signal } from '@angular/core';

export type Rol = 'CLIENTE' | 'ADMINISTRADOR';

/** Identidad del usuario autenticado. NUNCA incluye contrasena ni hash. */
export interface UsuarioSesion {
  id: string;
  nombre: string;
  rol: Rol;
}

/**
 * Estado de sesion del cliente (componente "Session Manager" del C4).
 *
 * Guarda solo la identidad { id, nombre, rol }, la persiste en localStorage
 * (sobrevive a un refresco) y la rehidrata al iniciar la app. No hay tokens/JWT:
 * la sesion es estado de cliente que refuerza el flujo de UI (Sprint 1).
 */
@Injectable({
  providedIn: 'root',
})
export class SessionManager {
  private static readonly CLAVE = 'lobbyone.sesion';

  private readonly _usuario = signal<UsuarioSesion | null>(this.rehidratar());

  /** Usuario autenticado actual (o null). Reactivo para componentes y guards. */
  readonly usuario = this._usuario.asReadonly();

  /** Guarda la sesion tras un login exitoso (solo id, nombre y rol). */
  iniciarSesion(perfil: UsuarioSesion): void {
    const sesion: UsuarioSesion = { id: perfil.id, nombre: perfil.nombre, rol: perfil.rol };
    this._usuario.set(sesion);
    this.persistir(sesion);
  }

  /** Cierra la sesion y limpia el almacenamiento. */
  cerrarSesion(): void {
    this._usuario.set(null);
    this.almacenamiento()?.removeItem(SessionManager.CLAVE);
  }

  estaAutenticado(): boolean {
    return this._usuario() !== null;
  }

  esAdministrador(): boolean {
    return this._usuario()?.rol === 'ADMINISTRADOR';
  }

  private persistir(sesion: UsuarioSesion): void {
    this.almacenamiento()?.setItem(SessionManager.CLAVE, JSON.stringify(sesion));
  }

  private rehidratar(): UsuarioSesion | null {
    const crudo = this.almacenamiento()?.getItem(SessionManager.CLAVE);
    if (!crudo) {
      return null;
    }
    try {
      const datos = JSON.parse(crudo) as Partial<UsuarioSesion>;
      if (
        typeof datos.id === 'string' &&
        typeof datos.nombre === 'string' &&
        (datos.rol === 'CLIENTE' || datos.rol === 'ADMINISTRADOR')
      ) {
        return { id: datos.id, nombre: datos.nombre, rol: datos.rol };
      }
    } catch {
      // Dato corrupto en localStorage: se ignora y se trata como sin sesion.
    }
    return null;
  }

  private almacenamiento(): Storage | null {
    return typeof localStorage !== 'undefined' ? localStorage : null;
  }
}
