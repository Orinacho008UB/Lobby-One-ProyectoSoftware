package com.lobbyone.habitaciones;

import com.lobbyone.common.ValidacionException;
import com.lobbyone.habitaciones.Habitacion.EstadoHabitacion;
import com.lobbyone.habitaciones.Habitacion.TipoHabitacion;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controlador REST de habitaciones. Expone los endpoints y delega en el Service.
 * Alcance Sprint 1: registrar y consultar.
 */
@RestController
@RequestMapping("/habitaciones")
public class HabitacionesController {

    private final HabitacionesService service;

    public HabitacionesController(HabitacionesService service) {
        this.service = service;
    }

    /**
     * POST /habitaciones — registra una habitacion.
     *
     * Se recibe como multipart: una parte JSON ({@code habitacion}) y la imagen
     * de portada ({@code imagenPortada}).
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Habitacion> registrar(
            @RequestPart("habitacion") Habitacion habitacion,
            @RequestPart("imagenPortada") MultipartFile imagenPortada) throws IOException {
        Habitacion creada = service.registrar(
                habitacion, imagenPortada.getBytes(), imagenPortada.getOriginalFilename());
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    /**
     * GET /habitaciones — consulta habitaciones con filtros opcionales:
     * {@code estado}, {@code tipo} y {@code capacidad} (minima).
     */
    @GetMapping
    public List<Habitacion> consultar(
            @RequestParam(required = false) EstadoHabitacion estado,
            @RequestParam(required = false) TipoHabitacion tipo,
            @RequestParam(required = false) Integer capacidad) {
        return service.consultar(new FiltroHabitaciones(estado, tipo, capacidad));
    }

    @ExceptionHandler(ValidacionException.class)
    public ResponseEntity<Map<String, Object>> manejarValidacion(ValidacionException ex) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("mensaje", "Hay errores de validacion.");
        cuerpo.put("errores", ex.getErrores());
        return ResponseEntity.badRequest().body(cuerpo);
    }
}
