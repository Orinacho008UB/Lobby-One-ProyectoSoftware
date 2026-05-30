import { Component, DestroyRef, ElementRef, inject, OnInit, ViewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { BehaviorSubject, combineLatest, map, Observable } from 'rxjs';

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
export class Habitaciones implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(ApiClient);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  private timeoutVolver: ReturnType<typeof setTimeout> | null = null;

  protected readonly tipos: TipoHabitacion[] = ['INDIVIDUAL', 'DOBLE', 'SUITE'];
  protected readonly estados: EstadoHabitacion[] = [
    'DISPONIBLE',
    'MANTENIMIENTO',
    'FUERA_DE_SERVICIO',
    'OCUPADA',
  ];

  // Vista activa
  protected vista: 'lista' | 'formulario' | 'detalle' = 'lista';

  // Listado (reactivo): la fuente de verdad vive en el ApiClient, no aqui.
  protected cargando = false;
  protected errorCarga = '';

  // Filtros como stream: ngModel escribe via getter/setter y empuja al subject,
  // de modo que la lista filtrada se recompone sola.
  private readonly filtros$ = new BehaviorSubject<{ estado: string; tipo: string }>({
    estado: '',
    tipo: '',
  });

  get filtroEstado(): string { return this.filtros$.value.estado; }
  set filtroEstado(estado: string) { this.filtros$.next({ ...this.filtros$.value, estado }); }
  get filtroTipo(): string { return this.filtros$.value.tipo; }
  set filtroTipo(tipo: string) { this.filtros$.next({ ...this.filtros$.value, tipo }); }

  /** Catalogo crudo desde el cache compartido (se entrega en 0 ms al re-entrar). */
  protected readonly habitaciones$ = this.api.habitaciones$;

  /** Lista visible = catalogo filtrado en cliente, de forma reactiva. */
  protected readonly habitacionesFiltradas$: Observable<Habitacion[]> = combineLatest([
    this.api.habitaciones$,
    this.filtros$,
  ]).pipe(
    map(([habitaciones, f]) =>
      habitaciones.filter(
        (h) => (!f.estado || h.estado === f.estado) && (!f.tipo || h.tipo === f.tipo),
      ),
    ),
  );

  /** Resumen de metricas, derivado del mismo catalogo compartido. */
  protected readonly resumen$ = this.api.habitaciones$.pipe(
    map((habitaciones) => ({
      total: habitaciones.length,
      disponibles: habitaciones.filter((h) => h.estado === 'DISPONIBLE').length,
      ocupadas: habitaciones.filter((h) => h.estado === 'OCUPADA').length,
      mantenimiento: habitaciones.filter((h) => h.estado === 'MANTENIMIENTO').length,
      fueraServicio: habitaciones.filter((h) => h.estado === 'FUERA_DE_SERVICIO').length,
    })),
  );

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
    tamanoM2: this.fb.control<number | null>(null, [Validators.required, Validators.min(1), Validators.pattern(/^[0-9]+$/)]),
    precioPorNoche: this.fb.control<number | null>(null, [Validators.required, Validators.min(0.01)]),
    configuracionCamas: this.fb.nonNullable.control('', Validators.required),
    estado: this.fb.control<EstadoHabitacion | null>(null, Validators.required),
    amenidades: this.fb.nonNullable.control(''),
    descripcion: this.fb.nonNullable.control(''),
  });

  ngOnInit(): void {
    this.route.data
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((data) => {
        const vistaRuta = data['vista'] as string;
        this.vista = vistaRuta === 'formulario' ? 'formulario' : 'lista';
        if (this.vista === 'lista') {
          this.cargarHabitaciones();
        }
      });
  }

  cargarHabitaciones(): void {
    // Solo mostramos spinner si el cache aun esta vacio: si ya hay datos
    // cacheados, la lista se ve al instante y la recarga ocurre en segundo plano.
    this.cargando = this.api.habitacionesActuales.length === 0;
    this.errorCarga = '';
    this.api.refrescarHabitaciones()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => { this.cargando = false; },
        error: () => { this.errorCarga = 'Error al cargar las habitaciones.'; this.cargando = false; }
      });
  }

  irAFormulario(): void {
    this.vista = 'formulario';
    this.limpiarMensajes();
  }

  irALista(): void {
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
        this.timeoutVolver = setTimeout(() => { this.irALista(); }, 2000);
        this.destroyRef.onDestroy(() => { if (this.timeoutVolver) clearTimeout(this.timeoutVolver); });
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
