package com.lobbyone.reservas;

import com.lobbyone.common.ValidacionException;
import com.lobbyone.habitaciones.FiltroHabitaciones;
import com.lobbyone.habitaciones.Habitacion;
import com.lobbyone.habitaciones.Habitacion.EstadoHabitacion;
import com.lobbyone.habitaciones.Habitacion.TipoHabitacion;
import com.lobbyone.habitaciones.HabitacionesService;
import com.lobbyone.notificationcomponent.NotificationComponent;
import com.lobbyone.ofertas.Oferta;
import com.lobbyone.ofertas.OfertasService;
import com.lobbyone.perfiles.Perfil;
import com.lobbyone.perfiles.PerfilesService;
import com.lobbyone.reservas.DesgloseReserva.LineaServicio;
import com.lobbyone.servicios.Servicio;
import com.lobbyone.servicios.ServiciosService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Logica de negocio de reservas. Alcance Sprint 1: crear reserva (flujo
 * completo). NO incluye modificar ni cancelar.
 *
 * Es el modulo mas dependiente: usa PerfilesService, HabitacionesService,
 * ServiciosService, OfertasService y NotificationComponent. La dependencia es
 * siempre reservas -> los demas, nunca al reves.
 */
@Service
public class ReservasService {

    /** Mensaje exacto cuando la habitacion deja de estar disponible al confirmar. */
    static final String HABITACION_NO_DISPONIBLE = "La habitación ya no se encuentra disponible";

    private final ReservasRepository repository;
    private final PerfilesService perfilesService;
    private final HabitacionesService habitacionesService;
    private final ServiciosService serviciosService;
    private final OfertasService ofertasService;
    private final NotificationComponent notificationComponent;

    public ReservasService(ReservasRepository repository, PerfilesService perfilesService,
                           HabitacionesService habitacionesService, ServiciosService serviciosService,
                           OfertasService ofertasService, NotificationComponent notificationComponent) {
        this.repository = repository;
        this.perfilesService = perfilesService;
        this.habitacionesService = habitacionesService;
        this.serviciosService = serviciosService;
        this.ofertasService = ofertasService;
        this.notificationComponent = notificationComponent;
    }

    /**
     * Busca las habitaciones disponibles para un rango de fechas, opcionalmente
     * filtrando por tipo y capacidad. Si se aplica una oferta, su tipo de
     * habitacion manda y se ignora cualquier tipo recibido aparte.
     *
     * Excluye las habitaciones con una reserva ACTIVA solapada con el rango.
     */
    public List<Habitacion> buscarDisponibles(LocalDate fechaEntrada, LocalDate fechaSalida,
                                              TipoHabitacion tipo, Integer capacidad, String ofertaId) {
        Map<String, String> errores = new LinkedHashMap<>();
        validarFechas(fechaEntrada, fechaSalida, errores);

        TipoHabitacion tipoEfectivo = tipo;
        if (!esVacio(ofertaId)) {
            Oferta oferta = resolverOfertaVigente(ofertaId, errores);
            if (oferta != null) {
                // La oferta predefine el tipo: ignora cualquier tipo recibido aparte.
                tipoEfectivo = oferta.getTipoHabitacion();
            }
        }

        if (!errores.isEmpty()) {
            throw new ValidacionException(errores);
        }

        List<Habitacion> candidatas = habitacionesService.consultar(
                new FiltroHabitaciones(EstadoHabitacion.DISPONIBLE, tipoEfectivo, capacidad));

        return candidatas.stream()
                .filter(h -> !haySolapamiento(h.getId(), fechaEntrada, fechaSalida))
                .collect(Collectors.toList());
    }

    /**
     * Calcula el desglose y el monto total de una posible reserva, SIN persistir.
     */
    public DesgloseReserva cotizar(String perfilId, String habitacionId, LocalDate fechaEntrada,
                                   LocalDate fechaSalida, List<String> serviciosIds, String ofertaId) {
        Map<String, String> errores = new LinkedHashMap<>();
        validarFechas(fechaEntrada, fechaSalida, errores);

        Habitacion habitacion = resolverHabitacion(habitacionId, errores);
        List<String> ids = serviciosIds == null ? List.of() : serviciosIds;
        Map<String, Servicio> activos = mapaServiciosActivos();
        validarServiciosActivos(ids, activos, errores);

        Oferta oferta = null;
        if (!esVacio(ofertaId)) {
            oferta = resolverOfertaVigente(ofertaId, errores);
            validarTipoCoincide(habitacion, oferta, errores);
        }

        if (!errores.isEmpty()) {
            throw new ValidacionException(errores);
        }

        return calcularDesglose(habitacion, fechaEntrada, fechaSalida, ids, oferta, activos);
    }

