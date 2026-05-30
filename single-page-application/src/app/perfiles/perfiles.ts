import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import {
  ApiClient,
  ApiError,
  CrearHuespedManual,
  HuespedCreadoResponse,
  Perfil,
  Reserva,
} from '../api-client/api-client';
import { SessionManager } from '../session-manager/session-manager';

type Vista = 'login' | 'registro' | 'perfil' | 'lista' | 'crearManual' | 'detalle';

/**
 * Componente Perfiles (C4): registro, login, "mi perfil" y gestión de huéspedes (admin).
 *
 * La misma clase sirve a /registro, /login, /perfil y /admin/perfiles*; la vista
 * activa se decide por el {@code data.vista} de la ruta. Usa Reactive Forms y el
 * API Client; tras el login guarda la sesion en el Session Manager.
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
  /** Datos completos del perfil propio (email, telefono, cedula) para "mi perfil". */
  protected perfilCompleto: Perfil | null = null;

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

  // --- Listado de huéspedes ---
  protected perfiles: Perfil[] = [];
  protected reservas: Reserva[] = [];
  protected cargando = false;
  protected errorCarga = '';
  protected filtroBusqueda = '';
  protected filtroClasificacion = '';

  // --- Detalle ---
  protected perfilSeleccionado: Perfil | null = null;
  protected notasEditando = false;
  protected notasTexto = '';
  protected guardandoNotas = false;

  // --- Crear manual ---
  protected contrasenaProvisional = '';
  protected huespedCreado: Perfil | null = null;
  protected crearForm = this.fb.nonNullable.group({
    nombre: this.fb.nonNullable.control('', Validators.required),
    email: this.fb.nonNullable.control('', [Validators.required, Validators.email]),
    telefono: this.fb.nonNullable.control('', Validators.required),
    cedula: this.fb.nonNullable.control(''),
  });

  ngOnInit(): void {
    this.route.data.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((data) => {
      const vistaRuta = (data['vista'] as Vista) ?? 'login';
      this.vista.set(vistaRuta);
      this.limpiarMensajes();
      if (vistaRuta === 'lista') {
        this.cargarDatosListado();
      }
      if (vistaRuta === 'perfil') {
        this.cargarPerfilPropio();
      }
    });
    // Mensaje de "registro exitoso" al volver de /registro a /login.
    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      if (params.get('registrado') === '1') {
        this.mensajeExito = 'Registro exitoso. Ya puedes iniciar sesion.';
      }
    });
  }

  // --- Getters computados para KPIs y filtrado ---

  get perfilesFiltrados(): Perfil[] {
    return this.perfiles.filter(p => {
      const busqueda = this.filtroBusqueda.toLowerCase();
      const coincide = !busqueda ||
        p.nombre?.toLowerCase().includes(busqueda) ||
        p.email?.toLowerCase().includes(busqueda) ||
        p.telefono?.includes(busqueda) ||
        p.cedula?.includes(busqueda);
      const porClasif = !this.filtroClasificacion ||
        this.clasificacion(p.id) === this.filtroClasificacion;
      return coincide && porClasif;
    });
  }

  get totalHuespedes(): number { return this.perfiles.length; }
  get totalVip(): number {
    return this.perfiles.filter(p => this.clasificacion(p.id) === 'VIP').length;
  }
  get totalFrecuentes(): number {
    return this.perfiles.filter(p => this.clasificacion(p.id) === 'Frecuente').length;
  }

  // --- Métodos de navegación y lógica de huéspedes ---

  /** Carga los datos completos del perfil en sesion para la vista "mi perfil". */
  cargarPerfilPropio(): void {
    const id = this.sesion.usuario()?.id;
    if (!id) return;
    this.api.obtenerPerfilPorId(id).subscribe({
      next: (perfil) => { this.perfilCompleto = perfil; },
      error: () => { this.perfilCompleto = null; },
    });
  }

  cargarDatosListado(): void {
    this.cargando = true;
    this.errorCarga = '';
    this.api.consultarTodosPerfiles().subscribe({
      next: (perfiles) => {
        this.perfiles = perfiles;
        this.api.consultarReservas().subscribe({
          next: (reservas) => { this.reservas = reservas; this.cargando = false; },
          error: () => { this.reservas = []; this.cargando = false; },
        });
      },
      error: () => { this.errorCarga = 'Error al cargar los huéspedes.'; this.cargando = false; },
    });
  }

  irALista(): void {
    this.perfilSeleccionado = null;
    this.contrasenaProvisional = '';
    this.huespedCreado = null;
    this.crearForm.reset();
    this.limpiarMensajes();
    this.vista.set('lista');
    this.cargarDatosListado();
  }

  irACrearManual(): void {
    this.contrasenaProvisional = '';
    this.huespedCreado = null;
    this.crearForm.reset();
    this.limpiarMensajes();
    this.vista.set('crearManual');
  }

  verDetalle(perfil: Perfil): void {
    this.perfilSeleccionado = perfil;
    this.notasTexto = perfil.notasInternas ?? '';
    this.notasEditando = false;
    this.vista.set('detalle');
  }

  clasificacion(perfilId: string): string {
    const total = this.reservas.filter(r => r.perfilId === perfilId).length;
    if (total >= 10) return 'VIP';
    if (total >= 5) return 'Frecuente';
    if (total >= 2) return 'Regular';
    return 'Nuevo';
  }

  badgeClasificacion(perfilId: string): string {
    const clases: Record<string, string> = {
      'VIP': 'badge-vip',
      'Frecuente': 'badge-frecuente',
      'Regular': 'badge-regular',
      'Nuevo': 'badge-nuevo',
    };
    return clases[this.clasificacion(perfilId)] ?? '';
  }

  reservasDelPerfil(perfilId: string): Reserva[] {
    return this.reservas.filter(r => r.perfilId === perfilId);
  }

  crearHuesped(): void {
    if (this.crearForm.invalid) {
      this.crearForm.markAllAsTouched();
      return;
    }
    this.limpiarMensajes();
    this.enviando = true;
    const v = this.crearForm.getRawValue();
    const datos: CrearHuespedManual = {
      nombre: v.nombre,
      email: v.email,
      telefono: v.telefono,
      cedula: v.cedula || undefined,
    };
    this.api.crearHuespedManual(datos).subscribe({
      next: (resp: HuespedCreadoResponse) => {
        this.enviando = false;
        this.huespedCreado = resp.perfil;
        this.contrasenaProvisional = resp.contrasenaProvisional;
      },
      error: (e: ApiError) => {
        this.enviando = false;
        if (e.errores) {
          this.erroresBackend = e.errores;
        } else {
          this.mensajeError = e.mensaje || 'No se pudo crear el huésped.';
        }
      },
    });
  }

  activarEdicionNotas(): void {
    this.notasEditando = true;
  }

  guardarNotas(): void {
    if (!this.perfilSeleccionado) return;
    this.guardandoNotas = true;
    this.api.actualizarNotasInternas(this.perfilSeleccionado.id, this.notasTexto).subscribe({
      next: (actualizado) => {
        this.guardandoNotas = false;
        this.notasEditando = false;
        this.perfilSeleccionado = actualizado;
        const idx = this.perfiles.findIndex(p => p.id === actualizado.id);
        if (idx !== -1) { this.perfiles[idx] = actualizado; }
      },
      error: () => {
        this.guardandoNotas = false;
        this.mensajeError = 'No se pudieron guardar las notas.';
      },
    });
  }

  // --- Métodos de autenticación existentes ---

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
          // Muestra la confirmacion en la pantalla de registro antes de redirigir (HU-03).
          this.mensajeExito = 'Registro exitoso. Redirigiendo al inicio de sesion...';
          setTimeout(() => {
            this.router.navigate(['/login'], { queryParams: { registrado: '1' } });
          }, 1500);
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
        const destino = perfil.rol === 'ADMINISTRADOR' ? '/admin/reservas' : '/reservar';
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
