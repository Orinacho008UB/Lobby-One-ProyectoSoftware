package com.lobbyone.common;

import java.util.Map;

/**
 * Excepcion de validacion de negocio compartida por los modulos.
 *
 * Acumula los errores por campo ({@code campo -> mensaje}) para devolverlos
 * todos a la vez. Vive en {@code common} por ser infraestructura transversal
 * (no es un componente del C4).
 */
public class ValidacionException extends RuntimeException {

    private final Map<String, String> errores;

    public ValidacionException(Map<String, String> errores) {
        super("Existen errores de validacion.");
        this.errores = errores;
    }

    public Map<String, String> getErrores() {
        return errores;
    }
}
