import { Component, ElementRef, inject, OnInit, signal, ViewChild } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import {
  ApiClient,
  ApiError,
  EstadoOferta,
  NuevaOferta,
  Oferta,
  Servicio,
  TipoHabitacion,
} from '../api-client/api-client';

type Vista = 'lista' | 'formulario' | 'detalle';

/**
 * Componente Ofertas (C4): catálogo de ofertas, formulario de creación y detalle (admin).
 * Usa Signal<Vista> y ActivatedRoute para alternancia de vistas.
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
  private readonly route = inject(ActivatedRoute);

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

  // --- Navegación entre vistas ---
  protected readonly vista = signal<Vista>('formulario');

  // --- Listado ---
  protected ofertas: Oferta[] = [];
  protected todosLosServicios: Servicio[] = [];
  protected cargando = false;
  protected errorCarga = '';
  protected filtroBusqueda = '';
  protected filtroEstado = '';
  protected filtroTipo = '';

  // --- Detalle ---
  protected ofertaSeleccionada: Oferta | null = null;
  protected cambiandoEstado = false;

  ngOnInit(): void {
    this.route.data.subscribe((data) => {
      const vistaRuta = data['vista'] as Vista | undefined;
      if (vistaRuta === 'lista') {
        this.vista.set('lista');
        this.cargarListado();
      } else {
        // 'formulario' o sin data.vista (compatibilidad)
        this.vista.set('formulario');
        // Cargar servicios activos para el formulario (lógica existente)
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
    });
  }

  // ---- LISTADO ----

  cargarListado(): void {
    this.cargando = true;
    this.errorCarga = '';
    this.api.consultarTodasOfertas().subscribe({
      next: (ofertas) => {
        this.ofertas = ofertas;
        this.api.consultarTodosServicios().subscribe({
          next: (servicios) => {
            this.todosLosServicios = servicios;
            this.cargando = false;
          },
          error: () => { this.todosLosServicios = []; this.cargando = false; },
        });
      },
      error: () => { this.errorCarga = 'Error al cargar las ofertas.'; this.cargando = false; },
    });
  }

  get ofertasFiltradas(): Oferta[] {
    return this.ofertas.filter(o => {
      const busqueda = this.filtroBusqueda.toLowerCase();
      const coincide = !busqueda ||
        o.nombre?.toLowerCase().includes(busqueda) ||
        o.descripcion?.toLowerCase().includes(busqueda);
      const porTipo = !this.filtroTipo || o.tipoHabitacion === this.filtroTipo;
      let porEstado = true;
      if (this.filtroEstado === 'ACTIVA') {
        porEstado = o.estado === 'ACTIVA' && !this.esCaducada(o);
      } else if (this.filtroEstado === 'INACTIVA') {
        porEstado = o.estado === 'INACTIVA';
      } else if (this.filtroEstado === 'CADUCADA') {
        porEstado = this.esCaducada(o);
      }
      return coincide && porTipo && porEstado;
    });
  }

  get totalOfertas(): number { return this.ofertas.length; }
  get totalActivas(): number {
    return this.ofertas.filter(o => o.estado === 'ACTIVA' && !this.esCaducada(o)).length;
  }
  get totalCaducadas(): number { return this.ofertas.filter(o => this.esCaducada(o)).length; }

  // ---- ESTADO Y BADGE ----

  esCaducada(oferta: Oferta): boolean {
    if (oferta.estado !== 'ACTIVA') return false;
    const hoy = new Date().toISOString().split('T')[0];
    return oferta.vigenciaHasta < hoy;
  }

  badgeTextoEstado(oferta: Oferta): string {
    if (this.esCaducada(oferta)) return 'CADUCADA';
    return oferta.estado;
  }

  badgeClaseEstado(oferta: Oferta): string {
    if (this.esCaducada(oferta)) return 'badge-caducada';
    return oferta.estado === 'ACTIVA' ? 'badge-activa' : 'badge-inactiva';
  }

  // ---- TOGGLE ESTADO ----

  toggleEstado(oferta: Oferta): void {
    const nuevoEstado: EstadoOferta = oferta.estado === 'ACTIVA' ? 'INACTIVA' : 'ACTIVA';
    this.cambiandoEstado = true;
    this.api.cambiarEstadoOferta(oferta.id, nuevoEstado).subscribe({
      next: (actualizada) => {
        this.cambiandoEstado = false;
        const idx = this.ofertas.findIndex(o => o.id === actualizada.id);
        if (idx !== -1) { this.ofertas[idx] = actualizada; }
        if (this.ofertaSeleccionada?.id === actualizada.id) {
          this.ofertaSeleccionada = actualizada;
        }
      },
      error: () => { this.cambiandoEstado = false; },
    });
  }

  // ---- NAVEGACIÓN ----

  irAFormulario(): void {
    this.vista.set('formulario');
    if (this.serviciosActivos.length === 0) {
      this.cargandoServicios = true;
      this.api.consultarServiciosActivos().subscribe({
        next: (s) => { this.serviciosActivos = s; this.cargandoServicios = false; },
        error: () => { this.cargandoServicios = false; },
      });
    }
  }

  irAListado(): void {
    this.ofertaSeleccionada = null;
    this.reiniciar();
    this.limpiarMensajes();
    this.vista.set('lista');
    this.cargarListado();
  }

  verDetalle(oferta: Oferta): void {
    this.ofertaSeleccionada = oferta;
    if (this.todosLosServicios.length === 0) {
      this.api.consultarTodosServicios().subscribe({
        next: (s) => { this.todosLosServicios = s; },
        error: () => {},
      });
    }
    this.vista.set('detalle');
  }

  // ---- RESOLUCIÓN DE IDs ----

  nombreServicio(id: string): string {
    return this.todosLosServicios.find(s => s.id === id)?.nombre ?? id;
  }

  precioServicio(id: string): number | null {
    return this.todosLosServicios.find(s => s.id === id)?.precio ?? null;
  }

  // ---- VIGENCIA ----

  porcentajeVigencia(oferta: Oferta): number {
    const desde = new Date(oferta.vigenciaDesde).getTime();
    const hasta = new Date(oferta.vigenciaHasta).getTime();
    const hoy = new Date().setHours(0, 0, 0, 0);
    if (hoy <= desde) return 0;
    if (hoy >= hasta) return 100;
    return Math.round(((hoy - desde) / (hasta - desde)) * 100);
  }

  diasRestantes(oferta: Oferta): number {
    const hoy = new Date().toISOString().split('T')[0];
    if (oferta.vigenciaHasta <= hoy) return 0;
    const hasta = new Date(oferta.vigenciaHasta).getTime();
    const ahoraMs = new Date().setHours(0, 0, 0, 0);
    return Math.ceil((hasta - ahoraMs) / 86400000);
  }

  // ---- FORMULARIO (métodos existentes) ----

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
        setTimeout(() => this.irAListado(), 2000);
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
