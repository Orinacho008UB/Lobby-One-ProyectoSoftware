import { Component, ElementRef, inject, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import {
  ApiClient,
  ApiError,
  EstadoHabitacion,
  Habitacion,
  NuevaHabitacion,
  TipoHabitacion,
} from '../api-client/api-client';

/**
 * Componente Habitaciones (C4): formulario de registro de habitacion (solo
 * admin). Reactive Forms + API Client; sube la imagen de portada por multipart.
 */
@Component({
  selector: 'app-habitaciones',
  standalone: false,
  templateUrl: './habitaciones.html',
  styleUrl: './habitaciones.css',
})
export class Habitaciones implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(ApiClient);
  private readonly route = inject(ActivatedRoute);
  private readonly destruir$ = new Subject<void>();

  protected readonly tipos: TipoHabitacion[] = ['INDIVIDUAL', 'DOBLE', 'SUITE'];
  protected readonly estados: EstadoHabitacion[] = [
    'DISPONIBLE',
    'MANTENIMIENTO',
    'FUERA_DE_SERVICIO',
    'OCUPADA',
  ];

  // Vista activa
  protected vista: 'lista' | 'formulario' | 'detalle' = 'lista';

  // Listado
  protected habitaciones: Habitacion[] = [];
  protected cargando = false;
  protected errorCarga = '';
  protected filtroEstado = '';
  protected filtroTipo = '';

  // Detalle
  protected habitacionSeleccionada: Habitacion | null = null;

  // Formulario
  protected erroresBackend: Record<string, string> = {};
  protected errorImagen = '';
  protected mensajeError = '';
  protected mensajeExito = '';
  protected enviando = false;

  /** Imagen de portada seleccionada (se envia como parte archivo, no por el form). */
  private imagen: File | null = null;

  @ViewChild('archivoInput') private archivoInput?: ElementRef<HTMLInputElement>;

  protected readonly form = this.fb.group({
    numero: this.fb.control<number | null>(null, [Validators.required, Validators.min(1)]),
    tipo: this.fb.control<TipoHabitacion | null>(null, Validators.required),
    capacidad: this.fb.control<number | null>(null, [Validators.required, Validators.min(1), Validators.pattern(/^[0-9]+$/)]),
    piso: this.fb.control<number | null>(null, Validators.required),
    tamanoM2: this.fb.control<number | null>(null, [Validators.required, Validators.min(0.01)]),
    precioPorNoche: this.fb.control<number | null>(null, [Validators.required, Validators.min(0.01)]),
    configuracionCamas: this.fb.nonNullable.control('', Validators.required),
    estado: this.fb.control<EstadoHabitacion | null>(null, Validators.required),
    amenidades: this.fb.nonNullable.control(''),
    descripcion: this.fb.nonNullable.control(''),
  });

  ngOnInit(): void {
    const vistaRuta = this.route.snapshot.data['vista'] as string;
    this.vista = vistaRuta === 'formulario' ? 'formulario' : 'lista';
    if (this.vista === 'lista') {
      this.cargarHabitaciones();
    }
  }

  get habitacionesFiltradas(): Habitacion[] {
    return this.habitaciones.filter(h => {
      const porEstado = !this.filtroEstado || h.estado === this.filtroEstado;
      const porTipo = !this.filtroTipo || h.tipo === this.filtroTipo;
      return porEstado && porTipo;
    });
  }

  get totalHabitaciones(): number { return this.habitaciones.length; }
  get totalDisponibles(): number { return this.habitaciones.filter(h => h.estado === 'DISPONIBLE').length; }
  get totalOcupadas(): number { return this.habitaciones.filter(h => h.estado === 'OCUPADA').length; }
  get totalMantenimiento(): number { return this.habitaciones.filter(h => h.estado === 'MANTENIMIENTO').length; }
  get totalFueraServicio(): number { return this.habitaciones.filter(h => h.estado === 'FUERA_DE_SERVICIO').length; }

  cargarHabitaciones(): void {
    this.cargando = true;
    this.errorCarga = '';
    this.api.consultarHabitaciones()
      .pipe(takeUntil(this.destruir$))
      .subscribe({
        next: (data) => { this.habitaciones = data; this.cargando = false; },
        error: () => { this.errorCarga = 'Error al cargar las habitaciones.'; this.cargando = false; }
      });
  }

  ngOnDestroy(): void {
    this.destruir$.next();
    this.destruir$.complete();
  }

  irAFormulario(): void {
    this.vista = 'formulario';
    this.limpiarMensajes();
  }

  irALista(): void {
    this.destruir$.next();
    this.vista = 'lista';
    this.cargarHabitaciones();
  }

  verDetalle(hab: Habitacion): void {
    this.habitacionSeleccionada = hab;
    this.vista = 'detalle';
  }

  badgeClase(estado: string): string {
    const clases: Record<string, string> = {
      'DISPONIBLE': 'badge-disponible',
      'OCUPADA': 'badge-ocupada',
      'MANTENIMIENTO': 'badge-mantenimiento',
      'FUERA_DE_SERVICIO': 'badge-fuera-servicio'
    };
    return clases[estado] || '';
  }

  badgeTexto(estado: string): string {
    const textos: Record<string, string> = {
      'DISPONIBLE': '✓ Disponible',
      'OCUPADA': 'Ocupada',
      'MANTENIMIENTO': 'Mantenimiento',
      'FUERA_DE_SERVICIO': 'Fuera de servicio'
    };
    return textos[estado] || estado;
  }

  onArchivo(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.imagen = input.files?.[0] ?? null;
    this.errorImagen = '';
  }

  registrar(): void {
    this.limpiarMensajes();

    if (this.form.invalid) {
      this.form.markAllAsTouched();
    }
    if (!this.imagen) {
      this.errorImagen = 'La imagen de portada es obligatoria.';
    }
    if (this.form.invalid || !this.imagen) {
      return;
    }

    const v = this.form.getRawValue();
    const habitacion: NuevaHabitacion = {
      numero: v.numero!,
      piso: v.piso!,
      tipo: v.tipo!,
      configuracionCamas: v.configuracionCamas,
      capacidad: v.capacidad!,
      tamanoM2: v.tamanoM2!,
      precioPorNoche: v.precioPorNoche!,
      estado: v.estado!,
      descripcion: v.descripcion.trim() || undefined,
      amenidades: this.parseLista(v.amenidades),
    };

    this.enviando = true;
    this.api.crearHabitacion(habitacion, this.imagen).subscribe({
      next: (creada) => {
        this.enviando = false;
        this.mensajeExito = `Habitacion #${creada.numero} registrada correctamente.`;
        this.reiniciar();
        setTimeout(() => { this.irALista(); }, 2000);
      },
      error: (e: ApiError) => {
        this.enviando = false;
        if (e.errores) {
          this.erroresBackend = e.errores;
          if (e.errores['imagenPortada']) {
            this.errorImagen = e.errores['imagenPortada'];
          }
        } else {
          this.mensajeError = e.mensaje || 'No se pudo registrar la habitacion.';
        }
      },
    });
  }

  /** Convierte "wifi, tv, balcon" en un arreglo; vacio -> undefined. */
  private parseLista(valor: string): string[] | undefined {
    const items = valor
      .split(',')
      .map((s) => s.trim())
      .filter((s) => s.length > 0);
    return items.length > 0 ? items : undefined;
  }

  private reiniciar(): void {
    this.form.reset();
    this.imagen = null;
    if (this.archivoInput) {
      this.archivoInput.nativeElement.value = '';
    }
  }

  private limpiarMensajes(): void {
    this.erroresBackend = {};
    this.errorImagen = '';
    this.mensajeError = '';
    this.mensajeExito = '';
  }
}
