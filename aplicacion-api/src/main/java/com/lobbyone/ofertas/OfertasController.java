package com.lobbyone.ofertas;

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
 * Controlador REST de ofertas. Expone los endpoints y delega en el Service.
 * Alcance Sprint 1: crear y consultar activas.
 */
@RestController
@RequestMapping("/ofertas")
public class OfertasController {

    private final OfertasService service;

    public OfertasController(OfertasService service) {
        this.service = service;
    }

    /**
     * POST /ofertas — crea una oferta.
     *
     * Se recibe como multipart: una parte JSON ({@code oferta}) y la imagen de
     * portada ({@code imagenPortada}).
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Oferta> crear(
            @RequestPart("oferta") Oferta oferta,
            @RequestPart("imagenPortada") MultipartFile imagenPortada) throws IOException {
        Oferta creada = service.crear(
                oferta, imagenPortada.getBytes(), imagenPortada.getOriginalFilename());
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    /** GET /ofertas — devuelve las ofertas en estado ACTIVA. */
    @GetMapping
    public List<Oferta> consultarActivas() {
        return service.consultarActivas();
    }

    @ExceptionHandler(ValidacionException.class)
    public ResponseEntity<Map<String, Object>> manejarValidacion(ValidacionException ex) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("mensaje", "Hay errores de validacion.");
        cuerpo.put("errores", ex.getErrores());
        return ResponseEntity.badRequest().body(cuerpo);
    }
}
