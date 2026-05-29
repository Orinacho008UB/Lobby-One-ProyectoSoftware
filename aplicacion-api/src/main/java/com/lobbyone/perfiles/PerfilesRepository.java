package com.lobbyone.perfiles;

import com.lobbyone.dataaccesscomponent.DataAccessComponent;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de perfiles. Persiste via DataAccessComponent en
 * {@code data/perfiles.json} (un archivo por entidad).
 */
@Repository
public class PerfilesRepository {

    private static final String ARCHIVO = "perfiles.json";

    private final DataAccessComponent dataAccess;

    public PerfilesRepository(DataAccessComponent dataAccess) {
        this.dataAccess = dataAccess;
    }

    public List<Perfil> buscarTodos() {
        return dataAccess.leerLista(ARCHIVO, Perfil.class);
    }

    public Optional<Perfil> buscarPorId(String id) {
        return buscarTodos().stream()
                .filter(p -> p.getId() != null && p.getId().equals(id))
                .findFirst();
    }

    public Optional<Perfil> buscarPorEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        return buscarTodos().stream()
                .filter(p -> p.getEmail() != null && p.getEmail().equalsIgnoreCase(email))
                .findFirst();
    }

    /**
     * Inserta el perfil (o lo reemplaza si ya existe uno con el mismo id) y
     * persiste la lista completa.
     */
    public Perfil guardar(Perfil perfil) {
        List<Perfil> perfiles = buscarTodos();
        perfiles.removeIf(p -> p.getId() != null && p.getId().equals(perfil.getId()));
        perfiles.add(perfil);
        dataAccess.guardarLista(ARCHIVO, perfiles);
        return perfil;
    }
}
