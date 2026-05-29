package com.lobbyone.reservas;

import com.lobbyone.dataaccesscomponent.DataAccessComponent;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Repositorio de reservas. Persiste via DataAccessComponent en
 * {@code data/reservas.json} (un archivo por entidad).
 */
@Repository
public class ReservasRepository {

    private static final String ARCHIVO = "reservas.json";

    private final DataAccessComponent dataAccess;

    public ReservasRepository(DataAccessComponent dataAccess) {
        this.dataAccess = dataAccess;
    }

    public List<Reserva> buscarTodas() {
        return dataAccess.leerLista(ARCHIVO, Reserva.class);
    }

    public Optional<Reserva> buscarPorId(String id) {
        return buscarTodas().stream()
                .filter(r -> r.getId() != null && r.getId().equals(id))
                .findFirst();
    }

    /**
     * Devuelve todas las reservas de una habitacion (usado para el chequeo de
     * solapamiento de fechas).
     */
    public List<Reserva> buscarPorHabitacion(String habitacionId) {
        if (habitacionId == null) {
            return List.of();
        }
        return buscarTodas().stream()
                .filter(r -> habitacionId.equals(r.getHabitacionId()))
                .collect(Collectors.toList());
    }

    /**
     * Inserta la reserva (o la reemplaza si ya existe una con el mismo id) y
     * persiste la lista completa.
     */
    public Reserva guardar(Reserva reserva) {
        List<Reserva> reservas = buscarTodas();
        reservas.removeIf(r -> r.getId() != null && r.getId().equals(reserva.getId()));
        reservas.add(reserva);
        dataAccess.guardarLista(ARCHIVO, reservas);
        return reserva;
    }
}
