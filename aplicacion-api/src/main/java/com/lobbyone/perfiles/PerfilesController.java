package com.lobbyone.perfiles;

import com.lobbyone.common.ValidacionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    /**
     * GET /perfiles/buscar?email=xxx — devuelve el perfil con ese email (sin hash).
     * Usado por el admin para crear reservas a nombre de un huesped registrado.
     */
    @GetMapping("/buscar")
    public ResponseEntity<Perfil> buscarPorEmail(@RequestParam String email) {
        Perfil perfil = service.buscarPorEmail(email);
        perfil.setContrasenaHash(null);
        return ResponseEntity.ok(perfil);
    }

    /**
     * GET /perfiles/todos — devuelve todos los perfiles (sin hash).
     * Solo para el administrador.
     */
    @GetMapping("/todos")
    public List<Perfil> consultarTodos() {
        return service.consultarTodos();
    }

    /**
     * GET /perfiles/{id} — devuelve el perfil con ese id (sin hash).
     * Usado por la vista de detalle del administrador.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Perfil> buscarPorId(@PathVariable String id) {
        Optional<Perfil> opt = service.buscarPorId(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Perfil perfil = opt.get();
        perfil.setContrasenaHash(null);
        return ResponseEntity.ok(perfil);
    }

    /**
     * POST /perfiles/manual — crea un huesped manualmente.
     * Cuerpo JSON: { "nombre", "email", "telefono", "cedula" (opcional) }
     * Respuesta: { "perfil": Perfil, "contrasenaProvisional": String }
     */
    @PostMapping("/manual")
    public ResponseEntity<Map<String, Object>> crearManualmente(
            @RequestBody CrearHuespedRequest request) {
        Map<String, Object> resultado = service.crearManualmente(
                request.nombre(), request.email(), request.telefono(), request.cedula());
        return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
    }

    /**
     * PATCH /perfiles/{id}/notas — actualiza las notas internas del staff.
     * Cuerpo JSON: { "notasInternas": "texto" }
     */
    @PatchMapping("/{id}/notas")
    public ResponseEntity<Perfil> actualizarNotas(
            @PathVariable String id,
            @RequestBody Map<String, String> cuerpo) {
        Perfil actualizado = service.actualizarNotas(id, cuerpo.get("notasInternas"));
        actualizado.setContrasenaHash(null);
        return ResponseEntity.ok(actualizado);
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

    /** Cuerpo de la peticion de creacion manual de huesped. */
    public record CrearHuespedRequest(String nombre, String email,
                                      String telefono, String cedula) {}
}
