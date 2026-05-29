package com.lobbyone.dataaccesscomponent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.CollectionType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Componente transversal de acceso a datos.
 *
 * Lee y escribe listas de entidades hacia/desde archivos JSON usando Jackson.
 * Cada entidad se persiste en su propio archivo dentro de la carpeta base de
 * datos (un archivo por entidad, p.ej. {@code reservas.json}).
 *
 * Es generico: no conoce los tipos de dominio; el repository que lo usa indica
 * el tipo concreto en cada llamada.
 */
@Component
public class DataAccessComponent {

    private final ObjectMapper objectMapper;
    private final Path rutaBase;

    /**
     * @param rutaBase carpeta donde viven los archivos JSON (por defecto {@code data}).
     */
    public DataAccessComponent(@Value("${lobbyone.data.ruta-base:data}") String rutaBase) {
        this.rutaBase = Paths.get(rutaBase);
        this.objectMapper = new ObjectMapper();
        // Registra modulos disponibles en el classpath (p.ej. soporte de java.time).
        this.objectMapper.findAndRegisterModules();
        // Fechas como texto ISO-8601, no como timestamps numericos.
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // JSON legible (util al ser un proyecto academico que inspecciona los archivos).
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Lee la lista de entidades del archivo indicado.
     *
     * @param nombreArchivo nombre del archivo dentro de la carpeta base (p.ej. {@code "perfiles.json"}).
     * @param tipo          clase de las entidades contenidas en la lista.
     * @return la lista de entidades; una lista vacia si el archivo no existe.
     */
    public <T> List<T> leerLista(String nombreArchivo, Class<T> tipo) {
        Path archivo = rutaBase.resolve(nombreArchivo);
        if (!Files.exists(archivo)) {
            return new ArrayList<>();
        }
        try {
            CollectionType tipoLista =
                    objectMapper.getTypeFactory().constructCollectionType(List.class, tipo);
            return objectMapper.readValue(archivo.toFile(), tipoLista);
        } catch (IOException e) {
            throw new DataAccessException("No se pudo leer el archivo: " + archivo, e);
        }
    }

    /**
     * Guarda la lista de entidades en el archivo indicado, sobrescribiendo su
     * contenido. Crea la carpeta base si no existe.
     *
     * @param nombreArchivo nombre del archivo dentro de la carpeta base.
     * @param entidades     lista de entidades a persistir.
     */
    public <T> void guardarLista(String nombreArchivo, List<T> entidades) {
        Path archivo = rutaBase.resolve(nombreArchivo);
        try {
            Files.createDirectories(rutaBase);
            objectMapper.writeValue(archivo.toFile(), entidades);
        } catch (IOException e) {
            throw new DataAccessException("No se pudo guardar el archivo: " + archivo, e);
        }
    }

    /**
     * Indica si el archivo de datos existe en la carpeta base.
     */
    public boolean existe(String nombreArchivo) {
        return Files.exists(rutaBase.resolve(nombreArchivo));
    }
}
