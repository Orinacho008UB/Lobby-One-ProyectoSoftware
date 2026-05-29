package com.lobbyone.reservas;

import java.time.LocalDate;
import java.util.List;

/**
 * Cuerpo de la peticion para confirmar y crear una reserva (POST /reservas).
 */
public record CrearReservaRequest(
        String perfilId,
        String habitacionId,
        LocalDate fechaEntrada,
        LocalDate fechaSalida,
        List<String> serviciosIds,
        String ofertaId,
        String emailContacto) {
}