    /**
     * Confirma y crea una reserva aplicando todas las reglas del ERS. Persiste
     * solo si todas las validaciones pasan, y envia el correo de confirmacion.
     *
     * @throws ValidacionException con un mensaje por cada campo invalido, o con
     *                             el mensaje de habitacion no disponible si esta
     *                             se ocupa entre la cotizacion y la confirmacion.
     */
    public Reserva crear(String perfilId, String habitacionId, LocalDate fechaEntrada, LocalDate fechaSalida,
                         List<String> serviciosIds, String ofertaId, String emailContacto) {
        Map<String, String> errores = new LinkedHashMap<>();

        // El perfil debe corresponder a un cliente registrado (equivale a tener sesion).
        Perfil perfil = null;
        if (esVacio(perfilId)) {
            errores.put("perfil", "Debe iniciar sesion para realizar una reserva.");
        } else {
            Optional<Perfil> encontrado = perfilesService.buscarPorId(perfilId);
            if (encontrado.isEmpty()) {
                errores.put("perfil", "Debe iniciar sesion con un cliente registrado para reservar.");
            } else {
                perfil = encontrado.get();
            }
        }

        validarFechas(fechaEntrada, fechaSalida, errores);

        Habitacion habitacion = resolverHabitacion(habitacionId, errores);
        if (habitacion != null && habitacion.getEstado() != EstadoHabitacion.DISPONIBLE) {
            errores.put("habitacion", "La habitacion no esta disponible para reservar.");
        }

        List<String> ids = serviciosIds == null ? List.of() : serviciosIds;
        Map<String, Servicio> activos = mapaServiciosActivos();
        validarServiciosActivos(ids, activos, errores);

        Oferta oferta = null;
        if (!esVacio(ofertaId)) {
            oferta = resolverOfertaVigente(ofertaId, errores);
            validarTipoCoincide(habitacion, oferta, errores);
        }

        if (!errores.isEmpty()) {
            throw new ValidacionException(errores);
        }

        // Re-verifica la disponibilidad por fechas justo antes de confirmar.
        if (haySolapamiento(habitacionId, fechaEntrada, fechaSalida)) {
            throw new ValidacionException(Map.of("disponibilidad", HABITACION_NO_DISPONIBLE));
        }

        DesgloseReserva desglose = calcularDesglose(habitacion, fechaEntrada, fechaSalida, ids, oferta, activos);
        String email = esVacio(emailContacto) ? perfil.getEmail() : emailContacto;

        Reserva reserva = new Reserva();
        reserva.setId(UUID.randomUUID().toString());
        reserva.setPerfilId(perfilId);
        reserva.setHabitacionId(habitacionId);
        reserva.setServiciosContratados(new ArrayList<>(ids));
        reserva.setOfertaAplicada(esVacio(ofertaId) ? null : ofertaId);
        reserva.setFechaEntrada(fechaEntrada);
        reserva.setFechaSalida(fechaSalida);
        reserva.setEstado(Reserva.EstadoReserva.ACTIVA);
        reserva.setMontoTotal(desglose.montoTotal());
        reserva.setEmailContacto(email);
        reserva.setFechaCreacion(LocalDateTime.now());

        Reserva guardada = repository.guardar(reserva);

        // La reserva ya esta persistida: si el correo falla no se revierte.
        try {
            notificationComponent.enviarConfirmacionReserva(
                    email, perfil.getNombre(), guardada.getId(), fechaEntrada, fechaSalida);
        } catch (Exception ex) {
            // SMTP no configurado en dev (credenciales placeholder). Se ignora.
        }

        return guardada;
    }

    // --- Calculo del desglose (compartido por cotizar y crear) ---

    private DesgloseReserva calcularDesglose(Habitacion habitacion, LocalDate fechaEntrada, LocalDate fechaSalida,
                                             List<String> serviciosIds, Oferta oferta, Map<String, Servicio> activos) {
        long noches = ChronoUnit.DAYS.between(fechaEntrada, fechaSalida);

        BigDecimal precioPorNoche;
        String concepto;
        Set<String> incluidosEnBundle;
        if (oferta == null) {
            precioPorNoche = habitacion.getPrecioPorNoche();
            concepto = "Habitacion #" + habitacion.getNumero();
            incluidosEnBundle = Set.of();
        } else {
            // El precio de la oferta es POR NOCHE y reemplaza al de la habitacion.
            precioPorNoche = oferta.getPrecio();
            concepto = "Paquete: " + oferta.getNombre();
            incluidosEnBundle = oferta.getServiciosIncluidos() == null
                    ? Set.of() : new HashSet<>(oferta.getServiciosIncluidos());
        }

        BigDecimal subtotalAlojamiento = precioPorNoche.multiply(BigDecimal.valueOf(noches));

        List<LineaServicio> lineas = new ArrayList<>();
        BigDecimal subtotalServicios = BigDecimal.ZERO;
        for (String servicioId : serviciosIds) {
            if (incluidosEnBundle.contains(servicioId)) {
                continue; // va en el bundle de la oferta: no se cobra.
            }
            Servicio servicio = activos.get(servicioId);
            // Cobro plano: el precio del servicio una sola vez (no se multiplica por noches).
            lineas.add(new LineaServicio(servicioId, servicio.getNombre(), servicio.getPrecio()));
            subtotalServicios = subtotalServicios.add(servicio.getPrecio());
        }

        BigDecimal montoTotal = subtotalAlojamiento.add(subtotalServicios);
        return new DesgloseReserva(noches, concepto, precioPorNoche, subtotalAlojamiento,
                lineas, subtotalServicios, montoTotal);
    }

