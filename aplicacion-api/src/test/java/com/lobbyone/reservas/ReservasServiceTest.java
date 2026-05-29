package com.lobbyone.reservas;

import com.lobbyone.common.ValidacionException;
import com.lobbyone.habitaciones.Habitacion;
import com.lobbyone.habitaciones.Habitacion.EstadoHabitacion;
import com.lobbyone.habitaciones.Habitacion.TipoHabitacion;
import com.lobbyone.habitaciones.HabitacionesService;
import com.lobbyone.notificationcomponent.NotificationComponent;
import com.lobbyone.ofertas.Oferta;
import com.lobbyone.ofertas.Oferta.EstadoOferta;
import com.lobbyone.ofertas.OfertasService;
import com.lobbyone.perfiles.Perfil;
import com.lobbyone.perfiles.PerfilesService;
import com.lobbyone.servicios.Servicio;
import com.lobbyone.servicios.Servicio.EstadoServicio;
import com.lobbyone.servicios.ServiciosService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de la logica de negocio de reservas (buscar disponibles, cotizar y
 * crear). Se mockean el repositorio y los servicios/componentes de los que
 * depende reservas.
 */
@ExtendWith(MockitoExtension.class)
class ReservasServiceTest {

    @Mock
    private ReservasRepository repository;
    @Mock
    private PerfilesService perfilesService;
    @Mock
    private HabitacionesService habitacionesService;
    @Mock
    private ServiciosService serviciosService;
    @Mock
    private OfertasService ofertasService;
    @Mock
    private NotificationComponent notificationComponent;

    private ReservasService nuevoService() {
        return new ReservasService(repository, perfilesService, habitacionesService,
                serviciosService, ofertasService, notificationComponent);
    }

    private Habitacion habitacionDisponible() {
        Habitacion h = new Habitacion();
        h.setId("h1");
        h.setNumero(101);
        h.setTipo(TipoHabitacion.SUITE);
        h.setEstado(EstadoHabitacion.DISPONIBLE);
        h.setPrecioPorNoche(new BigDecimal("100.00"));
        return h;
    }

    private Servicio servicio(String id, String precio) {
        Servicio s = new Servicio();
        s.setId(id);
        s.setNombre("Servicio " + id);
        s.setPrecio(new BigDecimal(precio));
        s.setEstado(EstadoServicio.ACTIVO);
        return s;
    }

    private Perfil cliente() {
        Perfil p = new Perfil();
        p.setId("p1");
        p.setNombre("Ana");
        p.setEmail("ana@correo.com");
        p.setRol(Perfil.Rol.CLIENTE);
        return p;
    }

    private Oferta ofertaVigente() {
        Oferta o = new Oferta();
        o.setId("o1");
        o.setNombre("Paquete Suite");
        o.setTipoHabitacion(TipoHabitacion.SUITE);
        o.setServiciosIncluidos(List.of("s1"));
        o.setPrecio(new BigDecimal("150.00"));
        o.setEstado(EstadoOferta.ACTIVA);
        o.setVigenciaDesde(LocalDate.now().minusDays(1));
        o.setVigenciaHasta(LocalDate.now().plusDays(30));
        return o;
    }

    // --- cotizar: calculo de montoTotal ---

    @Test
    void cotizarSinOfertaCalculaPrecioPorNochePorNochesMasServicios() {
        ReservasService service = nuevoService();
        when(habitacionesService.buscarPorId("h1")).thenReturn(Optional.of(habitacionDisponible()));
        when(serviciosService.consultarActivos())
                .thenReturn(List.of(servicio("s1", "50.00"), servicio("s2", "30.00")));

        // 3 noches x 100 = 300, + servicios 50 + 30 = 80 -> 380
        DesgloseReserva d = service.cotizar("p1", "h1",
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 4),
                List.of("s1", "s2"), null);

