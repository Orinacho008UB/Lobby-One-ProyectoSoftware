package com.lobbyone.servicios;

import com.lobbyone.common.ValidacionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST de servicios. Expone los endpoints y delega en el Service.
 * Alcance Sprint 1: crear y consultar activos.
 */
@RestController
@RequestMapping("/servicios")
public class ServiciosController {

    private final ServiciosService service;

    public ServiciosController(ServiciosService service) {
        this.service = service;
    }

    /**
     * POST /servicios — crea un servicio.
     *
     * Se recibe como multipart: una parte JSON ({@code servicio}) y la imagen de
     * portada ({@code imagenPortada}).
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Servicio> crear(
            @RequestPart("servicio") Servicio servicio,
            @RequestPart("imagenPortada") MultipartFile imagenPortada) throws IOException {
        Servicio creado = service.crear(
                servicio, imagenPortada.getBytes(), imagenPortada.getOriginalFilename());
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    /** GET /servicios — devuelve los servicios en estado ACTIVO. */
    @GetMapping
    public List<Servicio> consultarActivos() {
        return service.consultarActivos();
    }

    @ExceptionHandler(ValidacionException.class)
    public ResponseEntity<Map<String, Object>> manejarValidacion(ValidacionException ex) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("mensaje", "Hay errores de validacion.");
        cuerpo.put("errores", ex.getErrores());
        return ResponseEntity.badRequest().body(cuerpo);
    }
}