    // --- Validaciones y helpers ---

    private void validarFechas(LocalDate fechaEntrada, LocalDate fechaSalida, Map<String, String> errores) {
        if (fechaEntrada == null) {
            errores.put("fechaEntrada", "La fecha de entrada es obligatoria.");
        }
        if (fechaSalida == null) {
            errores.put("fechaSalida", "La fecha de salida es obligatoria.");
        }
        if (fechaEntrada != null && fechaSalida != null && !fechaEntrada.isBefore(fechaSalida)) {
            errores.put("fechaSalida", "La fecha de salida debe ser posterior a la de entrada.");
        }
    }

    private Habitacion resolverHabitacion(String habitacionId, Map<String, String> errores) {
        if (esVacio(habitacionId)) {
            errores.put("habitacionId", "La habitacion es obligatoria.");
            return null;
        }
        Optional<Habitacion> encontrada = habitacionesService.buscarPorId(habitacionId);
        if (encontrada.isEmpty()) {
            errores.put("habitacionId", "La habitacion indicada no existe.");
            return null;
        }
        return encontrada.get();
    }

    private Map<String, Servicio> mapaServiciosActivos() {
        return serviciosService.consultarActivos().stream()
                .collect(Collectors.toMap(Servicio::getId, s -> s, (a, b) -> a));
    }

    private void validarServiciosActivos(List<String> serviciosIds, Map<String, Servicio> activos,
                                         Map<String, String> errores) {
        for (String servicioId : serviciosIds) {
            if (!activos.containsKey(servicioId)) {
                errores.put("servicios", "Todos los servicios agregados deben existir y estar activos.");
                return;
            }
        }
    }

    /**
     * Resuelve una oferta y valida que exista, este ACTIVA y vigente (hoy dentro
     * de [vigenciaDesde, vigenciaHasta]). Devuelve la oferta encontrada (o null
     * si no existe) anotando los errores correspondientes.
     */
    private Oferta resolverOfertaVigente(String ofertaId, Map<String, String> errores) {
        Optional<Oferta> encontrada = ofertasService.buscarPorId(ofertaId);
        if (encontrada.isEmpty()) {
            errores.put("ofertaId", "La oferta indicada no existe.");
            return null;
        }
        Oferta oferta = encontrada.get();
        LocalDate hoy = LocalDate.now();
        boolean activa = oferta.getEstado() == Oferta.EstadoOferta.ACTIVA;
        boolean vigente = oferta.getVigenciaDesde() != null && oferta.getVigenciaHasta() != null
                && !hoy.isBefore(oferta.getVigenciaDesde())
                && !hoy.isAfter(oferta.getVigenciaHasta());
        if (!activa || !vigente) {
            errores.put("ofertaId", "La oferta no esta activa o vigente.");
        }
        return oferta;
    }

    private void validarTipoCoincide(Habitacion habitacion, Oferta oferta, Map<String, String> errores) {
        if (habitacion != null && oferta != null && habitacion.getTipo() != oferta.getTipoHabitacion()) {
            errores.put("oferta", "El tipo de la habitacion no coincide con el de la oferta.");
        }
    }

    /**
     * Hay solapamiento si existe una reserva ACTIVA de la habitacion cuyo rango
     * [r.entrada, r.salida) se cruza con [fechaEntrada, fechaSalida).
     * Regla: a1 < b2 && b1 < a2 (la salida libera la habitacion; salida == entrada
     * de otra reserva NO se solapa).
     */
    private boolean haySolapamiento(String habitacionId, LocalDate fechaEntrada, LocalDate fechaSalida) {
        return repository.buscarPorHabitacion(habitacionId).stream()
                .filter(r -> r.getEstado() == Reserva.EstadoReserva.ACTIVA)
                .anyMatch(r -> fechaEntrada.isBefore(r.getFechaSalida())
                        && r.getFechaEntrada().isBefore(fechaSalida));
    }

    private boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
