import { Component, ElementRef, inject, OnInit, ViewChild } from '@angular/core';
import { AbstractControl, FormArray, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

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
  protected vista: 'lista' | 'formulario' | 'detalle' = 'lista';

  // --- Listado ---
  protected servicios: Servicio[] = [];
  protected cargando = false;
  protected errorCarga = '';
  protected filtroEstado = '';
  protected filtroCategoria = '';

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
    const vistaRuta = this.route.snapshot.data['vista'] as string;
    this.vista = vistaRuta === 'formulario' ? 'formulario' : 'lista';
    if (this.vista === 'lista') {
      this.cargarServicios();
    }
  }

  get serviciosFiltrados(): Servicio[] {
    return this.servicios.filter(s => {
      const porEstado = !this.filtroEstado || s.estado === this.filtroEstado;
      const porCategoria = !this.filtroCategoria || s.categoria === this.filtroCategoria;
      return porEstado && porCategoria;
    });
  }

  get totalServicios(): number { return this.servicios.length; }
  get totalActivos(): number { return this.servicios.filter(s => s.estado === 'ACTIVO').length; }
  get totalInactivos(): number { return this.servicios.filter(s => s.estado === 'INACTIVO').length; }

  cargarServicios(): void {
    this.cargando = true;
    this.errorCarga = '';
    this.api.consultarTodosServicios().subscribe({
      next: (data) => { this.servicios = data; this.cargando = false; },
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
        const idx = this.servicios.findIndex(x => x.id === actualizado.id);
        if (idx !== -1) { this.servicios[idx] = actualizado; }
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
