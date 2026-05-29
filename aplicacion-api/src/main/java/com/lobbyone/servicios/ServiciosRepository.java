package com.lobbyone.servicios;

import com.lobbyone.dataaccesscomponent.DataAccessComponent;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de servicios. Persiste via DataAccessComponent en
 * {@code data/servicios.json} (un archivo por entidad).
 */
@Repository
public class ServiciosRepository {

    private static final String ARCHIVO = "servicios.json";

    private final DataAccessComponent dataAccess;

    public ServiciosRepository(DataAccessComponent dataAccess) {
        this.dataAccess = dataAccess;
    }

    public List<Servicio> buscarTodos() {
        return dataAccess.leerLista(ARCHIVO, Servicio.class);
    }

    public Optional<Servicio> buscarPorId(String id) {
        return buscarTodos().stream()
                .filter(s -> s.getId() != null && s.getId().equals(id))
                .findFirst();
    }

    /**
     * Busca un servicio por nombre dentro de una categoria (ambos sin distinguir
     * mayusculas/minusculas). Sirve para validar la unicidad del nombre por categoria.
     */
    public Optional<Servicio> buscarPorNombreYCategoria(String nombre, String categoria) {
        if (nombre == null || categoria == null) {
            return Optional.empty();
        }
        return buscarTodos().stream()
                .filter(s -> nombre.equalsIgnoreCase(s.getNombre())
                        && categoria.equalsIgnoreCase(s.getCategoria()))
                .findFirst();
    }

    /**
     * Inserta el servicio (o lo reemplaza si ya existe uno con el mismo id) y
     * persiste la lista completa.
     */
    public Servicio guardar(Servicio servicio) {
        List<Servicio> servicios = buscarTodos();
        servicios.removeIf(s -> s.getId() != null && s.getId().equals(servicio.getId()));
        servicios.add(servicio);
        dataAccess.guardarLista(ARCHIVO, servicios);
        return servicio;
    }
}
