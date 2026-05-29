package com.lobbyone.perfiles;

import com.lobbyone.common.ValidacionException;
import com.lobbyone.securitycomponent.SecurityComponent;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Logica de negocio de perfiles. Alcance Sprint 1: registrar y login.
 *
 * Nota de Sprint 1: el campo {@code contrasenaHash} del Perfil recibido en
 * {@link #registrar} transporta la contrasena en TEXTO PLANO; el service la
 * valida y la reemplaza por su hash Argon2 antes de persistir.
 */
@Service
public class PerfilesService {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int LONGITUD_MINIMA_CONTRASENA = 8;

    private final PerfilesRepository repository;
    private final SecurityComponent security;

    public PerfilesService(PerfilesRepository repository, SecurityComponent security) {
        this.repository = repository;
        this.security = security;
    }

    /**
     * Registra un nuevo perfil con rol CLIENTE.
     *
     * Valida: campos obligatorios, formato de email, email unico y longitud
     * minima de contrasena. Hashea la contrasena con SecurityComponent.
     *
     * @throws ValidacionException con un mensaje por cada campo invalido.
     */
    public Perfil registrar(Perfil perfil) {
        Map<String, String> errores = new LinkedHashMap<>();

        if (esVacio(perfil.getNombre())) {
            errores.put("nombre", "El nombre es obligatorio.");
        }

        String email = perfil.getEmail();
        if (esVacio(email)) {
            errores.put("email", "El email es obligatorio.");
        } else if (!EMAIL.matcher(email).matches()) {
            errores.put("email", "El email no tiene un formato valido.");
        } else if (repository.buscarPorEmail(email).isPresent()) {
            errores.put("email", "Ya existe un perfil registrado con ese email.");
        }

        if (esVacio(perfil.getTelefono())) {
            errores.put("telefono", "El telefono es obligatorio.");
        }

        if (esVacio(perfil.getCedula())) {
            errores.put("cedula", "La cedula es obligatoria.");
        }

        // En el registro, contrasenaHash trae la contrasena en texto plano.
        String contrasenaPlana = perfil.getContrasenaHash();
        if (esVacio(contrasenaPlana)) {
            errores.put("contrasena", "La contrasena es obligatoria.");
        } else if (contrasenaPlana.length() < LONGITUD_MINIMA_CONTRASENA) {
            errores.put("contrasena", "La contrasena debe tener al menos " + LONGITUD_MINIMA_CONTRASENA + " caracteres.");
        }

        if (!errores.isEmpty()) {
            throw new ValidacionException(errores);
        }

        perfil.setId(UUID.randomUUID().toString());
        perfil.setRol(Perfil.Rol.CLIENTE);
        perfil.setContrasenaHash(security.hashear(contrasenaPlana));
        return repository.guardar(perfil);
    }

    /**
     * Valida las credenciales y devuelve el perfil autenticado.
     *
     * @throws CredencialesInvalidasException si el email no existe o la
     *                                        contrasena no coincide.
     */
    public Perfil login(String email, String contrasena) {
        Optional<Perfil> encontrado = repository.buscarPorEmail(email);
        if (encontrado.isEmpty() || esVacio(contrasena)
                || !security.validar(contrasena, encontrado.get().getContrasenaHash())) {
            throw new CredencialesInvalidasException("Email o contrasena incorrectos.");
        }
        return encontrado.get();
    }

    private boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
