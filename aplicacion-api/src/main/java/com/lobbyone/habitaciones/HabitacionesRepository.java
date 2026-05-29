package com.lobbyone.habitaciones;

import com.lobbyone.dataaccesscomponent.DataAccessComponent;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de habitaciones. Persiste via DataAccessComponent en
 * {@code data/habitaciones.json} (un archivo por entidad).
 */
@Repository
public class HabitacionesRepository {

    private static final String ARCHIVO = "habitaciones.json";

    private final DataAccessComponent dataAccess;

    public HabitacionesRepository(DataAccessComponent dataAccess) {
        this.dataAccess = dataAccess;
    }

    public List<Habitacion> buscarTodas() {
        return dataAccess.leerLista(ARCHIVO, Habitacion.class);
    }

    public Optional<Habitacion> buscarPorId(String id) {
        return buscarTodas().stream()
                .filter(h -> h.getId() != null && h.getId().equals(id))
                .findFirst();
    }

    public Optional<Habitacion> buscarPorNumero(int numero) {
        return buscarTodas().stream()
                .filter(h -> h.getNumero() == numero)
                .findFirst();
    }

    /**
     * Inserta la habitacion (o la reemplaza si ya existe una con el mismo id) y
     * persiste la lista completa.
     */
    public Habitacion guardar(Habitacion habitacion) {
        List<Habitacion> habitaciones = buscarTodas();
        habitaciones.removeIf(h -> h.getId() != null && h.getId().equals(habitacion.getId()));
        habitaciones.add(habitacion);
        dataAccess.guardarLista(ARCHIVO, habitaciones);
        return habitacion;
    }
}
