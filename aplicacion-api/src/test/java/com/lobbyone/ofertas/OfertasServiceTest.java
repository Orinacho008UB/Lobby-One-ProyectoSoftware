package com.lobbyone.ofertas;

import com.lobbyone.common.ValidacionException;
import com.lobbyone.habitaciones.Habitacion;
import com.lobbyone.habitaciones.Habitacion.TipoHabitacion;
import com.lobbyone.habitaciones.HabitacionesService;
import com.lobbyone.imagestoragecomponent.ImageStorageComponent;
import com.lobbyone.ofertas.Oferta.EstadoOferta;
import com.lobbyone.servicios.Servicio;
import com.lobbyone.servicios.ServiciosService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de la logica de negocio de ofertas (crear y consultar activas).
 * Se mockean el repositorio, ImageStorageComponent, ServiciosService y
 * HabitacionesService.
 */
@ExtendWith(MockitoExtension.class)
class OfertasServiceTest {

    private static final byte[] IMAGEN_VALIDA = new byte[] {1, 2, 3, 4};

    @Mock
    private OfertasRepository repository;

    @Mock
    private ImageStorageComponent imageStorage;

    @Mock
    private ServiciosService serviciosService;

    @Mock
    private HabitacionesService habitacionesService;

    private OfertasService service;

    @BeforeEach
    void inicializar() {
        service = new OfertasService(repository, imageStorage, serviciosService, habitacionesService);
    }

    private Oferta ofertaValida() {
        Oferta o = new Oferta();
        o.setNombre("Paquete Romantico");
        o.setDescripcion("Suite con spa incluido");
        o.setTipoHabitacion(TipoHabitacion.SUITE);
        List<String> servicios = new ArrayList<>();
        servicios.add("s1");
        servicios.add("s2");
        o.setServiciosIncluidos(servicios);
        o.setPrecio(new BigDecimal("200.00"));
        o.setVigenciaDesde(LocalDate.now());
        o.setVigenciaHasta(LocalDate.now().plusDays(30));
        o.setEstado(EstadoOferta.ACTIVA);
        return o;
    }

    private Servicio servicioConId(String id) {
        Servicio s = new Servicio();
        s.setId(id);
        return s;
    }

    private Oferta ofertaConEstado(EstadoOferta estado) {
        Oferta o = new Oferta();
        o.setEstado(estado);
        return o;
    }

    /** Stubs que hacen pasar las validaciones cruzadas (servicios activos + tipo en catalogo). */
    private void stubDependenciasCoherentes() {
        when(serviciosService.consultarActivos())
                .thenReturn(List.of(servicioConId("s1"), servicioConId("s2")));
        when(habitacionesService.consultar(any())).thenReturn(List.of(new Habitacion()));
    }

    // --- crear ---

    @Test
    void crearOfertaValidaGuardaImagenAsignaIdYRespetaEstado() {
        when(repository.buscarPorNombre("Paquete Romantico")).thenReturn(Optional.empty());
        stubDependenciasCoherentes();
        when(imageStorage.guardar(any(), any())).thenReturn("oferta-1.png");
        when(repository.guardar(any(Oferta.class))).thenAnswer(inv -> inv.getArgument(0));

        Oferta resultado = service.crear(ofertaValida(), IMAGEN_VALIDA, "oferta.png");

        assertNotNull(resultado.getId());
        assertEquals("oferta-1.png", resultado.getImagenPortada());
        assertEquals(EstadoOferta.ACTIVA, resultado.getEstado());
        verify(imageStorage).guardar(IMAGEN_VALIDA, "oferta.png");
        verify(repository).guardar(any(Oferta.class));
    }

    @Test
    void crearConNombreDuplicadoLanzaValidacion() {
        when(repository.buscarPorNombre("Paquete Romantico")).thenReturn(Optional.of(new Oferta()));
        stubDependenciasCoherentes();

        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> service.crear(ofertaValida(), IMAGEN_VALIDA, "oferta.png"));

