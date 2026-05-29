package com.lobbyone.reservas;

import com.lobbyone.common.ValidacionException;
import com.lobbyone.habitaciones.Habitacion;
import com.lobbyone.habitaciones.Habitacion.TipoHabitacion;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST de reservas. Expone los endpoints y delega en el Service.
 * Alcance Sprint 1: buscar disponibilidad, cotizar y crear (confirmar) reserva.
 */
@RestController
@RequestMapping("/reservas")
public class ReservasController {

    private final ReservasService service;

    public ReservasController(ReservasService service) {
        this.service = service;
    }

    /**
     * GET /reservas/disponibilidad — habitaciones disponibles para el rango de
     * fechas, con filtros opcionales de tipo, capacidad y oferta.
     */
    @GetMapping("/disponibilidad")
    public List<Habitacion> disponibilidad(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaEntrada,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaSalida,
            @RequestParam(required = false) TipoHabitacion tipo,
            @RequestParam(required = false) Integer capacidad,
            @RequestParam(required = false) String ofertaId) {
        return service.buscarDisponibles(fechaEntrada, fechaSalida, tipo, capacidad, ofertaId);
    }

    /** POST /reservas/cotizar — calcula el desglose y el total sin persistir. */
    @PostMapping("/cotizar")
    public DesgloseReserva cotizar(@RequestBody CotizarRequest peticion) {
        return service.cotizar(peticion.perfilId(), peticion.habitacionId(),
                peticion.fechaEntrada(), peticion.fechaSalida(),
                peticion.serviciosIds(), peticion.ofertaId());
    }

    /** POST /reservas — confirma y crea la reserva (persiste y envia el correo). */
    @PostMapping
    public ResponseEntity<Reserva> crear(@RequestBody CrearReservaRequest peticion) {
        Reserva creada = service.crear(peticion.perfilId(), peticion.habitacionId(),
                peticion.fechaEntrada(), peticion.fechaSalida(),
                peticion.serviciosIds(), peticion.ofertaId(), peticion.emailContacto());
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @ExceptionHandler(ValidacionException.class)
    public ResponseEntity<Map<String, Object>> manejarValidacion(ValidacionException ex) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("mensaje", "Hay errores de validacion.");
        cuerpo.put("errores", ex.getErrores());
        return ResponseEntity.badRequest().body(cuerpo);
    }
}
