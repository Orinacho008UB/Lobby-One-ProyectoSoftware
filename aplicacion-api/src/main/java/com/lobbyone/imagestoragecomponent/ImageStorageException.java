package com.lobbyone.imagestoragecomponent;

/**
 * Excepcion no verificada lanzada cuando falla la lectura o escritura de una
 * imagen en el Almacen de Imagenes.
 */
public class ImageStorageException extends RuntimeException {

    public ImageStorageException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
