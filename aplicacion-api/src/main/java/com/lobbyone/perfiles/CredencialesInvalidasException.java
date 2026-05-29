package com.lobbyone.perfiles;

/**
 * Excepcion lanzada cuando el login falla (email inexistente o contrasena
 * incorrecta). Se usa un mensaje generico para no revelar cual de los dos fallo.
 */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException(String mensaje) {
        super(mensaje);
    }
}
