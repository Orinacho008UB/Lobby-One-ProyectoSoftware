package com.lobbyone.perfiles;

import com.lobbyone.common.ValidacionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controlador REST de perfiles. Expone los endpoints y delega en el Service.
 * Alcance Sprint 1: registrar y login.
 */
@RestController
@RequestMapping("/perfiles")
public class PerfilesController {

    private final PerfilesService service;

    public PerfilesController(PerfilesService service) {
        this.service = service;
    }

    /** POST /perfiles — registra un nuevo perfil (rol CLIENTE). */
    @PostMapping
    public ResponseEntity<Perfil> registrar(@RequestBody Perfil perfil) {
        Perfil creado = service.registrar(perfil);
        creado.setContrasenaHash(null); // nunca exponer el hash en la respuesta
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    /** POST /perfiles/login — valida credenciales. */
    @PostMapping("/login")
    public ResponseEntity<Perfil> login(@RequestBody LoginRequest credenciales) {
        Perfil perfil = service.login(credenciales.email(), credenciales.contrasena());
        perfil.setContrasenaHash(null);
        return ResponseEntity.ok(perfil);
    }

    @ExceptionHandler(ValidacionException.class)
    public ResponseEntity<Map<String, Object>> manejarValidacion(ValidacionException ex) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("mensaje", "Hay errores de validacion.");
        cuerpo.put("errores", ex.getErrores());
        return ResponseEntity.badRequest().body(cuerpo);
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<Map<String, Object>> manejarCredenciales(CredencialesInvalidasException ex) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("mensaje", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(cuerpo);
    }

    /** Cuerpo de la peticion de login. */
    public record LoginRequest(String email, String contrasena) {
    }
}
