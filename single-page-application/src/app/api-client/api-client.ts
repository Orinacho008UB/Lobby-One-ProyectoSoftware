import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';

// ===========================================================================
// Tipos del dominio (coherentes con los modelos del backend Spring Boot).
// Los montos llegan como number (el backend serializa BigDecimal a numero JSON)
// y las fechas como string ISO ('YYYY-MM-DD' o timestamp ISO).
// ===========================================================================

export type Rol = 'CLIENTE' | 'ADMINISTRADOR';

export interface Perfil {
  id: string;
  nombre: string;
  email: string;
  telefono: string;
  cedula: string;
  /** El backend nunca lo expone en las respuestas (llega null). */
  contrasenaHash?: string | null;
  rol: Rol;
  notasInternas?: string;
}

/**
 * Cuerpo de registro. En el Sprint 1 la contrasena en texto plano viaja en el
 * campo {@code contrasenaHash} (el backend la valida y la hashea con Argon2).
 */
export interface RegistroPerfil {
  nombre: string;
  email: string;
  telefono: string;
  cedula: string;
  contrasenaHash: string;
}

export type TipoHabitacion = 'INDIVIDUAL' | 'DOBLE' | 'SUITE';
export type EstadoHabitacion = 'DISPONIBLE' | 'MANTENIMIENTO' | 'FUERA_DE_SERVICIO' | 'OCUPADA';

export interface Habitacion {
  id: string;
  numero: number;
  piso: number;
  tipo: TipoHabitacion;
  descripcion?: string;
  configuracionCamas: string;
  capacidad: number;
  tamanoM2: number;
  precioPorNoche: number;
  estado: EstadoHabitacion;
  imagenPortada?: string;
  imagenesAdicionales?: string[];
  amenidades?: string[];
}

export type EstadoServicio = 'ACTIVO' | 'INACTIVO';

/** Franja de disponibilidad de un servicio (horas como string 'HH:mm'). */
export interface Horario {
  dia: string;
  horaInicio: string;
  horaFin: string;
}

export interface Servicio {
  id: string;
  nombre: string;
  descripcionCorta: string;
  categoria: string;
  unidadCobro: string;
  precio: number;
  estado: EstadoServicio;
  disponibilidadHorarios: Horario[];
  imagenPortada?: string;
  descripcionDetallada?: string;
  ubicacion?: string;
  capacidadMaxima?: number;
  requisitosRestricciones?: string;
  imagenesAdicionales?: string[];
}

export type EstadoOferta = 'ACTIVA' | 'INACTIVA';

export interface Oferta {
  id: string;
  nombre: string;
  descripcion: string;
  tipoHabitacion: TipoHabitacion;
  serviciosIncluidos: string[];
  precio: number;
  vigenciaDesde: string;
  vigenciaHasta: string;
  estado: EstadoOferta;
  imagenPortada?: string;
}

// Estados definidos en el backend (Reserva.EstadoReserva). En Sprint 1 solo se usa ACTIVA.
export type EstadoReserva = 'ACTIVA' | 'MODIFICADA' | 'CANCELADA';

export interface Reserva {
  id: string;
  perfilId: string;
  habitacionId: string;
  serviciosContratados: string[];
  ofertaAplicada?: string | null;
  fechaEntrada: string;
  fechaSalida: string;
  estado: EstadoReserva;
  montoTotal: number;
  emailContacto: string;
  fechaCreacion: string;
}

/** Linea de un servicio adicional dentro del desglose de la cotizacion. */
export interface LineaServicio {
  servicioId: string;
  nombre: string;
  precio: number;
}

/** Desglose devuelto por cotizar/crear reserva (lo calcula el backend). */
export interface DesgloseReserva {
  noches: number;
  concepto: string;
  precioPorNoche: number;
  subtotalAlojamiento: number;
  serviciosAdicionales: LineaServicio[];
  subtotalServicios: number;
  montoTotal: number;
}

