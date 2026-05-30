import { Component, DestroyRef, ElementRef, inject, OnInit, ViewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AbstractControl, FormArray, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { BehaviorSubject, combineLatest, map, Observable } from 'rxjs';

import {
  ApiClient,
  ApiError,
  EstadoServicio,
  Horario,
  NuevoServicio,
  Servicio,
} from '../api-client/api-client';

@Component({
  selector: 'app-servicios',
  standalone: false,
  templateUrl: './servicios.html',
  styleUrl: './servicios.css',
})
export class Servicios implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(ApiClient);

  protected readonly estados: EstadoServicio[] = ['ACTIVO', 'INACTIVO'];
  protected readonly dias: string[] = [
    'Lunes', 'Martes', 'Miercoles', 'Jueves', 'Viernes', 'Sabado', 'Domingo',
  ];

  protected erroresBackend: Record<string, string> = {};
  protected errorImagen = '';
  protected mensajeError = '';
  protected mensajeExito = '';
  protected enviando = false;

  private imagen: File | null = null;

  @ViewChild('archivoInput') private archivoInput?: ElementRef<HTMLInputElement>;

  // --- Navegación de vistas ---
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  protected vista: 'lista' | 'formulario' | 'detalle' = 'lista';

  // --- Listado (reactivo): la fuente de verdad vive en el ApiClient) ---
  protected cargando = false;
  protected errorCarga = '';

  // Filtros como stream para recomponer la lista filtrada de forma reactiva.
  private readonly filtros$ = new BehaviorSubject<{ estado: string; categoria: string }>({
    estado: '',
    categoria: '',
  });

  get filtroEstado(): string { return this.filtros$.value.estado; }
  set filtroEstado(estado: string) { this.filtros$.next({ ...this.filtros$.value, estado }); }
  get filtroCategoria(): string { return this.filtros$.value.categoria; }
  set filtroCategoria(categoria: string) { this.filtros$.next({ ...this.filtros$.value, categoria }); }

  /** Catalogo crudo desde el cache compartido (0 ms al re-entrar). */
  protected readonly servicios$ = this.api.servicios$;

  /** Lista visible = catalogo filtrado en cliente, reactivo. */
  protected readonly serviciosFiltrados$: Observable<Servicio[]> = combineLatest([
    this.api.servicios$,
    this.filtros$,
  ]).pipe(
    map(([servicios, f]) =>
      servicios.filter(
        (s) => (!f.estado || s.estado === f.estado) && (!f.categoria || s.categoria === f.categoria),
      ),
    ),
  );

  /** Resumen (KPIs) derivado del mismo catalogo. */
  protected readonly resumen$ = this.api.servicios$.pipe(
    map((servicios) => ({
      total: servicios.length,
      activos: servicios.filter((s) => s.estado === 'ACTIVO').length,
      inactivos: servicios.filter((s) => s.estado === 'INACTIVO').length,
    })),
  );

  // --- Detalle ---
  protected servicioSeleccionado: Servicio | null = null;

  // --- Opciones de categoría (usadas en filtros Y en el formulario) ---
  protected readonly categorias: string[] = [
    'Gastronomia',
    'Bienestar y Spa',
    'Transporte',
    'Tours y Actividades',
    'Otros',
  ];

  // FormArray declarado antes que form para referenciarlo en el group.
  protected readonly horarios: FormArray<FormGroup> = this.fb.array<FormGroup>([
    this.crearHorario(),
  ]);

  protected readonly form = this.fb.group({
    nombre: this.fb.nonNullable.control('', Validators.required),
    categoria: this.fb.nonNullable.control('', Validators.required),
    precio: this.fb.control<number | null>(null, [Validators.required, Validators.min(0.01)]),
    unidadCobro: this.fb.nonNullable.control('', Validators.required),
    descripcionCorta: this.fb.nonNullable.control('', Validators.required),
    estado: this.fb.control<EstadoServicio | null>(null, Validators.required),
    descripcionDetallada: this.fb.nonNullable.control(''),
    ubicacion: this.fb.nonNullable.control(''),
    capacidadMaxima: this.fb.control<number | null>(null),
    requisitosRestricciones: this.fb.nonNullable.control(''),
    disponibilidadHorarios: this.horarios,
  });

  ngOnInit(): void {
    this.route.data
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((data) => {
        const vistaRuta = data['vista'] as string;
        this.vista = vistaRuta === 'formulario' ? 'formulario' : 'lista';
        if (this.vista === 'lista') {
          this.cargarServicios();
        }
      });
  }

  cargarServicios(): void {
    // Spinner solo si el cache aun esta vacio; si ya hay datos, la lista se ve
    // al instante y la recarga ocurre en segundo plano.
    this.cargando = this.api.serviciosActuales.length === 0;
    this.errorCarga = '';
    this.api.refrescarServicios()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => { this.cargando = false; },
        error: () => { this.errorCarga = 'Error al cargar los servicios.'; this.cargando = false; },
      });
  }

  irAFormulario(): void {
    this.limpiarMensajes();
    this.vista = 'formulario';
  }

  irALista(): void {
    this.vista = 'lista';
    this.cargarServicios();
  }

  verDetalle(s: Servicio): void {
    this.servicioSeleccionado = s;
    this.vista = 'detalle';
  }

  toggleEstado(s: Servicio, event: Event): void {
    event.stopPropagation();
    const nuevoEstado: EstadoServicio = s.estado === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO';
    this.api.cambiarEstadoServicio(s.id, nuevoEstado).subscribe({
      next: (actualizado) => {
        // Actualiza el cache compartido: la lista (async) se refresca sola.
        this.api.actualizarServicioEnCache(actualizado);
        if (this.servicioSeleccionado?.id === actualizado.id) {
          this.servicioSeleccionado = actualizado;
        }
      },
      error: () => { this.mensajeError = 'No se pudo cambiar el estado del servicio.'; },
    });
  }

  badgeClaseEstado(estado: string): string {
    return estado === 'ACTIVO' ? 'badge-activo' : 'badge-inactivo';
  }

  badgeTextoEstado(estado: string): string {
    return estado === 'ACTIVO' ? '✓ Activo' : 'Inactivo';
  }

  /** Devuelve el control de un campo dentro del horario en el indice dado. */
  protected horarioControl(i: number, campo: string): AbstractControl | null {
    return this.horarios.at(i).get(campo);
  }

  private crearHorario(): FormGroup {
    return this.fb.group({
      dia: this.fb.nonNullable.control('', Validators.required),
      horaInicio: this.fb.nonNullable.control('', Validators.required),
      horaFin: this.fb.nonNullable.control('', Validators.required),
    });
  }

  agregarHorario(): void {
    this.horarios.push(this.crearHorario());
  }

  quitarHorario(i: number): void {
    if (this.horarios.length > 1) {
      this.horarios.removeAt(i);
    }
  }

  onArchivo(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.imagen = input.files?.[0] ?? null;
    this.errorImagen = '';
  }

  crear(): void {
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
    const servicio: NuevoServicio = {
      nombre: v.nombre,
      categoria: v.categoria,
      precio: v.precio!,
      unidadCobro: v.unidadCobro,
      descripcionCorta: v.descripcionCorta,
      estado: v.estado!,
      disponibilidadHorarios: this.horarios.getRawValue() as Horario[],
      descripcionDetallada: v.descripcionDetallada.trim() || undefined,
      ubicacion: v.ubicacion.trim() || undefined,
      capacidadMaxima: v.capacidadMaxima ?? undefined,
      requisitosRestricciones: v.requisitosRestricciones.trim() || undefined,
    };

    this.enviando = true;
    this.api.crearServicio(servicio, this.imagen).subscribe({
      next: (creado) => {
        this.enviando = false;
        this.mensajeExito = `Servicio "${creado.nombre}" creado correctamente.`;
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
          this.mensajeError = e.mensaje || 'No se pudo crear el servicio.';
        }
      },
    });
  }

  private reiniciar(): void {
    this.form.reset();
    while (this.horarios.length > 1) {
      this.horarios.removeAt(1);
    }
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
