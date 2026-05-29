package com.lobbyone.dataaccesscomponent;

/**
 * Excepcion no verificada lanzada cuando falla la lectura o escritura de un
 * archivo JSON del Almacen de Datos.
 */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
