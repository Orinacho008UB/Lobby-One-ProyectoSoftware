package com.lobbyone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de la Aplicacion API de Lobby One.
 *
 * Arranca el contexto de Spring Boot. La logica de negocio vive en los
 * packages por feature (reservas, habitaciones, servicios, ofertas, perfiles)
 * y en los componentes transversales.
 */
@SpringBootApplication
public class AplicacionApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AplicacionApiApplication.class, args);
    }
}