// Entidades a crear: el id y la imagenPortada los asigna el backend.
export type NuevaHabitacion = Omit<Habitacion, 'id' | 'imagenPortada'>;
export type NuevoServicio = Omit<Servicio, 'id' | 'imagenPortada'>;
export type NuevaOferta = Omit<Oferta, 'id' | 'imagenPortada'>;

export interface FiltroHabitaciones {
  estado?: EstadoHabitacion;
  tipo?: TipoHabitacion;
  capacidad?: number;
}

export interface CotizarPayload {
  perfilId: string;
  habitacionId: string;
  fechaEntrada: string;
  fechaSalida: string;
  serviciosIds: string[];
  ofertaId?: string | null;
}

export interface CrearReservaPayload extends CotizarPayload {
  emailContacto: string;
}

/** Body para crear un huesped manualmente. */
export interface CrearHuespedManual {
  nombre: string;
  email: string;
  telefono: string;
  cedula?: string;
}

/** Respuesta de la creacion manual: perfil + contrasena provisional (solo una vez). */
export interface HuespedCreadoResponse {
  perfil: Perfil;
  contrasenaProvisional: string;
}

/** Perfil devuelto por la busqueda de huesped (sin contrasena). */
export interface PerfilPublico {
  id: string;
  nombre: string;
  email: string;
  telefono: string;
  cedula: string;
  rol: Rol;
}

/**
 * Error normalizado que el API Client propaga a los componentes. {@code errores}
 * mapea campo -> mensaje (lo que el backend devuelve en 400) para mostrarlo
 * junto a cada control del formulario.
 */
export interface ApiError {
  status: number;
  mensaje: string;
  errores?: Record<string, string>;
}

/**
 * Gateway HTTP tipado hacia la Aplicacion API (backend en localhost:8080).
 *
 * Componente "API Client" del C4: es el unico lugar que habla HTTP; los demas
 * componentes inyectan este servicio y NO usan HttpClient crudo.
 */
@Injectable({
  providedIn: 'root',
})
export class ApiClient {
  private readonly baseUrl = 'http://localhost:8080';

  // === Estado compartido del catalogo de habitaciones (cache en RAM) ========
  // Patron "single source of truth" reactivo. El catalogo vive en este
  // singleton (providedIn: 'root'), asi que persiste en memoria aunque los
  // componentes que lo consumen se destruyan al cambiar de ruta. Cuando un
  // componente se vuelve a montar y se re-suscribe, el BehaviorSubject reemite
  // de inmediato (0 ms) el ultimo catalogo conocido: la lista nunca queda en
  // blanco mientras llega la respuesta fresca del backend.
  private readonly habitacionesSubject = new BehaviorSubject<Habitacion[]>([]);

  /** Stream publico del catalogo; los componentes lo consumen con `| async`. */
  readonly habitaciones$ = this.habitacionesSubject.asObservable();

  // Mismo patron para el resto de catalogos compartidos entre rutas: cada
  // BehaviorSubject es la "fuente unica de verdad" en RAM y sobrevive a la
  // destruccion de los componentes. Los listados usan los endpoints "todos".
  private readonly serviciosSubject = new BehaviorSubject<Servicio[]>([]);
  readonly servicios$ = this.serviciosSubject.asObservable();

  private readonly ofertasSubject = new BehaviorSubject<Oferta[]>([]);
  readonly ofertas$ = this.ofertasSubject.asObservable();

  private readonly perfilesSubject = new BehaviorSubject<Perfil[]>([]);
  readonly perfiles$ = this.perfilesSubject.asObservable();

  private readonly reservasSubject = new BehaviorSubject<Reserva[]>([]);
  readonly reservas$ = this.reservasSubject.asObservable();

  constructor(private readonly http: HttpClient) {}

  /** Snapshot sincrono del catalogo cacheado (p.ej. para decidir si mostrar spinner). */
  get habitacionesActuales(): Habitacion[] {
    return this.habitacionesSubject.value;
  }

