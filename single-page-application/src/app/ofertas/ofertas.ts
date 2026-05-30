import { Component, DestroyRef, ElementRef, inject, OnInit, signal, ViewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { BehaviorSubject, combineLatest, map, Observable } from 'rxjs';

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
  private readonly destroyRef = inject(DestroyRef);

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

  // --- Listado (reactivo): la fuente de verdad vive en el ApiClient ---
  protected cargando = false;
  protected errorCarga = '';

  /** Catalogo de servicios (mirror del cache) para resolver nombres/precios en el detalle. */
  protected todosLosServicios: Servicio[] = [];

  // Filtros como stream para recomponer la lista filtrada de forma reactiva.
  private readonly filtros$ = new BehaviorSubject<{ busqueda: string; estado: string; tipo: string }>({
    busqueda: '',
    estado: '',
    tipo: '',
  });

  get filtroBusqueda(): string { return this.filtros$.value.busqueda; }
  set filtroBusqueda(busqueda: string) { this.filtros$.next({ ...this.filtros$.value, busqueda }); }
  get filtroEstado(): string { return this.filtros$.value.estado; }
  set filtroEstado(estado: string) { this.filtros$.next({ ...this.filtros$.value, estado }); }
  get filtroTipo(): string { return this.filtros$.value.tipo; }
  set filtroTipo(tipo: string) { this.filtros$.next({ ...this.filtros$.value, tipo }); }

  /** Lista visible = catalogo de ofertas filtrado en cliente, reactivo. */
  protected readonly ofertasFiltradas$: Observable<Oferta[]> = combineLatest([
    this.api.ofertas$,
    this.filtros$,
  ]).pipe(
    map(([ofertas, f]) =>
      ofertas.filter((o) => {
        const busqueda = f.busqueda.toLowerCase();
        const coincide =
          !busqueda ||
          o.nombre?.toLowerCase().includes(busqueda) ||
          o.descripcion?.toLowerCase().includes(busqueda);
        const porTipo = !f.tipo || o.tipoHabitacion === f.tipo;
        let porEstado = true;
        if (f.estado === 'ACTIVA') porEstado = o.estado === 'ACTIVA' && !this.esCaducada(o);
        else if (f.estado === 'INACTIVA') porEstado = o.estado === 'INACTIVA';
        else if (f.estado === 'CADUCADA') porEstado = this.esCaducada(o);
        return coincide && porTipo && porEstado;
      }),
    ),
  );

  /** Resumen (KPIs) derivado del mismo catalogo. */
  protected readonly resumen$ = this.api.ofertas$.pipe(
    map((ofertas) => ({
      total: ofertas.length,
      activas: ofertas.filter((o) => o.estado === 'ACTIVA' && !this.esCaducada(o)).length,
      caducadas: ofertas.filter((o) => this.esCaducada(o)).length,
    })),
  );

  // --- Detalle ---
  protected ofertaSeleccionada: Oferta | null = null;
  protected cambiandoEstado = false;

  ngOnInit(): void {
    // Mirror del cache de servicios: alimenta nombreServicio()/precioServicio()
    // del detalle y se reemite al instante al volver a la vista.
    this.api.servicios$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((servicios) => { this.todosLosServicios = servicios; });

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
    // Spinner solo si el cache aun esta vacio; con datos cacheados se ve al instante.
    this.cargando = this.api.ofertasActuales.length === 0;
    this.errorCarga = '';
    this.api.refrescarOfertas()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => { this.cargando = false; },
        error: () => { this.errorCarga = 'Error al cargar las ofertas.'; this.cargando = false; },
      });
    // Refresca en segundo plano el catalogo de servicios (para nombres del detalle).
    this.api.refrescarServicios()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: () => {}, error: () => {} });
  }

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
        // Actualiza el cache compartido: la lista (async) se refresca sola.
        this.api.actualizarOfertaEnCache(actualizada);
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
    if (this.api.serviciosActuales.length === 0) {
      // Llena el cache de servicios si aun no hay (p.ej. deep-link directo al detalle).
      this.api.refrescarServicios().subscribe({ next: () => {}, error: () => {} });
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
