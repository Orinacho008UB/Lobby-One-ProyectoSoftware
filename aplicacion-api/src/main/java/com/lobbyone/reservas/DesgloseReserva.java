package com.lobbyone.reservas;

import java.math.BigDecimal;
import java.util.List;

/**
 * Desglose de la cotizacion de una reserva (no se persiste). Detalla el
 * alojamiento (habitacion o paquete) por noche, los servicios adicionales que
 * se cobran y el monto total.
 *
 * @param noches              numero de noches (salida exclusiva).
 * @param concepto            descripcion del alojamiento (habitacion o paquete).
 * @param precioPorNoche      precio por noche aplicado (de la habitacion o de la oferta).
 * @param subtotalAlojamiento precioPorNoche x noches.
 * @param serviciosAdicionales servicios cobrados aparte (los del bundle no se incluyen).
 * @param subtotalServicios   suma de los servicios adicionales (cobro plano).
 * @param montoTotal          subtotalAlojamiento + subtotalServicios.
 */
public record DesgloseReserva(
        long noches,
        String concepto,
        BigDecimal precioPorNoche,
        BigDecimal subtotalAlojamiento,
        List<LineaServicio> serviciosAdicionales,
        BigDecimal subtotalServicios,
        BigDecimal montoTotal) {

    /** Linea de un servicio adicional cobrado en la reserva. */
    public record LineaServicio(String servicioId, String nombre, BigDecimal precio) {
    }
}
