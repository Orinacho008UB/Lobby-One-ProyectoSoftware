package com.lobbyone.securitycomponent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del componente de seguridad (hashing Argon2).
 */
class SecurityComponentTest {

    private SecurityComponent security;

    @BeforeEach
    void inicializar() {
        security = new SecurityComponent();
    }

    @Test
    void hashearProduceUnHashArgon2DistintoDelTextoPlano() {
        String hash = security.hashear("Secreta123");

        assertNotNull(hash);
        assertNotEquals("Secreta123", hash);
        assertTrue(hash.startsWith("$argon2"), "el hash debe tener el prefijo de Argon2");
    }

    @Test
    void validarDevuelveTrueConLaContrasenaCorrecta() {
        String hash = security.hashear("Secreta123");

        assertTrue(security.validar("Secreta123", hash));
    }

    @Test
    void validarDevuelveFalseConLaContrasenaIncorrecta() {
        String hash = security.hashear("Secreta123");

        assertFalse(security.validar("OtraClave456", hash));
    }

    @Test
    void dosHashesDeLaMismaContrasenaSonDistintosPeroAmbosValidan() {
        String hash1 = security.hashear("Secreta123");
        String hash2 = security.hashear("Secreta123");

        assertNotEquals(hash1, hash2, "la sal aleatoria debe producir hashes distintos");
        assertTrue(security.validar("Secreta123", hash1));
        assertTrue(security.validar("Secreta123", hash2));
    }
}
