import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import {
  ApiClient,
  ApiError,
  CotizarPayload,
  CrearReservaPayload,
  DesgloseReserva,
  Habitacion,
  Oferta,
  Perfil,
  PerfilPublico,
  Reserva,
  Servicio,
} from '../api-client/api-client';
import { SessionManager } from '../session-manager/session-manager';

/**
 * Componente Reservas (C4): flujo multi-paso del cliente para crear una reserva.
 *
 * Paso 1 — Fechas y capacidad minima opcional.
 * Paso 2 — Oferta activa (opcional; fuerza el tipo de habitacion).
 * Paso 3 — Seleccion de habitacion disponible (busca la disponibilidad al entrar).
 * Paso 4 — Servicios adicionales (los del bundle van incluidos, no se cobran extra).
 * Paso 5 — Email de contacto + cotizacion (preview) + confirmacion (persiste).
 * Paso 6 — Pantalla de exito con detalles de la reserva creada.
 */
@Component({
  selector: 'app-reservas',
  standalone: false,
  templateUrl: './reservas.html',
  styleUrl: './reservas.css',
})
export class Reservas implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(ApiClient);
  private readonly sesion = inject(SessionManager);

  protected paso = 1;

  // --- Paso 1: Fechas ---
  protected readonly hoy = new Date().toISOString().split('T')[0];
  protected readonly fechasForm = this.fb.group({
    fechaEntrada: this.fb.nonNullable.control('', Validators.required),
    fechaSalida: this.fb.nonNullable.control('', Validators.required),
    // Capacidad minima opcional: el ERS pide filtrar por capacidad solo si el cliente la indica.
    capacidad: this.fb.control<number | null>(null, [Validators.min(1)]),
  });

  // --- Paso 2: Oferta ---
  protected ofertasActivas: Oferta[] = [];
  protected ofertaSeleccionada: Oferta | null = null;

  // --- Paso 3: Habitacion ---
  protected habitacionesDisponibles: Habitacion[] = [];
  protected habitacionSeleccionada: Habitacion | null = null;
  protected cargandoDisponibilidad = false;

  // --- Paso 4: Servicios ---
  protected serviciosActivos: Servicio[] = [];
  protected serviciosSeleccionados = new Set<string>();

  // --- Paso 5: Cotizar y confirmar ---
  protected readonly emailForm = this.fb.group({
    emailContacto: this.fb.nonNullable.control('', [Validators.required, Validators.email]),
  });
  protected desglose: DesgloseReserva | null = null;
  protected cotizando = false;
  protected confirmando = false;

  // --- Paso 6: Resultado ---
  protected reservaCreada: Reserva | null = null;

  // --- Estado de error global ---
  protected erroresBackend: Record<string, string> = {};
  protected mensajeError = '';

  // --- Vista y navegacion ---
  protected vista: 'crear' | 'lista' | 'detalle' | 'crearAdmin' | 'misReservas' | 'detalleCliente' = 'crear';
  private readonly route = inject(ActivatedRoute);
  private readonly destruir$ = new Subject<void>();

  // --- Listado admin ---
  protected reservas: Reserva[] = [];
  protected cargandoReservas = false;
  protected errorCarga = '';
  protected filtroEstado = '';

  // --- Detalle ---
  protected reservaSeleccionada: Reserva | null = null;

  // --- Datos de resolución (cargados junto con las reservas) ---
  protected perfiles: Perfil[] = [];
  protected habitaciones: Habitacion[] = [];
  protected todosLosServicios: Servicio[] = [];
  protected todasLasOfertas: Oferta[] = [];

  // --- Listado: búsqueda adicional por texto ---
  protected filtroBusqueda = '';

  // --- Mis Reservas (cliente) ---
  protected misReservas: Reserva[] = [];
  protected cargandoMisReservas = false;
  protected errorMisReservas = '';
  protected filtroEstadoCliente = '';
  protected reservaDetalleCliente: Reserva | null = null;

  // --- Crear reserva admin (paso 0 busqueda de huesped) ---
  protected emailBusqueda = '';
  protected huespedEncontrado: PerfilPublico | null = null;
  protected buscandoHuesped = false;
  protected errorBusqueda = '';
  protected perfilIdAdmin: string | null = null;

  ngOnInit(): void {
    const vistaRuta = this.route.snapshot.data['vista'] as string;
    if (vistaRuta === 'lista') {
      this.vista = 'lista';
      this.cargarReservas();
    } else if (vistaRuta === 'crearAdmin') {
      this.vista = 'crearAdmin';
      this.cargarDatosCreacion();
    } else if (vistaRuta === 'misReservas') {
      this.vista = 'misReservas';
      this.cargarMisReservas();
    } else {
      this.vista = 'crear';
      this.cargarDatosCreacion();
    }
  }

  private cargarDatosCreacion(): void {
    const email = this.sesion.usuario()?.email;
    if (email) {
      this.emailForm.controls.emailContacto.setValue(email);
    }
    this.api.consultarOfertasActivas().subscribe({
      next: (o) => { this.ofertasActivas = o; },
      error: () => {},
    });
    this.api.consultarServiciosActivos().subscribe({
      next: (s) => { this.serviciosActivos = s; },
      error: () => {},
    });
  }

  // ==================== PASO 1: FECHAS ====================

  avanzarFechas(): void {
    this.limpiarErrores();
    if (this.fechasForm.invalid) {
      this.fechasForm.markAllAsTouched();
      return;
    }
    const { fechaEntrada, fechaSalida } = this.fechasForm.getRawValue();
    if (fechaEntrada >= fechaSalida) {
      this.mensajeError = 'La fecha de entrada debe ser anterior a la de salida.';
      return;
    }
    this.paso = 2;
  }

  // ==================== PASO 2: OFERTA ====================

  seleccionarOferta(oferta: Oferta): void {
    this.ofertaSeleccionada = oferta;
  }

  quitarOferta(): void {
    this.ofertaSeleccionada = null;
  }

  /** Busca disponibilidad (con la oferta elegida o sin ella) y avanza al paso 3. */
  avanzarOferta(): void {
    this.limpiarErrores();
    const { fechaEntrada, fechaSalida, capacidad } = this.fechasForm.getRawValue();
    this.cargandoDisponibilidad = true;
    this.habitacionSeleccionada = null;
    this.habitacionesDisponibles = [];

    this.api.consultarDisponibilidad(
      fechaEntrada,
      fechaSalida,
      this.ofertaSeleccionada?.tipoHabitacion,
      capacidad ?? undefined,
      this.ofertaSeleccionada?.id,
    ).subscribe({
      next: (habitaciones) => {
        this.cargandoDisponibilidad = false;
        this.habitacionesDisponibles = habitaciones;
        this.paso = 3;
      },
      error: (e: ApiError) => {
        this.cargandoDisponibilidad = false;
        if (e.errores) {
          this.erroresBackend = e.errores;
          this.mensajeError = e.errores['ofertaId']
            || e.errores['fechaEntrada']
            || e.errores['fechaSalida']
            || e.mensaje
            || 'No se pudo buscar disponibilidad.';
        } else {
          this.mensajeError = e.mensaje || 'No se pudo buscar disponibilidad.';
        }
      },
    });
  }

  // ==================== PASO 3: HABITACION ====================

  seleccionarHabitacion(h: Habitacion): void {
    this.habitacionSeleccionada = h;
  }

  avanzarHabitacion(): void {
    if (!this.habitacionSeleccionada) return;
    this.limpiarErrores();
    this.paso = 4;
  }

  // ==================== PASO 4: SERVICIOS ====================

  estaEnBundle(id: string): boolean {
    return (this.ofertaSeleccionada?.serviciosIncluidos ?? []).includes(id);
  }

  estaSeleccionado(id: string): boolean {
    return this.serviciosSeleccionados.has(id);
  }

  toggleServicio(id: string): void {
    if (this.estaEnBundle(id)) return;
    if (this.serviciosSeleccionados.has(id)) {
      this.serviciosSeleccionados.delete(id);
    } else {
      this.serviciosSeleccionados.add(id);
    }
  }

  avanzarServicios(): void {
    this.limpiarErrores();
    this.desglose = null;
    this.paso = 5;
  }

  // ==================== PASO 5: COTIZAR Y CONFIRMAR ====================

  cotizar(): void {
    if (this.emailForm.invalid) {
      this.emailForm.markAllAsTouched();
      return;
    }
    this.limpiarErrores();

    let payload: CotizarPayload;
    try {
      payload = this.construirPayload();
    } catch {
      this.mensajeError = 'Error al preparar la cotizacion.';
      return;
    }

    this.cotizando = true;
    this.api.cotizar(payload).subscribe({
      next: (d) => {
        this.cotizando = false;
        this.desglose = d;
      },
      error: (e: ApiError) => {
        this.cotizando = false;
        if (e.errores) {
          this.erroresBackend = e.errores;
          this.mensajeError = e.errores['ofertaId']
            || e.errores['habitacionId']
            || e.errores['servicios']
            || e.mensaje
            || 'No se pudo calcular la cotizacion.';
        } else {
          this.mensajeError = e.mensaje || 'No se pudo calcular la cotizacion.';
        }
      },
    });
  }

  confirmar(): void {
    this.limpiarErrores();
    const { emailContacto } = this.emailForm.getRawValue();

    const payload: CrearReservaPayload = { ...this.construirPayload(), emailContacto };

    this.confirmando = true;
    this.api.crearReserva(payload).subscribe({
      next: (reserva) => {
        this.confirmando = false;
        this.reservaCreada = reserva;
        this.paso = 6;
        if (this.vista === 'crear') {
          setTimeout(() => this.irAMisReservas(), 3000);
        }
      },
      error: (e: ApiError) => {
        this.confirmando = false;
        if (e.errores) {
          this.erroresBackend = e.errores;
          // La habitacion ya no esta disponible: volver a elegir.
          if (e.errores['disponibilidad']) {
            this.mensajeError = e.errores['disponibilidad'];
            this.habitacionSeleccionada = null;
            this.desglose = null;
            this.paso = 3;
            this.recargarDisponibilidad();
          }
        } else {
          this.mensajeError = e.mensaje || 'No se pudo confirmar la reserva.';
        }
      },
    });
  }

  // ==================== CICLO DE VIDA ====================

  ngOnDestroy(): void {
    this.destruir$.next();
    this.destruir$.complete();
  }

  // ==================== LISTADO ADMIN ====================

  cargarReservas(): void {
    this.cargandoReservas = true;
    this.errorCarga = '';
    this.api.consultarReservas().subscribe({
      next: (reservas) => {
        this.reservas = reservas;
        this.api.consultarTodosPerfiles().subscribe({
          next: (perfiles) => {
            this.perfiles = perfiles;
            this.api.consultarTodasHabitaciones().subscribe({
              next: (habitaciones) => {
                this.habitaciones = habitaciones;
                this.api.consultarTodosServicios().subscribe({
                  next: (servicios) => {
                    this.todosLosServicios = servicios;
                    this.api.consultarTodasOfertas().subscribe({
                      next: (ofertas) => { this.todasLasOfertas = ofertas; this.cargandoReservas = false; },
                      error: () => { this.cargandoReservas = false; },
                    });
                  },
                  error: () => { this.cargandoReservas = false; },
                });
              },
              error: () => { this.cargandoReservas = false; },
            });
          },
          error: () => { this.cargandoReservas = false; },
        });
      },
      error: () => { this.errorCarga = 'Error al cargar las reservas.'; this.cargandoReservas = false; },
    });
  }

  cargarMisReservas(): void {
    const perfilId = this.sesion.usuario()?.id;
    if (!perfilId) return;
    this.cargandoMisReservas = true;
    this.errorMisReservas = '';
    this.api.consultarReservas().subscribe({
      next: (todas) => {
        this.misReservas = todas.filter(r => r.perfilId === perfilId);
        this.api.consultarTodosServicios().subscribe({
          next: (s) => { this.todosLosServicios = s; },
          error: () => {},
        });
        this.api.consultarTodasOfertas().subscribe({
          next: (o) => { this.todasLasOfertas = o; },
          error: () => {},
        });
        this.api.consultarTodasHabitaciones().subscribe({
          next: (h) => { this.habitaciones = h; this.cargandoMisReservas = false; },
          error: () => { this.cargandoMisReservas = false; },
        });
      },
      error: () => {
        this.errorMisReservas = 'No se pudieron cargar tus reservas.';
        this.cargandoMisReservas = false;
      },
    });
  }

  get misReservasFiltradas(): Reserva[] {
    if (!this.filtroEstadoCliente) return this.misReservas;
    return this.misReservas.filter(r => r.estado === this.filtroEstadoCliente);
  }

  get reservasFiltradas(): Reserva[] {
    return this.reservas.filter(r => {
      const porEstado = !this.filtroEstado || r.estado === this.filtroEstado;
      const busqueda = this.filtroBusqueda.toLowerCase();
      const nombreH = this.nombreHuesped(r.perfilId).toLowerCase();
      const emailC = (r.emailContacto ?? '').toLowerCase();
      const numHab = String(this.numeroHabitacion(r.habitacionId));
      const idCorto = r.id.slice(0, 8).toUpperCase();
      const porTexto = !busqueda ||
        nombreH.includes(busqueda) ||
        emailC.includes(busqueda) ||
        numHab.includes(busqueda) ||
        idCorto.toLowerCase().includes(busqueda);
      return porEstado && porTexto;
    });
  }

  get totalActivas(): number { return this.reservas.filter(r => r.estado === 'ACTIVA').length; }
  get totalCanceladas(): number { return this.reservas.filter(r => r.estado === 'CANCELADA').length; }

  irAListaReservas(): void {
    this.destruir$.next();
    this.vista = 'lista';
    this.cargarReservas();
  }

  verDetalleReserva(r: Reserva): void {
    this.reservaSeleccionada = r;
    this.vista = 'detalle';
    if (this.todosLosServicios.length === 0) {
      this.api.consultarTodosServicios().subscribe({
        next: (s) => { this.todosLosServicios = s; }, error: () => {},
      });
    }
    if (this.todasLasOfertas.length === 0) {
      this.api.consultarTodasOfertas().subscribe({
        next: (o) => { this.todasLasOfertas = o; }, error: () => {},
      });
    }
  }

  badgeEstadoClase(estado: string): string {
    const clases: Record<string, string> = {
      'ACTIVA': 'badge-activa',
      'MODIFICADA': 'badge-modificada',
      'CANCELADA': 'badge-cancelada',
    };
    return clases[estado] || '';
  }

  badgeEstadoTexto(estado: string): string {
    const textos: Record<string, string> = {
      'ACTIVA': 'Activa',
      'MODIFICADA': 'Modificada',
      'CANCELADA': 'Cancelada',
    };
    return textos[estado] || estado;
  }

  // ---- RESOLUCIÓN DE IDs ----

  nombreHuesped(perfilId: string): string {
    return this.perfiles.find(p => p.id === perfilId)?.nombre ?? perfilId.slice(0, 8) + '...';
  }

  numeroHabitacion(habitacionId: string): string {
    const hab = this.habitaciones.find(h => h.id === habitacionId);
    return hab ? String(hab.numero) : habitacionId.slice(0, 8) + '...';
  }

  tipoHabitacion(habitacionId: string): string {
    return this.habitaciones.find(h => h.id === habitacionId)?.tipo ?? '';
  }

  nombreServicio(servicioId: string): string {
    return this.todosLosServicios.find(s => s.id === servicioId)?.nombre ?? servicioId.slice(0, 8) + '...';
  }

  nombreOferta(ofertaId: string | null | undefined): string {
    if (!ofertaId) return '';
    return this.todasLasOfertas.find(o => o.id === ofertaId)?.nombre ?? ofertaId.slice(0, 8) + '...';
  }

  calcularNoches(fechaEntrada: string, fechaSalida: string): number {
    const entrada = new Date(fechaEntrada).getTime();
    const salida = new Date(fechaSalida).getTime();
    return Math.round((salida - entrada) / 86400000);
  }

  // ==================== CREAR RESERVA ADMIN ====================

  buscarHuesped(): void {
    if (!this.emailBusqueda.trim()) return;
    this.buscandoHuesped = true;
    this.errorBusqueda = '';
    this.huespedEncontrado = null;
    this.api.buscarPerfilPorEmail(this.emailBusqueda.trim()).subscribe({
      next: (perfil) => {
        this.buscandoHuesped = false;
        this.huespedEncontrado = perfil;
      },
      error: () => {
        this.buscandoHuesped = false;
        this.errorBusqueda = 'No se encontró ningún huésped con ese email.';
      },
    });
  }

  iniciarCreacionAdmin(): void {
    if (!this.huespedEncontrado) return;
    this.perfilIdAdmin = this.huespedEncontrado.id;
    this.emailForm.controls.emailContacto.setValue(this.huespedEncontrado.email);
    this.paso = 1;
  }

  get enFlujoCreacion(): boolean {
    return this.vista === 'crear' ||
      (this.vista === 'crearAdmin' && this.perfilIdAdmin !== null);
  }

  // ==================== NAVEGACION ====================

  irAMisReservas(): void {
    this.destruir$.next();
    this.reservaDetalleCliente = null;
    this.vista = 'misReservas';
    this.cargarMisReservas();
  }

  irACrearReserva(): void {
    this.limpiarErrores();
    this.paso = 1;
    this.ofertaSeleccionada = null;
    this.habitacionSeleccionada = null;
    this.serviciosSeleccionados = new Set();
    this.desglose = null;
    this.reservaCreada = null;
    this.cargarDatosCreacion();
    this.vista = 'crear';
  }

  verDetalleCliente(r: Reserva): void {
    this.reservaDetalleCliente = r;
    if (this.todosLosServicios.length === 0) {
      this.api.consultarTodosServicios().subscribe({
        next: (s) => { this.todosLosServicios = s; }, error: () => {},
      });
    }
    if (this.todasLasOfertas.length === 0) {
      this.api.consultarTodasOfertas().subscribe({
        next: (o) => { this.todasLasOfertas = o; }, error: () => {},
      });
    }
    this.vista = 'detalleCliente';
  }

  retroceder(): void {
    this.limpiarErrores();
    if (this.paso === 5) {
      this.desglose = null;
    }
    this.paso = Math.max(1, this.paso - 1);
  }

  nuevaReserva(): void {
    this.paso = 1;
    this.fechasForm.reset();
    this.ofertaSeleccionada = null;
    this.habitacionSeleccionada = null;
    this.habitacionesDisponibles = [];
    this.serviciosSeleccionados = new Set();
    this.emailForm.reset();
    this.desglose = null;
    this.reservaCreada = null;
    this.limpiarErrores();
    this.huespedEncontrado = null;
    this.perfilIdAdmin = null;
    this.emailBusqueda = '';
    this.errorBusqueda = '';
    const email = this.sesion.usuario()?.email;
    if (email) {
      this.emailForm.controls.emailContacto.setValue(email);
    }
  }

  // ==================== HELPERS PRIVADOS ====================

  private construirPayload(): CotizarPayload {
    const { fechaEntrada, fechaSalida } = this.fechasForm.getRawValue();
    const perfilId = this.perfilIdAdmin ?? this.sesion.usuario()!.id;
    return {
      perfilId,
      habitacionId: this.habitacionSeleccionada!.id,
      fechaEntrada,
      fechaSalida,
      serviciosIds: [
        ...(this.ofertaSeleccionada?.serviciosIncluidos ?? []),
        ...this.serviciosSeleccionados,
      ],
      ofertaId: this.ofertaSeleccionada?.id ?? null,
    };
  }

  private recargarDisponibilidad(): void {
    const { fechaEntrada, fechaSalida, capacidad } = this.fechasForm.getRawValue();
    this.cargandoDisponibilidad = true;
    this.habitacionesDisponibles = [];
    this.api
      .consultarDisponibilidad(
        fechaEntrada,
        fechaSalida,
        this.ofertaSeleccionada?.tipoHabitacion,
        capacidad ?? undefined,
        this.ofertaSeleccionada?.id,
      )
      .subscribe({
        next: (h) => {
          this.cargandoDisponibilidad = false;
          this.habitacionesDisponibles = h;
        },
        error: () => {
          this.cargandoDisponibilidad = false;
        },
      });
  }

  private limpiarErrores(): void {
    this.erroresBackend = {};
    this.mensajeError = '';
  }
}