        assertEquals(3, d.noches());
        assertEquals(0, new BigDecimal("300.00").compareTo(d.subtotalAlojamiento()));
        assertEquals(0, new BigDecimal("80.00").compareTo(d.subtotalServicios()));
        assertEquals(0, new BigDecimal("380.00").compareTo(d.montoTotal()));
        assertEquals(2, d.serviciosAdicionales().size());
    }

    @Test
    void cotizarConOfertaUsaPrecioPaqueteYNoCobraServiciosIncluidos() {
        ReservasService service = nuevoService();
        when(habitacionesService.buscarPorId("h1")).thenReturn(Optional.of(habitacionDisponible()));
        when(serviciosService.consultarActivos())
                .thenReturn(List.of(servicio("s1", "50.00"), servicio("s2", "30.00")));
        when(ofertasService.buscarPorId("o1")).thenReturn(Optional.of(ofertaVigente()));

        // Oferta 150/noche x 2 noches = 300. s1 va en bundle (no se cobra), s2 adicional = 30 -> 330
        DesgloseReserva d = service.cotizar("p1", "h1",
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 3),
                List.of("s1", "s2"), "o1");

        assertEquals(2, d.noches());
        assertEquals(0, new BigDecimal("300.00").compareTo(d.subtotalAlojamiento()));
        assertEquals(0, new BigDecimal("30.00").compareTo(d.subtotalServicios()));
        assertEquals(0, new BigDecimal("330.00").compareTo(d.montoTotal()));
        // Solo s2 se cobra aparte; s1 va en el bundle.
        assertEquals(1, d.serviciosAdicionales().size());
        assertEquals("s2", d.serviciosAdicionales().get(0).servicioId());
    }

    @Test
    void cotizarConFechasInvalidasLanzaValidacion() {
        ReservasService service = nuevoService();

        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> service.cotizar("p1", "h1",
                        LocalDate.of(2026, 6, 4), LocalDate.of(2026, 6, 1), null, null));

        assertTrue(ex.getErrores().containsKey("fechaSalida"));
    }

    // --- buscarDisponibles: solapamiento de fechas ---

    @Test
    void buscarDisponiblesExcluyeHabitacionConReservaSolapada() {
        ReservasService service = nuevoService();
        when(habitacionesService.consultar(any())).thenReturn(List.of(habitacionDisponible()));
        Reserva existente = new Reserva();
        existente.setEstado(Reserva.EstadoReserva.ACTIVA);
        existente.setFechaEntrada(LocalDate.of(2026, 6, 10));
        existente.setFechaSalida(LocalDate.of(2026, 6, 15));
        when(repository.buscarPorHabitacion("h1")).thenReturn(List.of(existente));

        // [12,14) se solapa con [10,15)
        List<Habitacion> disponibles = service.buscarDisponibles(
                LocalDate.of(2026, 6, 12), LocalDate.of(2026, 6, 14), null, null, null);

        assertTrue(disponibles.isEmpty());
    }

    @Test
    void buscarDisponiblesIncluyeHabitacionCuandoSalidaIgualEntradaNoSeSolapa() {
        ReservasService service = nuevoService();
        when(habitacionesService.consultar(any())).thenReturn(List.of(habitacionDisponible()));
        Reserva existente = new Reserva();
        existente.setEstado(Reserva.EstadoReserva.ACTIVA);
        existente.setFechaEntrada(LocalDate.of(2026, 6, 10));
        existente.setFechaSalida(LocalDate.of(2026, 6, 15));
        when(repository.buscarPorHabitacion("h1")).thenReturn(List.of(existente));

        // [15,18): la salida de la existente (15) == entrada de la nueva -> NO se solapa
        List<Habitacion> disponibles = service.buscarDisponibles(
                LocalDate.of(2026, 6, 15), LocalDate.of(2026, 6, 18), null, null, null);

        assertEquals(1, disponibles.size());
    }

    @Test
    void buscarDisponiblesConOfertaFuerzaElTipoDeLaOferta() {
        ReservasService service = nuevoService();
        when(ofertasService.buscarPorId("o1")).thenReturn(Optional.of(ofertaVigente()));
        when(habitacionesService.consultar(any())).thenReturn(List.of());

        service.buscarDisponibles(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 3),
                TipoHabitacion.INDIVIDUAL, null, "o1");

        // El tipo efectivo debe ser el de la oferta (SUITE), ignorando INDIVIDUAL.
        ArgumentCaptor<com.lobbyone.habitaciones.FiltroHabitaciones> captor =
                ArgumentCaptor.forClass(com.lobbyone.habitaciones.FiltroHabitaciones.class);
        verify(habitacionesService).consultar(captor.capture());
        assertEquals(TipoHabitacion.SUITE, captor.getValue().tipo());
        assertEquals(EstadoHabitacion.DISPONIBLE, captor.getValue().estado());
    }

    // --- crear: flujo de confirmacion ---

    @Test
    void crearReservaValidaPersisteYEnviaCorreo() {
        ReservasService service = nuevoService();
        when(perfilesService.buscarPorId("p1")).thenReturn(Optional.of(cliente()));
        when(habitacionesService.buscarPorId("h1")).thenReturn(Optional.of(habitacionDisponible()));
        when(serviciosService.consultarActivos()).thenReturn(List.of(servicio("s1", "50.00")));
        when(repository.buscarPorHabitacion("h1")).thenReturn(List.of());
        when(repository.guardar(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

        Reserva creada = service.crear("p1", "h1",
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 4),
                List.of("s1"), null, "ana@correo.com");

        assertNotNull(creada.getId());
        assertEquals(Reserva.EstadoReserva.ACTIVA, creada.getEstado());
        assertNotNull(creada.getFechaCreacion());
        // 3 noches x 100 = 300 + servicio 50 = 350
        assertEquals(0, new BigDecimal("350.00").compareTo(creada.getMontoTotal()));

        ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
        verify(repository).guardar(captor.capture());
        assertEquals("p1", captor.getValue().getPerfilId());

        verify(notificationComponent).enviarConfirmacionReserva(
                eq("ana@correo.com"), eq("Ana"), anyString(),
                eq(LocalDate.of(2026, 6, 1)), eq(LocalDate.of(2026, 6, 4)));
    }

    @Test
    void crearConHabitacionYaReservadaInterrumpeConMensajeNoDisponible() {
        ReservasService service = nuevoService();
        when(perfilesService.buscarPorId("p1")).thenReturn(Optional.of(cliente()));
        when(habitacionesService.buscarPorId("h1")).thenReturn(Optional.of(habitacionDisponible()));
        when(serviciosService.consultarActivos()).thenReturn(List.of());
        Reserva existente = new Reserva();
        existente.setEstado(Reserva.EstadoReserva.ACTIVA);
        existente.setFechaEntrada(LocalDate.of(2026, 6, 1));
        existente.setFechaSalida(LocalDate.of(2026, 6, 10));
        when(repository.buscarPorHabitacion("h1")).thenReturn(List.of(existente));

        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> service.crear("p1", "h1",
                        LocalDate.of(2026, 6, 2), LocalDate.of(2026, 6, 5), null, null, null));

        assertTrue(ex.getErrores().containsValue("La habitación ya no se encuentra disponible"));
        verify(repository, never()).guardar(any());
        verify(notificationComponent, never())
                .enviarConfirmacionReserva(any(), any(), any(), any(), any());
    }

    @Test
    void crearConPerfilInexistenteLanzaValidacion() {
        ReservasService service = nuevoService();
        when(perfilesService.buscarPorId("desconocido")).thenReturn(Optional.empty());
        when(habitacionesService.buscarPorId("h1")).thenReturn(Optional.of(habitacionDisponible()));
        when(serviciosService.consultarActivos()).thenReturn(List.of());

        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> service.crear("desconocido", "h1",
                        LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 4), null, null, null));

        assertTrue(ex.getErrores().containsKey("perfil"));
        verify(repository, never()).guardar(any());
    }

    @Test
    void crearConServicioInactivoOInexistenteLanzaValidacion() {
        ReservasService service = nuevoService();
        when(perfilesService.buscarPorId("p1")).thenReturn(Optional.of(cliente()));
        when(habitacionesService.buscarPorId("h1")).thenReturn(Optional.of(habitacionDisponible()));
        when(serviciosService.consultarActivos()).thenReturn(List.of(servicio("s1", "50.00")));

        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> service.crear("p1", "h1",
                        LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 4),
                        List.of("s1", "sX"), null, null));

        assertTrue(ex.getErrores().containsKey("servicios"));
        verify(repository, never()).guardar(any());
    }

    @Test
    void crearConOfertaNoVigenteLanzaValidacion() {
        ReservasService service = nuevoService();
        when(perfilesService.buscarPorId("p1")).thenReturn(Optional.of(cliente()));
        when(habitacionesService.buscarPorId("h1")).thenReturn(Optional.of(habitacionDisponible()));
        when(serviciosService.consultarActivos()).thenReturn(List.of());
        Oferta vencida = ofertaVigente();
        vencida.setVigenciaDesde(LocalDate.now().minusDays(30));
        vencida.setVigenciaHasta(LocalDate.now().minusDays(1)); // ya expiro
        when(ofertasService.buscarPorId("o1")).thenReturn(Optional.of(vencida));

        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> service.crear("p1", "h1",
                        LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 4), null, "o1", null));

        assertTrue(ex.getErrores().containsKey("ofertaId"));
        verify(repository, never()).guardar(any());
    }

    @Test
    void crearConHabitacionNoDisponiblePorEstadoLanzaValidacion() {
        ReservasService service = nuevoService();
        Habitacion enMantenimiento = habitacionDisponible();
        enMantenimiento.setEstado(EstadoHabitacion.MANTENIMIENTO);
        when(perfilesService.buscarPorId("p1")).thenReturn(Optional.of(cliente()));
        when(habitacionesService.buscarPorId("h1")).thenReturn(Optional.of(enMantenimiento));
        when(serviciosService.consultarActivos()).thenReturn(List.of());

        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> service.crear("p1", "h1",
                        LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 4), null, null, null));

        assertTrue(ex.getErrores().containsKey("habitacion"));
        verify(repository, never()).guardar(any());
    }

    @Test
    void crearSinEmailContactoUsaElEmailDelPerfil() {
        ReservasService service = nuevoService();
        when(perfilesService.buscarPorId("p1")).thenReturn(Optional.of(cliente()));
        when(habitacionesService.buscarPorId("h1")).thenReturn(Optional.of(habitacionDisponible()));
        when(serviciosService.consultarActivos()).thenReturn(List.of());
        when(repository.buscarPorHabitacion("h1")).thenReturn(List.of());
        when(repository.guardar(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

        Reserva creada = service.crear("p1", "h1",
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 4), null, null, null);

        assertEquals("ana@correo.com", creada.getEmailContacto());
        verify(notificationComponent).enviarConfirmacionReserva(
                eq("ana@correo.com"), eq("Ana"), anyString(), any(), any());
    }
}
