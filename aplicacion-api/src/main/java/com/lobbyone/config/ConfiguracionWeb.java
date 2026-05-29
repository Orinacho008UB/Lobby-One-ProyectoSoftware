package com.lobbyone.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuracion transversal de Spring MVC.
 *
 * NO es un componente del modelo C4 (no aparece en el diagrama de Nivel 3): es
 * detalle de infraestructura, igual que el package {@code common} o los DTOs.
 *
 * Habilita CORS para que la Single-Page Application (Angular, servida en
 * http://localhost:4200 con {@code ng serve}) pueda llamar a la API en
 * localhost:8080 desde el navegador.
 */
@Configuration
public class ConfiguracionWeb implements WebMvcConfigurer {

    private static final String ORIGEN_FRONTEND = "http://localhost:4200";

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(ORIGEN_FRONTEND)
                // Sprint 1 solo usa creacion/registro y consultas.
                .allowedMethods("GET", "POST", "OPTIONS")
                // Todos los headers (incluye los de multipart/form-data en los uploads).
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
