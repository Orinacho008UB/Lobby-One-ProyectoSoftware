package com.lobbyone.ofertas;

import com.lobbyone.dataaccesscomponent.DataAccessComponent;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de ofertas. Persiste via DataAccessComponent en
 * {@code data/ofertas.json} (un archivo por entidad).
 */
@Repository
public class OfertasRepository {

    private static final String ARCHIVO = "ofertas.json";

    private final DataAccessComponent dataAccess;

    public OfertasRepository(DataAccessComponent dataAccess) {
        this.dataAccess = dataAccess;
    }

    public List<Oferta> buscarTodas() {
        return dataAccess.leerLista(ARCHIVO, Oferta.class);
    }

    public Optional<Oferta> buscarPorId(String id) {
        return buscarTodas().stream()
                .filter(o -> o.getId() != null && o.getId().equals(id))
                .findFirst();
    }

    public Optional<Oferta> buscarPorNombre(String nombre) {
        if (nombre == null) {
            return Optional.empty();
        }
        return buscarTodas().stream()
                .filter(o -> nombre.equalsIgnoreCase(o.getNombre()))
                .findFirst();
    }

    /**
     * Inserta la oferta (o la reemplaza si ya existe una con el mismo id) y
     * persiste la lista completa.
     */
    public Oferta guardar(Oferta oferta) {
        List<Oferta> ofertas = buscarTodas();
        ofertas.removeIf(o -> o.getId() != null && o.getId().equals(oferta.getId()));
        ofertas.add(oferta);
        dataAccess.guardarLista(ARCHIVO, ofertas);
        return oferta;
    }
}
