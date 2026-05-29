package com.lobbyone.imagestoragecomponent;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

/**
 * Componente transversal de almacenamiento de imagenes.
 *
 * Guarda y lee imagenes en el sistema de archivos local (Java NIO), bajo la
 * carpeta base de imagenes. Valida que el contenido sea realmente una imagen
 * (firma de bytes) y que la extension sea de un formato soportado.
 *
 * Las imagenes se guardan con un nombre unico generado; los metodos devuelven
 * la ruta RELATIVA a la carpeta base (es decir, el nombre del archivo), que es
 * lo que se persiste en las entidades. Usado por habitaciones y ofertas.
 */
@Component
public class ImageStorageComponent {

    private static final Set<String> EXTENSIONES_PERMITIDAS =
            Set.of("png", "jpg", "jpeg", "gif", "bmp", "webp");

    private final Path rutaBase;

    public ImageStorageComponent(@Value("${lobbyone.imagenes.ruta-base:images}") String rutaBase) {
        this.rutaBase = Paths.get(rutaBase);
    }

    /**
     * Guarda una imagen en la carpeta base con un nombre unico.
     *
     * @param contenido      bytes de la imagen.
     * @param nombreOriginal nombre original (se usa solo para deducir la extension).
     * @return la ruta relativa (nombre de archivo) con la que quedo almacenada.
     * @throws IllegalArgumentException si el contenido no es una imagen o la extension no se soporta.
     * @throws ImageStorageException    si ocurre un error de E/S al escribir.
     */
    public String guardar(byte[] contenido, String nombreOriginal) {
        String extension = extraerExtension(nombreOriginal);
        if (!EXTENSIONES_PERMITIDAS.contains(extension)) {
            throw new IllegalArgumentException("Extension de imagen no soportada: " + extension);
        }
        if (contenido == null || !contenidoEsImagen(contenido)) {
            throw new IllegalArgumentException("El archivo no es una imagen valida.");
        }

        String nombreUnico = UUID.randomUUID() + "." + extension;
        Path destino = rutaBase.resolve(nombreUnico);
        try {
            Files.createDirectories(rutaBase);
            Files.write(destino, contenido);
        } catch (IOException e) {
            throw new ImageStorageException("No se pudo guardar la imagen: " + destino, e);
        }
        return nombreUnico;
    }

    /**
     * Lee los bytes de una imagen previamente almacenada.
     *
     * @param rutaRelativa nombre/ruta relativa devuelta por {@link #guardar}.
     * @throws ImageStorageException si la imagen no existe o no se puede leer.
     */
    public byte[] leer(String rutaRelativa) {
        Path archivo = rutaBase.resolve(rutaRelativa);
        if (!Files.exists(archivo)) {
            throw new ImageStorageException("La imagen no existe: " + archivo, null);
        }
        try {
            return Files.readAllBytes(archivo);
        } catch (IOException e) {
            throw new ImageStorageException("No se pudo leer la imagen: " + archivo, e);
        }
    }

    /**
     * Indica si existe una imagen con la ruta relativa indicada.
     */
    public boolean existe(String rutaRelativa) {
        return Files.exists(rutaBase.resolve(rutaRelativa));
    }

    private String extraerExtension(String nombre) {
        if (nombre == null) {
            return "";
        }
        int punto = nombre.lastIndexOf('.');
        if (punto < 0 || punto == nombre.length() - 1) {
            return "";
        }
        return nombre.substring(punto + 1).toLowerCase();
    }

    /**
     * Valida el contenido por firma de bytes (magic numbers) de los formatos
     * de imagen mas comunes.
     */
    private boolean contenidoEsImagen(byte[] b) {
        if (b.length >= 8
                && (b[0] & 0xFF) == 0x89 && b[1] == 0x50 && b[2] == 0x4E && b[3] == 0x47) {
            return true; // PNG
        }
        if (b.length >= 3
                && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return true; // JPEG
        }
        if (b.length >= 4
                && b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8') {
            return true; // GIF
        }
        if (b.length >= 2 && b[0] == 'B' && b[1] == 'M') {
            return true; // BMP
        }
        if (b.length >= 12
                && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return true; // WEBP
        }
        return false;
    }
}
