package com.lobbyone.dataaccesscomponent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del componente generico de acceso a datos. Cada prueba opera sobre
 * una carpeta temporal aislada ({@link TempDir}), por lo que no toca la carpeta
 * real {@code data/}.
 */
class DataAccessComponentTest {

    @TempDir
    Path carpetaTemporal;

    private DataAccessComponent dataAccess;

    @BeforeEach
    void inicializar() {
        dataAccess = new DataAccessComponent(carpetaTemporal.toString());
    }

    @Test
    void leerListaDeArchivoInexistenteDevuelveListaVacia() {
        List<Entidad> resultado = dataAccess.leerLista("inexistente.json", Entidad.class);

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }

    @Test
    void guardarYLeerHaceRoundTripDeLosDatos() {
        Entidad a = new Entidad("1", "Uno", LocalDate.of(2026, 1, 15), Entidad.Tipo.ALFA);
        Entidad b = new Entidad("2", "Dos", LocalDate.of(2026, 6, 30), Entidad.Tipo.BETA);

        dataAccess.guardarLista("entidades.json", List.of(a, b));
        List<Entidad> resultado = dataAccess.leerLista("entidades.json", Entidad.class);

        assertEquals(2, resultado.size());
        assertEquals("1", resultado.get(0).getId());
        assertEquals("Uno", resultado.get(0).getNombre());
        // Verifica que las fechas (java.time) se serializan y deserializan bien.
        assertEquals(LocalDate.of(2026, 1, 15), resultado.get(0).getFecha());
        // Verifica que los enums se preservan.
        assertEquals(Entidad.Tipo.BETA, resultado.get(1).getTipo());
    }

    @Test
    void guardarListaCreaLaCarpetaBaseSiNoExiste() {
        Path subcarpeta = carpetaTemporal.resolve("data");
        DataAccessComponent conSubcarpeta = new DataAccessComponent(subcarpeta.toString());
        assertFalse(Files.exists(subcarpeta), "la carpeta no deberia existir antes de guardar");

        conSubcarpeta.guardarLista("entidades.json", List.of(new Entidad("9", "Nueve", null, null)));

        assertTrue(Files.exists(subcarpeta.resolve("entidades.json")));
    }

    @Test
    void guardarListaSobrescribeElContenidoPrevio() {
        dataAccess.guardarLista("entidades.json",
                List.of(new Entidad("1", "Original", null, null)));
        dataAccess.guardarLista("entidades.json",
                List.of(new Entidad("2", "Reemplazo", null, null)));

        List<Entidad> resultado = dataAccess.leerLista("entidades.json", Entidad.class);

        assertEquals(1, resultado.size());
        assertEquals("2", resultado.get(0).getId());
        assertEquals("Reemplazo", resultado.get(0).getNombre());
    }

    @Test
    void guardarListaVaciaProduceLecturaVacia() {
        dataAccess.guardarLista("entidades.json", List.of());

        List<Entidad> resultado = dataAccess.leerLista("entidades.json", Entidad.class);

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }

    @Test
    void existeReflejaLaPresenciaDelArchivo() {
        assertFalse(dataAccess.existe("entidades.json"));

        dataAccess.guardarLista("entidades.json", List.of(new Entidad("1", "Uno", null, null)));

        assertTrue(dataAccess.existe("entidades.json"));
    }

    /**
     * Entidad de prueba: ejercita generics, fechas java.time y enums sin acoplar
     * el test a un modelo de dominio concreto.
     */
    public static class Entidad {

        public enum Tipo {
            ALFA,
            BETA
        }

        private String id;
        private String nombre;
        private LocalDate fecha;
        private Tipo tipo;

        public Entidad() {
        }

        public Entidad(String id, String nombre, LocalDate fecha, Tipo tipo) {
            this.id = id;
            this.nombre = nombre;
            this.fecha = fecha;
            this.tipo = tipo;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getNombre() {
            return nombre;
        }

        public void setNombre(String nombre) {
            this.nombre = nombre;
        }

        public LocalDate getFecha() {
            return fecha;
        }

        public void setFecha(LocalDate fecha) {
            this.fecha = fecha;
        }

        public Tipo getTipo() {
            return tipo;
        }

        public void setTipo(Tipo tipo) {
            this.tipo = tipo;
        }
    }
}