  get serviciosActuales(): Servicio[] { return this.serviciosSubject.value; }
  get ofertasActuales(): Oferta[] { return this.ofertasSubject.value; }
  get perfilesActuales(): Perfil[] { return this.perfilesSubject.value; }
  get reservasActuales(): Reserva[] { return this.reservasSubject.value; }

  /**
   * Fuerza la recarga del catalogo desde el backend y empuja el resultado al
   * BehaviorSubject (lo que refresca a TODOS los suscriptores actuales). Devuelve
   * el Observable para que quien dispare la recarga gestione su propio spinner /
   * error; la actualizacion del cache ocurre via `tap`, independientemente de eso.
   */
  refrescarHabitaciones(): Observable<Habitacion[]> {
    return this.http
      .get<Habitacion[]>(`${this.baseUrl}/habitaciones`)
      .pipe(
        tap((habitaciones) => this.habitacionesSubject.next(habitaciones)),
        catchError((e: HttpErrorResponse) => this.manejarError(e)),
      );
  }

  /** Recarga el catalogo completo de servicios y lo empuja al cache. */
  refrescarServicios(): Observable<Servicio[]> {
    return this.http
      .get<Servicio[]>(`${this.baseUrl}/servicios/todos`)
      .pipe(
        tap((servicios) => this.serviciosSubject.next(servicios)),
        catchError((e: HttpErrorResponse) => this.manejarError(e)),
      );
  }

  /** Recarga el catalogo completo de ofertas y lo empuja al cache. */
  refrescarOfertas(): Observable<Oferta[]> {
    return this.http
      .get<Oferta[]>(`${this.baseUrl}/ofertas/todos`)
      .pipe(
        tap((ofertas) => this.ofertasSubject.next(ofertas)),
        catchError((e: HttpErrorResponse) => this.manejarError(e)),
      );
  }

  /** Recarga todos los perfiles y los empuja al cache. */
  refrescarPerfiles(): Observable<Perfil[]> {
    return this.http
      .get<Perfil[]>(`${this.baseUrl}/perfiles/todos`)
      .pipe(
        tap((perfiles) => this.perfilesSubject.next(perfiles)),
        catchError((e: HttpErrorResponse) => this.manejarError(e)),
      );
  }

  /** Recarga todas las reservas y las empuja al cache. */
  refrescarReservas(): Observable<Reserva[]> {
    return this.http
      .get<Reserva[]>(`${this.baseUrl}/reservas`)
      .pipe(
        tap((reservas) => this.reservasSubject.next(reservas)),
        catchError((e: HttpErrorResponse) => this.manejarError(e)),
      );
  }

  // Upserts en cache: tras una mutacion puntual (cambiar estado, editar notas)
  // sustituyen el elemento en el BehaviorSubject sin volver a pedir toda la lista,
  // de modo que TODOS los suscriptores ven el cambio al instante.
  actualizarServicioEnCache(servicio: Servicio): void {
    this.serviciosSubject.next(
      this.serviciosSubject.value.map((s) => (s.id === servicio.id ? servicio : s)),
    );
  }

  actualizarOfertaEnCache(oferta: Oferta): void {
    this.ofertasSubject.next(
      this.ofertasSubject.value.map((o) => (o.id === oferta.id ? oferta : o)),
    );
  }

  actualizarPerfilEnCache(perfil: Perfil): void {
    this.perfilesSubject.next(
      this.perfilesSubject.value.map((p) => (p.id === perfil.id ? perfil : p)),
    );
  }

  // --- Perfiles ---

  registrarPerfil(dto: RegistroPerfil): Observable<Perfil> {
    return this.http
      .post<Perfil>(`${this.baseUrl}/perfiles`, dto)
      .pipe(catchError((e: HttpErrorResponse) => this.manejarError(e)));
  }

  login(email: string, contrasena: string): Observable<Perfil> {
    return this.http
      .post<Perfil>(`${this.baseUrl}/perfiles/login`, { email, contrasena })
      .pipe(catchError((e: HttpErrorResponse) => this.manejarError(e)));
  }

