package com.lobbyone.habitaciones;

import com.lobbyone.common.ValidacionException;
import com.lobbyone.habitaciones.Habitacion.EstadoHabitacion;
import com.lobbyone.habitaciones.Habitacion.TipoHabitacion;
import com.lobbyone.imagestoragecomponent.ImageStorageComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
 * Pruebas de la logica de negocio de habitaciones (registrar y consultar).
 * El repositorio e ImageStorageComponent se mockean.
 */
@ExtendWith(MockitoExtension.class)
class HabitacionesServiceTest {

    private static final byte[] IMAGEN_VALIDA = new byte[] {1, 2, 3, 4};

    @Mock
    private HabitacionesRepository repository;

    @Mock
    private ImageStorageComponent imageStorage;

    private HabitacionesService service;

    @BeforeEach
    void inicializar() {
        service = new HabitacionesService(repository, imageStorage);
    }

    private Habitacion habitacionValida() {
        Habitacion h = new Habitacion();
        h.setNumero(101);
        h.setPiso(1);
        h.setTipo(TipoHabitacion.DOBLE);
        h.setDescripcion("Habitacion doble con vista a la ciudad");
        h.setConfiguracionCamas("2 camas matrimoniales");
        h.setCapacidad(2);
        h.setTamanoM2(28.5);
        h.setPrecioPorNoche(new BigDecimal("120.00"));
        h.setEstado(EstadoHabitacion.DISPONIBLE);
        return h;
    }

    private Habitacion habitacion(int numero, TipoHabitacion tipo, int capacidad, EstadoHabitacion estado) {
        Habitacion h = new Habitacion();
        h.setNumero(numero);
        h.setTipo(tipo);
        h.setCapacidad(capacidad);
        h.setEstado(estado);
        return h;
    }

    // --- registrar ---

    @Test
    void registrarHabitacionValidaGuardaImagenAsignaIdYPersiste() {
        when(repository.buscarPorNumero(101)).thenReturn(Optional.empty());
        when(imageStorage.guardar(any(), any())).thenReturn("portada-101.png");
        when(repository.guardar(any(Habitacion.class))).thenAnswer(inv -> inv.getArgument(0));

        Habitacion resultado = service.registrar(habitacionValida(), IMAGEN_VALIDA, "portada.png");

        assertNotNull(resultado.getId());
        assertEquals("portada-101.png", resultado.getImagenPortada());
        verify(imageStorage).guardar(IMAGEN_VALIDA, "portada.png");
        verify(repository).guardar(any(Habitacion.class));
    }

    @Test
    void registrarSinDescripcionEsValidoPorqueEsOpcional() {
        Habitacion h = habitacionValida();
        h.setDescripcion(null);
        when(repository.buscarPorNumero(101)).thenReturn(Optional.empty());
        when(imageStorage.guardar(any(), any())).thenReturn("portada-101.png");
        when(repository.guardar(any(Habitacion.class))).thenAnswer(inv -> inv.getArgument(0));

        Habitacion resultado = service.registrar(h, IMAGEN_VALIDA, "portada.png");

        assertNotNull(resultado.getId());
        verify(repository).guardar(any(Habitacion.class));
    }

    @Test
    void registrarSinPisoLanzaValidacion() {
        Habitacion h = habitacionValida();
        h.setPiso(null);
        when(repository.buscarPorNumero(101)).thenReturn(Optional.empty());

        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> service.registrar(h, IMAGEN_VALIDA, "portada.png"));

