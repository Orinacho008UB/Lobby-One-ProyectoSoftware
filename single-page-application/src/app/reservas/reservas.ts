import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';

import {
  ApiClient,
  ApiError,
  CotizarPayload,
  CrearReservaPayload,
  DesgloseReserva,
  Habitacion,
  Oferta,
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
export class Reservas implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(ApiClient);
  private readonly sesion = inject(SessionManager);

  protected paso = 1;

  // --- Paso 1: Fechas ---
  protected readonly hoy = new Date().toISOString().split('T')[0];
  protected readonly fechasForm = this.fb.group({
    fechaEntrada: this.fb.nonNullable.control('', Validators.required),
    fechaSalida: this.fb.nonNullable.control('', Validators.required),
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

  ngOnInit(): void {
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
    const { fechaEntrada, fechaSalida } = this.fechasForm.getRawValue();
    this.cargandoDisponibilidad = true;
    this.habitacionSeleccionada = null;
    this.habitacionesDisponibles = [];

    this.api.consultarDisponibilidad(
      fechaEntrada,
      fechaSalida,
      this.ofertaSeleccionada?.tipoHabitacion,
      undefined,
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

  // ==================== NAVEGACION ====================

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
    const email = this.sesion.usuario()?.email;
    if (email) {
      this.emailForm.controls.emailContacto.setValue(email);
    }
  }

  // ==================== HELPERS PRIVADOS ====================

  private construirPayload(): CotizarPayload {
    const { fechaEntrada, fechaSalida } = this.fechasForm.getRawValue();
    return {
      perfilId: this.sesion.usuario()!.id,
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
    const { fechaEntrada, fechaSalida } = this.fechasForm.getRawValue();
    this.cargandoDisponibilidad = true;
    this.habitacionesDisponibles = [];
    this.api
      .consultarDisponibilidad(
        fechaEntrada,
        fechaSalida,
        this.ofertaSeleccionada?.tipoHabitacion,
        undefined,
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