        assertTrue(ex.getErrores().containsKey("nombre"));
        verify(repository, never()).guardar(any());
        verify(imageStorage, never()).guardar(any(), any());
    }

    @Test
    void crearConPrecioNoPositivoLanzaValidacion() {
        Oferta o = ofertaValida();
        o.setPrecio(BigDecimal.ZERO);
        when(repository.buscarPorNombre(any())).thenReturn(Optional.empty());
        stubDependenciasCoherentes();

        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> service.crear(o, IMAGEN_VALIDA, "oferta.png"));

        assertTrue(ex.getErrores().containsKey("precio"));
        verify(repository, never()).guardar(any());
    }

    @Test
    void crearConServicioIncluidoInactivoOInexistenteLanzaValidacion() {
        Oferta o = ofertaValida();
        o.getServiciosIncluidos().add("sX"); // no esta entre los activos
        when(repository.buscarPorNombre(any())).thenReturn(Optional.empty());
        when(serviciosService.consultarActivos())
                .thenReturn(List.of(servicioConId("s1"), servicioConId("s2")));
        when(habitacionesService.consultar(any())).thenReturn(List.of(new Habitacion()));

        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> service.crear(o, IMAGEN_VALIDA, "oferta.png"));

        assertTrue(ex.getErrores().containsKey("serviciosIncluidos"));
        verify(repository, never()).guardar(any());
    }

    @Test
    void crearConTipoHabitacionInexistenteEnCatalogoLanzaValidacion() {
        when(repository.buscarPorNombre(any())).thenReturn(Optional.empty());
        when(serviciosService.consultarActivos())
                .thenReturn(List.of(servicioConId("s1"), servicioConId("s2")));
        when(habitacionesService.consultar(any())).thenReturn(List.of()); // catalogo sin ese tipo

        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> service.crear(ofertaValida(), IMAGEN_VALIDA, "oferta.png"));

        assertTrue(ex.getErrores().containsKey("tipoHabitacion"));
        verify(repository, never()).guardar(any());
    }

    @Test
    void crearConVigenciaHastaNoPosteriorADesdeLanzaValidacion() {
        Oferta o = ofertaValida();
        o.setVigenciaDesde(LocalDate.now().plusDays(5));
        o.setVigenciaHasta(LocalDate.now().plusDays(2)); // anterior a desde (pero futura)
        when(repository.buscarPorNombre(any())).thenReturn(Optional.empty());
        stubDependenciasCoherentes();

        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> service.crear(o, IMAGEN_VALIDA, "oferta.png"));

        assertTrue(ex.getErrores().containsKey("vigenciaHasta"));
    }

    @Test
    void crearConVigenciaDesdeEnElPasadoLanzaValidacion() {
        Oferta o = ofertaValida();
        o.setVigenciaDesde(LocalDate.now().minusDays(1));
        o.setVigenciaHasta(LocalDate.now().plusDays(10));
        when(repository.buscarPorNombre(any())).thenReturn(Optional.empty());
        stubDependenciasCoherentes();

        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> service.crear(o, IMAGEN_VALIDA, "oferta.png"));

        assertTrue(ex.getErrores().containsKey("vigenciaDesde"));
    }

    @Test
    void crearConCamposObligatoriosVaciosReportaCadaCampo() {
        Oferta vacia = new Oferta();

        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> service.crear(vacia, null, null));

        assertTrue(ex.getErrores().containsKey("nombre"));
        assertTrue(ex.getErrores().containsKey("precio"));
        assertTrue(ex.getErrores().containsKey("estado"));
        assertTrue(ex.getErrores().containsKey("tipoHabitacion"));
        assertTrue(ex.getErrores().containsKey("serviciosIncluidos"));
        assertTrue(ex.getErrores().containsKey("vigenciaDesde"));
        assertTrue(ex.getErrores().containsKey("vigenciaHasta"));
        assertTrue(ex.getErrores().containsKey("imagenPortada"));
        verify(repository, never()).guardar(any());
    }

    @Test
    void crearConImagenInvalidaLanzaValidacionDeImagen() {
        when(repository.buscarPorNombre(any())).thenReturn(Optional.empty());
        stubDependenciasCoherentes();
        when(imageStorage.guardar(any(), any()))
                .thenThrow(new IllegalArgumentException("El archivo no es una imagen valida."));

        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> service.crear(ofertaValida(), IMAGEN_VALIDA, "falsa.png"));

        assertTrue(ex.getErrores().containsKey("imagenPortada"));
        verify(repository, never()).guardar(any());
    }

    // --- consultarActivas ---

    @Test
    void consultarActivasDevuelveSoloLasOfertasActivas() {
        when(repository.buscarTodas()).thenReturn(List.of(
                ofertaConEstado(EstadoOferta.ACTIVA),
                ofertaConEstado(EstadoOferta.INACTIVA),
                ofertaConEstado(EstadoOferta.ACTIVA)));

        List<Oferta> resultado = service.consultarActivas();

        assertEquals(2, resultado.size());
        assertTrue(resultado.stream().allMatch(o -> o.getEstado() == EstadoOferta.ACTIVA));
    }
}
