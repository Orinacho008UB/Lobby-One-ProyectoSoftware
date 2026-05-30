package com.lobbyone.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuracion transversal de Spring MVC.
 *
 * NO es un componente del modelo C4 (no aparece en el diagrama de Nivel 3): es
 * detalle de infraestructura, igual que el package {@code common} o los DTOs.
 *
 * 1. CORS: habilita que la Single-Page Application (Angular, localhost:4200) llame
 *    a la API en localhost:8080 desde el navegador.
 * 2. Recursos estaticos: sirve las imagenes subidas desde el directorio del
 *    filesystem bajo la URL /images/**.
 */
@Configuration
public class ConfiguracionWeb implements WebMvcConfigurer {

    private static final String ORIGEN_FRONTEND = "http://localhost:4200";

    @Value("${lobbyone.imagenes.ruta-base:images}")
    private String rutaBaseImagenes;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(ORIGEN_FRONTEND)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Sirve las imagenes del filesystem bajo /images/**
        // Ejemplo: GET http://localhost:8080/images/abc123.jpg
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:" + rutaBaseImagenes + "/");
    }
}
