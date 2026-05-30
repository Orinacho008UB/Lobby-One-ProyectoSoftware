import { Component, ElementRef, inject, ViewChild } from '@angular/core';
import { AbstractControl, FormArray, FormBuilder, FormGroup, Validators } from '@angular/forms';

import {
  ApiClient,
  ApiError,
  EstadoServicio,
  Horario,
  NuevoServicio,
} from '../api-client/api-client';

/**
 * Componente Servicios (C4): formulario de creacion de servicio (solo admin).
 * Reactive Forms + API Client; sube la imagen de portada por multipart.
 * La lista de horarios es un FormArray dinamico (agregar/quitar filas).
 */
@Component({
  selector: 'app-servicios',
  standalone: false,
  templateUrl: './servicios.html',
  styleUrl: './servicios.css',
})
export class Servicios {
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
