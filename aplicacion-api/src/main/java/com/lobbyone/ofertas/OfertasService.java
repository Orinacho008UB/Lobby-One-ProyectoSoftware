package com.lobbyone.ofertas;

import com.lobbyone.common.ValidacionException;
import com.lobbyone.habitaciones.FiltroHabitaciones;
import com.lobbyone.habitaciones.HabitacionesService;
import com.lobbyone.imagestoragecomponent.ImageStorageComponent;
import com.lobbyone.servicios.Servicio;
import com.lobbyone.servicios.ServiciosService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Logica de negocio de ofertas. Alcance Sprint 1: crear y consultar activas.
 *
 * Una oferta es un PAQUETE (tipo de habitacion + servicios incluidos + precio
 * fijo), no un descuento.
 *
 * Depende de ServiciosService y HabitacionesService para validar la coherencia
 * del paquete (servicios activos y tipo de habitacion existente en el catalogo).
 */
@Service
public class OfertasService {

    private final OfertasRepository repository;
    private final ImageStorageComponent imageStorage;
    private final ServiciosService serviciosService;
    private final HabitacionesService habitacionesService;

    public OfertasService(OfertasRepository repository, ImageStorageComponent imageStorage,
                          ServiciosService serviciosService, HabitacionesService habitacionesService) {
        this.repository = repository;
        this.imageStorage = imageStorage;
        this.serviciosService = serviciosService;
        this.habitacionesService = habitacionesService;
    }

    /**
     * Crea una nueva oferta (paquete). El administrador define el estado inicial.
     *
     * @throws ValidacionException con un mensaje por cada campo invalido.
     */
    public Oferta crear(Oferta oferta, byte[] imagenPortadaBytes, String nombreImagenPortada) {
        Map<String, String> errores = new LinkedHashMap<>();
        LocalDate hoy = LocalDate.now();

        if (esVacio(oferta.getNombre())) {
            errores.put("nombre", "El nombre es obligatorio.");
        } else if (repository.buscarPorNombre(oferta.getNombre()).isPresent()) {
            errores.put("nombre", "Ya existe una oferta con ese nombre.");
        }

        // La descripcion es obligatoria segun el ERS (Historia 5).
        if (esVacio(oferta.getDescripcion())) {
            errores.put("descripcion", "La descripcion es obligatoria.");
        }

        BigDecimal precio = oferta.getPrecio();
        if (precio == null || precio.compareTo(BigDecimal.ZERO) <= 0) {
            errores.put("precio", "El precio debe ser mayor que 0.");
        }

        if (oferta.getEstado() == null) {
            errores.put("estado", "El estado inicial es obligatorio.");
        }

        validarTipoHabitacion(oferta, errores);
        validarServiciosIncluidos(oferta, errores);
        validarVigencias(oferta, hoy, errores);

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

        oferta.setId(UUID.randomUUID().toString());
        oferta.setImagenPortada(rutaImagen);
        return repository.guardar(oferta);
    }

    /**
     * Devuelve unicamente las ofertas en estado ACTIVA.
     */
    public List<Oferta> consultarActivas() {
        return repository.buscarTodas().stream()
                .filter(o -> o.getEstado() == Oferta.EstadoOferta.ACTIVA)
                .collect(Collectors.toList());
    }

    /** El tipo de habitacion del paquete debe existir en el catalogo. */
    private void validarTipoHabitacion(Oferta oferta, Map<String, String> errores) {
        if (oferta.getTipoHabitacion() == null) {
            errores.put("tipoHabitacion", "El tipo de habitacion es obligatorio.");
            return;
        }
        boolean existe = !habitacionesService
                .consultar(new FiltroHabitaciones(null, oferta.getTipoHabitacion(), null))
                .isEmpty();
        if (!existe) {
            errores.put("tipoHabitacion", "No existe ninguna habitacion de ese tipo en el catalogo.");
        }
    }

    /** Los servicios incluidos deben existir y estar ACTIVOS. */
    private void validarServiciosIncluidos(Oferta oferta, Map<String, String> errores) {
        List<String> incluidos = oferta.getServiciosIncluidos();
        if (incluidos == null || incluidos.isEmpty()) {
            errores.put("serviciosIncluidos", "Debe incluir al menos un servicio.");
            return;
        }
        Set<String> idsActivos = serviciosService.consultarActivos().stream()
                .map(Servicio::getId)
                .collect(Collectors.toSet());
        boolean todosActivos = idsActivos.containsAll(incluidos);
        if (!todosActivos) {
            errores.put("serviciosIncluidos",
                    "Todos los servicios incluidos deben existir y estar activos.");
        }
    }

    /** vigenciaDesde >= hoy, vigenciaHasta > hoy y vigenciaHasta > vigenciaDesde. */
    private void validarVigencias(Oferta oferta, LocalDate hoy, Map<String, String> errores) {
        LocalDate desde = oferta.getVigenciaDesde();
        LocalDate hasta = oferta.getVigenciaHasta();

        if (desde == null) {
            errores.put("vigenciaDesde", "La fecha de inicio de vigencia es obligatoria.");
        } else if (desde.isBefore(hoy)) {
            errores.put("vigenciaDesde", "La vigencia no puede comenzar antes de hoy.");
        }

        if (hasta == null) {
            errores.put("vigenciaHasta", "La fecha de fin de vigencia es obligatoria.");
        } else if (!hasta.isAfter(hoy)) {
            errores.put("vigenciaHasta", "La vigencia debe terminar despues de hoy.");
        } else if (desde != null && !hasta.isAfter(desde)) {
            errores.put("vigenciaHasta", "La fecha de fin debe ser posterior a la de inicio.");
        }
    }

    /**
     * Devuelve todas las ofertas sin filtrar por estado.
     * Solo para el administrador.
     */
    public List<Oferta> consultarTodas() {
        return repository.buscarTodas();
    }

    /**
     * Cambia el estado de una oferta (ACTIVA <-> INACTIVA).
     *
     * @throws ValidacionException si no existe una oferta con ese id.
     */
    public Oferta cambiarEstado(String id, Oferta.EstadoOferta nuevoEstado) {
        Oferta oferta = repository.buscarPorId(id)
                .orElseThrow(() -> new ValidacionException(
                        Map.of("id", "No se encontro una oferta con ese id.")));
        oferta.setEstado(nuevoEstado);
        return repository.guardar(oferta);
    }

    /**
     * Busca una oferta por su id. Lo usa el modulo de reservas para validar
     * vigencia y leer el precio del paquete al aplicar la oferta.
     */
    public java.util.Optional<Oferta> buscarPorId(String id) {
        return repository.buscarPorId(id);
    }

    private boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
