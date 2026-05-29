package com.lobbyone.habitaciones;

import com.lobbyone.common.ValidacionException;
import com.lobbyone.imagestoragecomponent.ImageStorageComponent;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Logica de negocio de habitaciones. Alcance Sprint 1: registrar y consultar.
 */
@Service
public class HabitacionesService {

    private final HabitacionesRepository repository;
    private final ImageStorageComponent imageStorage;

    public HabitacionesService(HabitacionesRepository repository, ImageStorageComponent imageStorage) {
        this.repository = repository;
        this.imageStorage = imageStorage;
    }

    /**
     * Registra una nueva habitacion. El administrador define el estado inicial.
     *
     * Valida campos obligatorios, numero unico, precio/capacidad/tamano positivos,
     * y guarda la imagen de portada con ImageStorageComponent (que verifica que el
     * contenido sea realmente una imagen).
     *
     * @param habitacion          datos de la habitacion (sin imagen ni id).
     * @param imagenPortadaBytes  bytes de la imagen de portada (obligatoria).
     * @param nombreImagenPortada nombre original del archivo (para deducir la extension).
     * @throws ValidacionException con un mensaje por cada campo invalido.
     */
    public Habitacion registrar(Habitacion habitacion, byte[] imagenPortadaBytes, String nombreImagenPortada) {
        Map<String, String> errores = new LinkedHashMap<>();

        if (habitacion.getNumero() <= 0) {
            errores.put("numero", "El numero de habitacion debe ser mayor que 0.");
        } else if (repository.buscarPorNumero(habitacion.getNumero()).isPresent()) {
            errores.put("numero", "Ya existe una habitacion con ese numero.");
        }

        if (habitacion.getPiso() == null) {
            errores.put("piso", "El piso es obligatorio.");
        }
        if (habitacion.getTipo() == null) {
            errores.put("tipo", "El tipo de habitacion es obligatorio.");
        }
        // La descripcion es opcional segun el ERS (Historia 2).
        if (esVacio(habitacion.getConfiguracionCamas())) {
            errores.put("configuracionCamas", "La configuracion de camas es obligatoria.");
        }
        if (habitacion.getEstado() == null) {
            errores.put("estado", "El estado inicial es obligatorio.");
        }

        BigDecimal precio = habitacion.getPrecioPorNoche();
        if (precio == null || precio.compareTo(BigDecimal.ZERO) <= 0) {
            errores.put("precioPorNoche", "El precio por noche debe ser mayor que 0.");
        }
        if (habitacion.getCapacidad() <= 0) {
            errores.put("capacidad", "La capacidad debe ser mayor que 0.");
        }
        if (habitacion.getTamanoM2() <= 0) {
            errores.put("tamanoM2", "El tamano en m2 debe ser mayor que 0.");
        }
        if (imagenPortadaBytes == null || imagenPortadaBytes.length == 0) {
            errores.put("imagenPortada", "La imagen de portada es obligatoria.");
        }

        if (!errores.isEmpty()) {
            throw new ValidacionException(errores);
        }

        // Guarda la imagen; ImageStorageComponent valida que el contenido sea una imagen.
        String rutaImagen;
        try {
            rutaImagen = imageStorage.guardar(imagenPortadaBytes, nombreImagenPortada);
        } catch (IllegalArgumentException e) {
            throw new ValidacionException(Map.of("imagenPortada", e.getMessage()));
        }

        habitacion.setId(UUID.randomUUID().toString());
        habitacion.setImagenPortada(rutaImagen);
        return repository.guardar(habitacion);
    }

    /**
     * Devuelve las habitaciones que cumplen los filtros indicados. Cada filtro
     * en {@code null} se ignora.
     */
    public List<Habitacion> consultar(FiltroHabitaciones filtro) {
        return repository.buscarTodas().stream()
                .filter(h -> filtro.estado() == null || h.getEstado() == filtro.estado())
                .filter(h -> filtro.tipo() == null || h.getTipo() == filtro.tipo())
                .filter(h -> filtro.capacidad() == null || h.getCapacidad() >= filtro.capacidad())
                .collect(Collectors.toList());
    }

    private boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
