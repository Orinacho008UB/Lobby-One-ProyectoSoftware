package com.lobbyone.habitaciones;

import com.lobbyone.habitaciones.Habitacion.EstadoHabitacion;
import com.lobbyone.habitaciones.Habitacion.TipoHabitacion;

/**
 * Filtros opcionales para consultar habitaciones. Un campo en {@code null}
 * significa "no filtrar por ese criterio".
 *
 * @param estado    estado exacto a buscar (p.ej. DISPONIBLE).
 * @param tipo      tipo de habitacion a buscar.
 * @param capacidad capacidad minima requerida (devuelve habitaciones que alojen
 *                  al menos esa cantidad de personas).
 */
public record FiltroHabitaciones(EstadoHabitacion estado, TipoHabitacion tipo, Integer capacidad) {
}
