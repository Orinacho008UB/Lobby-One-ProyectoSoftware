package com.lobbyone.reservas;

import java.time.LocalDate;
import java.util.List;

/**
 * Cuerpo de la peticion para cotizar una reserva (POST /reservas/cotizar).
 * No persiste nada; solo calcula el desglose y el monto total.
 */
public record CotizarRequest(
        String perfilId,
        String habitacionId,
        LocalDate fechaEntrada,
        LocalDate fechaSalida,
        List<String> serviciosIds,
        String ofertaId) {
}