  buscarPerfilPorEmail(email: string): Observable<PerfilPublico> {
    const params = new HttpParams().set('email', email);
    return this.http
      .get<PerfilPublico>(`${this.baseUrl}/perfiles/buscar`, { params })
      .pipe(catchError((e: HttpErrorResponse) => this.manejarError(e)));
  }

  consultarTodosPerfiles(): Observable<Perfil[]> {
    return this.http
      .get<Perfil[]>(`${this.baseUrl}/perfiles/todos`)
      .pipe(catchError((e: HttpErrorResponse) => this.manejarError(e)));
  }

  obtenerPerfilPorId(id: string): Observable<Perfil> {
    return this.http
      .get<Perfil>(`${this.baseUrl}/perfiles/${id}`)
      .pipe(catchError((e: HttpErrorResponse) => this.manejarError(e)));
  }

  crearHuespedManual(datos: CrearHuespedManual): Observable<HuespedCreadoResponse> {
    return this.http
      .post<HuespedCreadoResponse>(`${this.baseUrl}/perfiles/manual`, datos)
      .pipe(catchError((e: HttpErrorResponse) => this.manejarError(e)));
  }

  actualizarNotasInternas(id: string, notasInternas: string): Observable<Perfil> {
    return this.http
      .patch<Perfil>(`${this.baseUrl}/perfiles/${id}/notas`, { notasInternas })
      .pipe(catchError((e: HttpErrorResponse) => this.manejarError(e)));
  }

  // --- Habitaciones ---

  crearHabitacion(habitacion: NuevaHabitacion, imagen: File): Observable<Habitacion> {
    const cuerpo = this.construirMultipart('habitacion', habitacion, imagen);
    return this.http
      .post<Habitacion>(`${this.baseUrl}/habitaciones`, cuerpo)
      .pipe(catchError((e: HttpErrorResponse) => this.manejarError(e)));
  }

  consultarTodasHabitaciones(): Observable<Habitacion[]> {
    return this.http
      .get<Habitacion[]>(`${this.baseUrl}/habitaciones`)
      .pipe(catchError((e: HttpErrorResponse) => this.manejarError(e)));
  }

  consultarHabitaciones(filtros?: FiltroHabitaciones): Observable<Habitacion[]> {
    let params = new HttpParams();
    if (filtros?.estado) {
      params = params.set('estado', filtros.estado);
    }
    if (filtros?.tipo) {
      params = params.set('tipo', filtros.tipo);
    }
    if (filtros?.capacidad != null) {
      params = params.set('capacidad', String(filtros.capacidad));
    }
    return this.http
      .get<Habitacion[]>(`${this.baseUrl}/habitaciones`, { params })
      .pipe(catchError((e: HttpErrorResponse) => this.manejarError(e)));
  }

  // --- Servicios ---

  crearServicio(servicio: NuevoServicio, imagen: File): Observable<Servicio> {
    const cuerpo = this.construirMultipart('servicio', servicio, imagen);
    return this.http
      .post<Servicio>(`${this.baseUrl}/servicios`, cuerpo)
      .pipe(catchError((e: HttpErrorResponse) => this.manejarError(e)));
  }

  consultarServiciosActivos(): Observable<Servicio[]> {
    return this.http
      .get<Servicio[]>(`${this.baseUrl}/servicios`)
      .pipe(catchError((e: HttpErrorResponse) => this.manejarError(e)));
  }

  consultarTodosServicios(): Observable<Servicio[]> {
    return this.http
      .get<Servicio[]>(`${this.baseUrl}/servicios/todos`)
      .pipe(catchError((e: HttpErrorResponse) => this.manejarError(e)));
  }

  cambiarEstadoServicio(id: string, estado: EstadoServicio): Observable<Servicio> {
    return this.http
      .patch<Servicio>(`${this.baseUrl}/servicios/${id}/estado`, { estado })
      .pipe(catchError((e: HttpErrorResponse) => this.manejarError(e)));
  }

