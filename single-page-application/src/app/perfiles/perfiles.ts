import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { ApiClient, ApiError, Perfil } from '../api-client/api-client';
import { SessionManager } from '../session-manager/session-manager';

type Vista = 'login' | 'registro' | 'perfil';

/**
 * Componente Perfiles (C4): registro, login y "mi perfil".
 *
 * La misma clase sirve a /registro, /login y /perfil; la vista activa se decide
 * por el {@code data.vista} de la ruta. Usa Reactive Forms y el API Client; tras
 * el login guarda la sesion en el Session Manager.
 */
@Component({
  selector: 'app-perfiles',
  standalone: false,
  templateUrl: './perfiles.html',
  styleUrl: './perfiles.css',
})
export class Perfiles implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(ApiClient);
  private readonly sesion = inject(SessionManager);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly vista = signal<Vista>('login');
  /** Usuario en sesion (para la vista "mi perfil"). */
  protected readonly usuario = this.sesion.usuario;

  /** Errores por campo devueltos por el backend (campo -> mensaje). */
  protected erroresBackend: Record<string, string> = {};
  protected mensajeError = '';
  protected mensajeExito = '';
  protected enviando = false;

  protected readonly registroForm = this.fb.nonNullable.group({
    nombre: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    telefono: ['', Validators.required],
    cedula: ['', Validators.required],
    contrasena: ['', [Validators.required, Validators.minLength(8)]],
  });

  protected readonly loginForm = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    contrasena: ['', Validators.required],
  });

  ngOnInit(): void {
    this.route.data.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((data) => {
      this.vista.set((data['vista'] as Vista) ?? 'login');
      this.limpiarMensajes();
    });
    // Mensaje de "registro exitoso" al volver de /registro a /login.
    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      if (params.get('registrado') === '1') {
        this.mensajeExito = 'Registro exitoso. Ya puedes iniciar sesion.';
      }
    });
  }

  /** /registro: crea el cliente y, si todo va bien, lleva al login. */
  registrar(): void {
    this.limpiarMensajes();
    if (this.registroForm.invalid) {
      this.registroForm.markAllAsTouched();
      return;
    }
    this.enviando = true;
    const v = this.registroForm.getRawValue();
    this.api
      .registrarPerfil({
        nombre: v.nombre,
        email: v.email,
        telefono: v.telefono,
        cedula: v.cedula,
        // Sprint 1: la contrasena viaja en contrasenaHash (el backend la hashea).
        contrasenaHash: v.contrasena,
      })
      .subscribe({
        next: () => {
          this.enviando = false;
          this.router.navigate(['/login'], { queryParams: { registrado: '1' } });
        },
        error: (e: ApiError) => {
          this.enviando = false;
          if (e.errores) {
            this.erroresBackend = e.errores;
          } else {
            this.mensajeError = e.mensaje || 'No se pudo completar el registro.';
          }
        },
      });
  }

  /** /login: valida credenciales, guarda la sesion y redirige segun el rol. */
  iniciarSesion(): void {
    this.limpiarMensajes();
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }
    this.enviando = true;
    const { email, contrasena } = this.loginForm.getRawValue();
    this.api.login(email, contrasena).subscribe({
      next: (perfil: Perfil) => {
        this.enviando = false;
        this.sesion.iniciarSesion({ id: perfil.id, nombre: perfil.nombre, rol: perfil.rol, email: perfil.email });
        const destino = perfil.rol === 'ADMINISTRADOR' ? '/admin/habitaciones/nueva' : '/reservar';
        this.router.navigateByUrl(destino);
      },
      error: (e: ApiError) => {
        this.enviando = false;
        // 401 -> mensaje generico, sin filtrar si fallo el email o la contrasena.
        this.mensajeError =
          e.status === 401 ? 'Email o contrasena incorrectos.' : e.mensaje || 'No se pudo iniciar sesion.';
      },
    });
  }

  /** /perfil: cierra la sesion y vuelve al login. */
  cerrarSesion(): void {
    this.sesion.cerrarSesion();
    this.router.navigateByUrl('/login');
  }

  private limpiarMensajes(): void {
    this.erroresBackend = {};
    this.mensajeError = '';
    this.mensajeExito = '';
  }
}
