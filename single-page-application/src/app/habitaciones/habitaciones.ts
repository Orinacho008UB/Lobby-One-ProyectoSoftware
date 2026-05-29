import { Component, ElementRef, inject, ViewChild } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';

import {
  ApiClient,
  ApiError,
  EstadoHabitacion,
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
export class Habitaciones {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(ApiClient);

  protected readonly tipos: TipoHabitacion[] = ['INDIVIDUAL', 'DOBLE', 'SUITE'];
  protected readonly estados: EstadoHabitacion[] = [
    'DISPONIBLE',
    'MANTENIMIENTO',
    'FUERA_DE_SERVICIO',
    'OCUPADA',
  ];

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
    capacidad: this.fb.control<number | null>(null, [Validators.required, Validators.min(1)]),
    piso: this.fb.control<number | null>(null, Validators.required),
    tamanoM2: this.fb.control<number | null>(null, [Validators.required, Validators.min(0.01)]),
    precioPorNoche: this.fb.control<number | null>(null, [Validators.required, Validators.min(0.01)]),
    configuracionCamas: this.fb.nonNullable.control('', Validators.required),
    estado: this.fb.control<EstadoHabitacion | null>(null, Validators.required),
    amenidades: this.fb.nonNullable.control(''),
    descripcion: this.fb.nonNullable.control(''),
  });

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