  // --- Ofertas ---

  crearOferta(oferta: NuevaOferta, imagen: File): Observable<Oferta> {
    const cuerpo = this.construirMultipart('oferta', oferta, imagen);
    return this.http
      .post<Oferta>(`${this.baseUrl}/ofertas`, cuerpo)
      .pipe(catchError((e: HttpErrorResponse) => this.manejarError(e)));
  }

  consultarOfertasActivas(): Observable<Oferta[]> {
    return this.http
      .get<Oferta[]>(`${this.baseUrl}/ofertas`)
      .pipe(catchError((e: HttpErrorResponse) => this.manejarError(e)));
  }

  consultarTodasOfertas(): Observable<Oferta[]> {
    return this.http
      .get<Oferta[]>(`${this.baseUrl}/ofertas/todos`)
      .pipe(catchError((e: HttpErrorResponse) => this.manejarError(e)));
  }

  cambiarEstadoOferta(id: string, estado: EstadoOferta): Observable<Oferta> {
    return this.http
      .patch<Oferta>(`${this.baseUrl}/ofertas/${id}/estado`, { estado })
      .pipe(catchError((e: HttpErrorResponse) => this.manejarError(e)));
  }

  // --- Reservas ---

  consultarDisponibilidad(
    fechaEntrada: string,
    fechaSalida: string,
    tipo?: TipoHabitacion,
    capacidad?: number,
    ofertaId?: string,
  ): Observable<Habitacion[]> {
    let params = new HttpParams()
      .set('fechaEntrada', fechaEntrada)
      .set('fechaSalida', fechaSalida);
    if (tipo) {
      params = params.set('tipo', tipo);
    }
    if (capacidad != null) {
      params = params.set('capacidad', String(capacidad));
    }
    if (ofertaId) {
      params = params.set('ofertaId', ofertaId);
    }
    return this.http
      .get<Habitacion[]>(`${this.baseUrl}/reservas/disponibilidad`, { params })
      .pipe(catchError((e: HttpErrorResponse) => this.manejarError(e)));
  }

  consultarReservas(): Observable<Reserva[]> {
    return this.http
      .get<Reserva[]>(`${this.baseUrl}/reservas`)
      .pipe(catchError((e: HttpErrorResponse) => this.manejarError(e)));
  }

  cotizar(payload: CotizarPayload): Observable<DesgloseReserva> {
    return this.http
      .post<DesgloseReserva>(`${this.baseUrl}/reservas/cotizar`, payload)
      .pipe(catchError((e: HttpErrorResponse) => this.manejarError(e)));
  }

  crearReserva(payload: CrearReservaPayload): Observable<Reserva> {
    return this.http
      .post<Reserva>(`${this.baseUrl}/reservas`, payload)
      .pipe(catchError((e: HttpErrorResponse) => this.manejarError(e)));
  }

  // --- Helpers ---

  /**
   * Arma un FormData con la entidad como parte JSON (igual que esperan los
   * endpoints multipart del backend) + la imagen de portada como parte archivo.
   * No fija Content-Type: el navegador define el boundary del multipart.
   */
  private construirMultipart(nombreParte: string, entidad: unknown, imagen: File): FormData {
    const form = new FormData();
    const json = new Blob([JSON.stringify(entidad)], { type: 'application/json' });
    form.append(nombreParte, json, `${nombreParte}.json`);
    form.append('imagenPortada', imagen, imagen.name);
    return form;
  }

  /** Normaliza el error del backend y lo propaga (campo -> mensaje en 400). */
  private manejarError(err: HttpErrorResponse): Observable<never> {
    const cuerpo = (err.error ?? {}) as Partial<ApiError>;
    const apiError: ApiError = {
      status: err.status,
      mensaje: cuerpo.mensaje ?? 'Ocurrio un error al comunicarse con el servidor.',
      errores: cuerpo.errores,
    };
    return throwError(() => apiError);
  }
}