        assertTrue(ex.getErrores().containsKey("piso"));
        verify(repository, never()).guardar(any());
    }

    @Test
    void registrarConNumeroDuplicadoLanzaValidacion() {
        when(repository.buscarPorNumero(101)).thenReturn(Optional.of(new Habitacion()));

        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> service.registrar(habitacionValida(), IMAGEN_VALIDA, "portada.png"));

        assertTrue(ex.getErrores().containsKey("numero"));
        verify(repository, never()).guardar(any());
        verify(imageStorage, never()).guardar(any(), any());
    }

    @Test
    void registrarConPrecioNoPositivoLanzaValidacion() {
        Habitacion h = habitacionValida();
        h.setPrecioPorNoche(BigDecimal.ZERO);
        when(repository.buscarPorNumero(101)).thenReturn(Optional.empty());

        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> service.registrar(h, IMAGEN_VALIDA, "portada.png"));

        assertTrue(ex.getErrores().containsKey("precioPorNoche"));
        verify(repository, never()).guardar(any());
    }

    @Test
    void registrarConCapacidadYTamanoNoPositivosLanzaValidacion() {
        Habitacion h = habitacionValida();
        h.setCapacidad(0);
        h.setTamanoM2(0);
        when(repository.buscarPorNumero(101)).thenReturn(Optional.empty());

        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> service.registrar(h, IMAGEN_VALIDA, "portada.png"));

        assertTrue(ex.getErrores().containsKey("capacidad"));
        assertTrue(ex.getErrores().containsKey("tamanoM2"));
    }

    @Test
    void registrarConCamposObligatoriosVaciosReportaCadaCampo() {
        Habitacion vacia = new Habitacion(); // numero 0, todo nulo

        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> service.registrar(vacia, null, null));

        assertTrue(ex.getErrores().containsKey("numero"));
        assertTrue(ex.getErrores().containsKey("piso"));
        assertTrue(ex.getErrores().containsKey("tipo"));
        assertTrue(ex.getErrores().containsKey("configuracionCamas"));
        assertTrue(ex.getErrores().containsKey("estado"));
        assertTrue(ex.getErrores().containsKey("precioPorNoche"));
        assertTrue(ex.getErrores().containsKey("imagenPortada"));
        verify(repository, never()).guardar(any());
    }

    @Test
    void registrarConImagenInvalidaLanzaValidacionDeImagen() {
        when(repository.buscarPorNumero(101)).thenReturn(Optional.empty());
        when(imageStorage.guardar(any(), any()))
                .thenThrow(new IllegalArgumentException("El archivo no es una imagen valida."));

        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> service.registrar(habitacionValida(), IMAGEN_VALIDA, "falsa.png"));

        assertTrue(ex.getErrores().containsKey("imagenPortada"));
        verify(repository, never()).guardar(any());
    }

    // --- consultar ---

    @Test
    void consultarSinFiltrosDevuelveTodas() {
        when(repository.buscarTodas()).thenReturn(List.of(
                habitacion(101, TipoHabitacion.DOBLE, 2, EstadoHabitacion.DISPONIBLE),
                habitacion(102, TipoHabitacion.SUITE, 4, EstadoHabitacion.OCUPADA)));

        List<Habitacion> resultado = service.consultar(new FiltroHabitaciones(null, null, null));

        assertEquals(2, resultado.size());
    }

    @Test
    void consultarFiltraPorEstadoDisponible() {
        when(repository.buscarTodas()).thenReturn(List.of(
                habitacion(101, TipoHabitacion.DOBLE, 2, EstadoHabitacion.DISPONIBLE),
                habitacion(102, TipoHabitacion.SUITE, 4, EstadoHabitacion.MANTENIMIENTO)));

        List<Habitacion> resultado =
                service.consultar(new FiltroHabitaciones(EstadoHabitacion.DISPONIBLE, null, null));

        assertEquals(1, resultado.size());
        assertEquals(101, resultado.get(0).getNumero());
    }

    @Test
    void consultarFiltraPorTipoYCapacidadMinima() {
        when(repository.buscarTodas()).thenReturn(List.of(
                habitacion(101, TipoHabitacion.DOBLE, 2, EstadoHabitacion.DISPONIBLE),
                habitacion(102, TipoHabitacion.SUITE, 4, EstadoHabitacion.DISPONIBLE),
                habitacion(103, TipoHabitacion.SUITE, 1, EstadoHabitacion.DISPONIBLE)));

        List<Habitacion> resultado =
                service.consultar(new FiltroHabitaciones(null, TipoHabitacion.SUITE, 2));

        assertEquals(1, resultado.size());
        assertEquals(102, resultado.get(0).getNumero());
    }
}
