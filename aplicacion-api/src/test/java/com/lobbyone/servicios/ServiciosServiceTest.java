package com.lobbyone.servicios;

import com.lobbyone.common.ValidacionException;
import com.lobbyone.imagestoragecomponent.ImageStorageComponent;
import com.lobbyone.servicios.Servicio.EstadoServicio;
import com.lobbyone.servicios.Servicio.Horario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalTime;
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
 * Pruebas de la logica de negocio de servicios (crear y consultar activos).
 * El repositorio e ImageStorageComponent se mockean.
 */
@ExtendWith(MockitoExtension.class)
class ServiciosServiceTest {

    private static final byte[] IMAGEN_VALIDA = new byte[] {1, 2, 3, 4};

    @Mock
    private ServiciosRepository repository;

    @Mock
    private ImageStorageComponent imageStorage;

    private ServiciosService service;

    @BeforeEach
    void inicializar() {
        service = new ServiciosService(repository, imageStorage);
    }

    private Servicio servicioValido() {
        Servicio s = new Servicio();
        s.setNombre("Masaje relajante");
        s.setCategoria("Spa");
        s.setDescripcionCorta("Masaje de 60 minutos");
        s.setUnidadCobro("por persona");
        s.setPrecio(new BigDecimal("45.00"));
        s.setEstado(EstadoServicio.ACTIVO);
        List<Horario> horarios = new ArrayList<>();
        horarios.add(new Horario("LUNES", LocalTime.of(8, 0), LocalTime.of(12, 0)));
        s.setDisponibilidadHorarios(horarios);
        return s;
    }

    private Servicio servicioConEstado(EstadoServicio estado) {
        Servicio s = new Servicio();
        s.setNombre("X");
        s.setEstado(estado);
        return s;
    }

    // --- crear ---

    @Test
    void crearServicioValidoGuardaImagenAsignaIdYRespetaEstado() {
        when(repository.buscarPorNombreYCategoria("Masaje relajante", "Spa")).thenReturn(Optional.empty());
        when(imageStorage.guardar(any(), any())).thenReturn("portada-spa.png");
        when(repository.guardar(any(Servicio.class))).thenAnswer(inv -> inv.getArgument(0));

        Servicio resultado = service.crear(servicioValido(), IMAGEN_VALIDA, "spa.png");

        assertNotNull(resultado.getId());
        assertEquals("portada-spa.png", resultado.getImagenPortada());
        assertEquals(EstadoServicio.ACTIVO, resultado.getEstado()); // el admin define el estado
        verify(imageStorage).guardar(IMAGEN_VALIDA, "spa.png");
        verify(repository).guardar(any(Servicio.class));
    }

    @Test
    void crearConNombreDuplicadoEnLaMismaCategoriaLanzaValidacion() {
        when(repository.buscarPorNombreYCategoria("Masaje relajante", "Spa"))
                .thenReturn(Optional.of(new Servicio()));

        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> service.crear(servicioValido(), IMAGEN_VALIDA, "spa.png"));

        assertTrue(ex.getErrores().containsKey("nombre"));
        verify(repository, never()).guardar(any());
        verify(imageStorage, never()).guardar(any(), any());
    }

    @Test
    void crearConPrecioNoPositivoLanzaValidacion() {
        Servicio s = servicioValido();
        s.setPrecio(BigDecimal.ZERO);
        when(repository.buscarPorNombreYCategoria(any(), any())).thenReturn(Optional.empty());

        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> service.crear(s, IMAGEN_VALIDA, "spa.png"));

        assertTrue(ex.getErrores().containsKey("precio"));
        verify(repository, never()).guardar(any());
    }

    @Test
    void crearSinDisponibilidadLanzaValidacion() {
        Servicio s = servicioValido();
        s.setDisponibilidadHorarios(null);
        when(repository.buscarPorNombreYCategoria(any(), any())).thenReturn(Optional.empty());

        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> service.crear(s, IMAGEN_VALIDA, "spa.png"));

        assertTrue(ex.getErrores().containsKey("disponibilidadHorarios"));
    }

    @Test
    void crearConHorarioInvalidoLanzaValidacion() {
        Servicio s = servicioValido();
        List<Horario> horarios = new ArrayList<>();
        // hora de inicio posterior a la de fin -> invalido
        horarios.add(new Horario("MARTES", LocalTime.of(14, 0), LocalTime.of(10, 0)));
        s.setDisponibilidadHorarios(horarios);
        when(repository.buscarPorNombreYCategoria(any(), any())).thenReturn(Optional.empty());

        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> service.crear(s, IMAGEN_VALIDA, "spa.png"));

        assertTrue(ex.getErrores().containsKey("disponibilidadHorarios"));
    }

    @Test
    void crearConCamposObligatoriosVaciosReportaCadaCampo() {
        Servicio vacio = new Servicio();

        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> service.crear(vacio, null, null));

        assertTrue(ex.getErrores().containsKey("nombre"));
        assertTrue(ex.getErrores().containsKey("categoria"));
        assertTrue(ex.getErrores().containsKey("descripcionCorta"));
        assertTrue(ex.getErrores().containsKey("unidadCobro"));
        assertTrue(ex.getErrores().containsKey("estado"));
        assertTrue(ex.getErrores().containsKey("precio"));
        assertTrue(ex.getErrores().containsKey("disponibilidadHorarios"));
        assertTrue(ex.getErrores().containsKey("imagenPortada"));
        verify(repository, never()).guardar(any());
    }

    @Test
    void crearConImagenInvalidaLanzaValidacionDeImagen() {
        when(repository.buscarPorNombreYCategoria(any(), any())).thenReturn(Optional.empty());
        when(imageStorage.guardar(any(), any()))
                .thenThrow(new IllegalArgumentException("El archivo no es una imagen valida."));

        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> service.crear(servicioValido(), IMAGEN_VALIDA, "falsa.png"));

        assertTrue(ex.getErrores().containsKey("imagenPortada"));
        verify(repository, never()).guardar(any());
    }

    // --- consultarActivos ---

    @Test
    void consultarActivosDevuelveSoloLosServiciosActivos() {
        when(repository.buscarTodos()).thenReturn(List.of(
                servicioConEstado(EstadoServicio.ACTIVO),
                servicioConEstado(EstadoServicio.INACTIVO),
                servicioConEstado(EstadoServicio.ACTIVO)));

        List<Servicio> resultado = service.consultarActivos();

        assertEquals(2, resultado.size());
        assertTrue(resultado.stream().allMatch(s -> s.getEstado() == EstadoServicio.ACTIVO));
    }
}
