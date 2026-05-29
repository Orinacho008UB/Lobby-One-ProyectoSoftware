package com.lobbyone.servicios;

import com.lobbyone.common.ValidacionException;
import com.lobbyone.imagestoragecomponent.ImageStorageComponent;
import com.lobbyone.servicios.Servicio.Horario;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Logica de negocio de servicios. Alcance Sprint 1: crear y consultar activos.
 */
@Service
public class ServiciosService {

    private final ServiciosRepository repository;
    private final ImageStorageComponent imageStorage;

    public ServiciosService(ServiciosRepository repository, ImageStorageComponent imageStorage) {
        this.repository = repository;
        this.imageStorage = imageStorage;
    }

    /**
     * Crea un nuevo servicio. El administrador define el estado inicial.
     *
     * Valida campos obligatorios, nombre unico dentro de la misma categoria,
     * precio mayor que 0, y que la disponibilidad tenga al menos un dia con un
     * horario valido. Guarda la imagen de portada con ImageStorageComponent.
     *
     * @throws ValidacionException con un mensaje por cada campo invalido.
     */
    public Servicio crear(Servicio servicio, byte[] imagenPortadaBytes, String nombreImagenPortada) {
        Map<String, String> errores = new LinkedHashMap<>();

        if (esVacio(servicio.getNombre())) {
            errores.put("nombre", "El nombre es obligatorio.");
        }
        if (esVacio(servicio.getCategoria())) {
            errores.put("categoria", "La categoria es obligatoria.");
        }
        // Unicidad de nombre dentro de la misma categoria (solo si ambos vienen).
        if (!esVacio(servicio.getNombre()) && !esVacio(servicio.getCategoria())
                && repository.buscarPorNombreYCategoria(servicio.getNombre(), servicio.getCategoria()).isPresent()) {
            errores.put("nombre", "Ya existe un servicio con ese nombre en esa categoria.");
        }

        if (esVacio(servicio.getDescripcionCorta())) {
            errores.put("descripcionCorta", "La descripcion corta es obligatoria.");
        }
        if (esVacio(servicio.getUnidadCobro())) {
            errores.put("unidadCobro", "La unidad de cobro es obligatoria.");
        }
        if (servicio.getEstado() == null) {
            errores.put("estado", "El estado inicial es obligatorio.");
        }

        BigDecimal precio = servicio.getPrecio();
        if (precio == null || precio.compareTo(BigDecimal.ZERO) <= 0) {
            errores.put("precio", "El precio debe ser mayor que 0.");
        }

        if (!tieneAlMenosUnHorarioValido(servicio.getDisponibilidadHorarios())) {
            errores.put("disponibilidadHorarios",
                    "Debe indicar al menos un dia con un horario valido (hora de inicio antes de la de fin).");
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

        servicio.setId(UUID.randomUUID().toString());
        servicio.setImagenPortada(rutaImagen);
        return repository.guardar(servicio);
    }

    /**
     * Devuelve unicamente los servicios en estado ACTIVO.
     */
    public List<Servicio> consultarActivos() {
        return repository.buscarTodos().stream()
                .filter(s -> s.getEstado() == Servicio.EstadoServicio.ACTIVO)
                .collect(Collectors.toList());
    }

    private boolean tieneAlMenosUnHorarioValido(List<Horario> horarios) {
        if (horarios == null || horarios.isEmpty()) {
            return false;
        }
        return horarios.stream().anyMatch(this::horarioValido);
    }

    private boolean horarioValido(Horario horario) {
        return horario != null
                && !esVacio(horario.getDia())
                && horario.getHoraInicio() != null
                && horario.getHoraFin() != null
                && horario.getHoraInicio().isBefore(horario.getHoraFin());
    }

    private boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
