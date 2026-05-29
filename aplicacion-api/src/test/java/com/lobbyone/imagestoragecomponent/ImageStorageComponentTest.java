package com.lobbyone.imagestoragecomponent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del componente de almacenamiento de imagenes. Cada prueba usa una
 * carpeta temporal aislada ({@link TempDir}); no toca la carpeta real images/.
 */
class ImageStorageComponentTest {

    /** Cabecera PNG valida seguida de bytes de relleno. */
    private static final byte[] PNG_VALIDO = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x01, 0x02, 0x03
    };

    @TempDir
    Path carpetaTemporal;

    private ImageStorageComponent almacen;

    @BeforeEach
    void inicializar() {
        almacen = new ImageStorageComponent(carpetaTemporal.toString());
    }

    @Test
    void guardarImagenValidaDevuelveRutaRelativaYPermiteLeerla() {
        String ruta = almacen.guardar(PNG_VALIDO, "portada.png");

        assertTrue(ruta.endsWith(".png"));
        assertTrue(almacen.existe(ruta));
        assertArrayEquals(PNG_VALIDO, almacen.leer(ruta));
    }

    @Test
    void guardarCreaLaCarpetaBaseSiNoExiste() {
        Path subcarpeta = carpetaTemporal.resolve("images");
        ImageStorageComponent conSubcarpeta = new ImageStorageComponent(subcarpeta.toString());

        String ruta = conSubcarpeta.guardar(PNG_VALIDO, "foto.png");

        assertTrue(conSubcarpeta.existe(ruta));
    }

    @Test
    void guardarConExtensionNoSoportadaLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> almacen.guardar(PNG_VALIDO, "documento.pdf"));
    }

    @Test
    void guardarContenidoQueNoEsImagenLanzaExcepcion() {
        byte[] noImagen = "esto es texto plano, no una imagen".getBytes();

        assertThrows(IllegalArgumentException.class,
                () -> almacen.guardar(noImagen, "falsa.png"));
    }

    @Test
    void leerImagenInexistenteLanzaExcepcion() {
        assertThrows(ImageStorageException.class,
                () -> almacen.leer("no-existe.png"));
    }
}
