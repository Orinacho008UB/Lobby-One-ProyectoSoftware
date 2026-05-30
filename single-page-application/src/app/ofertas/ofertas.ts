import { Component, ElementRef, inject, OnInit, ViewChild } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';

import {
  ApiClient,
  ApiError,
  EstadoOferta,
  NuevaOferta,
  Servicio,
  TipoHabitacion,
} from '../api-client/api-client';

/**
 * Componente Ofertas (C4): formulario de creacion de paquete (solo admin).
 * Carga los servicios activos en ngOnInit para el selector multiple.
 * La seleccion de servicios se gestiona fuera del FormGroup con un Set<string>.
 */
@Component({
  selector: 'app-ofertas',
  standalone: false,
  templateUrl: './ofertas.html',
  styleUrl: './ofertas.css',
})
export class Ofertas implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(ApiClient);

  protected readonly tipos: TipoHabitacion[] = ['INDIVIDUAL', 'DOBLE', 'SUITE'];
  protected readonly estados: EstadoOferta[] = ['ACTIVA', 'INACTIVA'];

  protected serviciosActivos: Servicio[] = [];
  protected cargandoServicios = true;

  protected serviciosSeleccionados = new Set<string>();
  protected errorServicios = '';

  protected erroresBackend: Record<string, string> = {};
  protected errorImagen = '';
  protected mensajeError = '';
  protected mensajeExito = '';
  protected enviando = false;

  // Limites para los date inputs (evita seleccion de fechas invalidas).
  protected readonly hoy = new Date().toISOString().split('T')[0];
  protected readonly manana = (() => {
    const d = new Date();
    d.setDate(d.getDate() + 1);
    return d.toISOString().split('T')[0];
  })();

  private imagen: File | null = null;

  @ViewChild('archivoInput') private archivoInput?: ElementRef<HTMLInputElement>;

  protected readonly form = this.fb.group({
    nombre: this.fb.nonNullable.control('', Validators.required),
    descripcion: this.fb.nonNullable.control('', Validators.required),
    tipoHabitacion: this.fb.control<TipoHabitacion | null>(null, Validators.required),
    precio: this.fb.control<number | null>(null, [Validators.required, Validators.min(0.01)]),
    vigenciaDesde: this.fb.nonNullable.control('', Validators.required),
    vigenciaHasta: this.fb.nonNullable.control('', Validators.required),
    estado: this.fb.control<EstadoOferta | null>(null, Validators.required),
  });

  ngOnInit(): void {
    this.api.consultarServiciosActivos().subscribe({
      next: (servicios) => {
        this.serviciosActivos = servicios;
        this.cargandoServicios = false;
      },
      error: () => {
        this.cargandoServicios = false;
        this.errorServicios = 'No se pudieron cargar los servicios activos.';
      },
    });
  }

  estaSeleccionado(id: string): boolean {
    return this.serviciosSeleccionados.has(id);
  }

  toggleServicio(id: string): void {
    if (this.serviciosSeleccionados.has(id)) {
      this.serviciosSeleccionados.delete(id);
    } else {
      this.serviciosSeleccionados.add(id);
    }
    this.errorServicios = '';
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
    if (this.serviciosSeleccionados.size === 0) {
      this.errorServicios = 'Debe seleccionar al menos un servicio.';
    }
    if (this.form.invalid || !this.imagen || this.serviciosSeleccionados.size === 0) {
      return;
    }

    const v = this.form.getRawValue();
    const oferta: NuevaOferta = {
      nombre: v.nombre,
      descripcion: v.descripcion,
      tipoHabitacion: v.tipoHabitacion!,
      precio: v.precio!,
      vigenciaDesde: v.vigenciaDesde,
      vigenciaHasta: v.vigenciaHasta,
      estado: v.estado!,
      serviciosIncluidos: [...this.serviciosSeleccionados],
    };

    this.enviando = true;
    this.api.crearOferta(oferta, this.imagen).subscribe({
      next: (creada) => {
        this.enviando = false;
        this.mensajeExito = `Oferta "${creada.nombre}" creada correctamente.`;
        this.reiniciar();
      },
      error: (e: ApiError) => {
        this.enviando = false;
        if (e.errores) {
          this.erroresBackend = e.errores;
          if (e.errores['imagenPortada']) {
            this.errorImagen = e.errores['imagenPortada'];
          }
          if (e.errores['serviciosIncluidos']) {
            this.errorServicios = e.errores['serviciosIncluidos'];
          }
        } else {
          this.mensajeError = e.mensaje || 'No se pudo crear la oferta.';
        }
      },
    });
  }

  private reiniciar(): void {
    this.form.reset();
    this.serviciosSeleccionados = new Set();
    this.imagen = null;
    if (this.archivoInput) {
      this.archivoInput.nativeElement.value = '';
    }
  }

  private limpiarMensajes(): void {
    this.erroresBackend = {};
    this.errorImagen = '';
    this.errorServicios = '';
    this.mensajeError = '';
    this.mensajeExito = '';
  }
}
