package com.lobbyone.securitycomponent;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Componente transversal de seguridad.
 *
 * Hashea contrasenas con Argon2 (Spring Security Crypto, respaldado por Bouncy
 * Castle) y valida credenciales. Usado por el modulo de perfiles.
 *
 * El hash incluye su propia sal aleatoria, por lo que dos hashes de la misma
 * contrasena son distintos pero ambos validan correctamente.
 */
@Component
public class SecurityComponent {

    private final Argon2PasswordEncoder encoder;

    public SecurityComponent() {
        // Parametros recomendados por Spring Security para Argon2.
        this.encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    /**
     * Devuelve el hash Argon2 de la contrasena en texto plano.
     */
    public String hashear(String contrasenaPlana) {
        return encoder.encode(contrasenaPlana);
    }

    /**
     * Indica si la contrasena en texto plano corresponde al hash almacenado.
     */
    public boolean validar(String contrasenaPlana, String hashAlmacenado) {
        return encoder.matches(contrasenaPlana, hashAlmacenado);
    }
}
