package com.lobbyone.perfiles;

import com.lobbyone.common.ValidacionException;
import com.lobbyone.securitycomponent.SecurityComponent;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
    private static final java.security.SecureRandom SECURE_RANDOM = new java.security.SecureRandom();

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

    /**
     * Busca un perfil por su id. Lo usan otros modulos (p.ej. reservas) para
     * confirmar que el cliente esta registrado.
     */
    public Optional<Perfil> buscarPorId(String id) {
        return repository.buscarPorId(id);
    }

    /**
     * Busca un perfil por email. Usado por el admin para crear reservas a nombre
     * de un huesped registrado.
     *
     * @throws ValidacionException si no existe ningun perfil con ese email.
     */
    public Perfil buscarPorEmail(String email) {
        return repository.buscarPorEmail(email)
                .orElseThrow(() -> new ValidacionException(
                        Map.of("email", "No se encontro ningun huesped con ese email.")));
    }

    /**
     * Devuelve todos los perfiles sin el hash de contrasena.
     * Solo para uso del administrador.
     */
    public List<Perfil> consultarTodos() {
        return repository.buscarTodos().stream()
                .peek(p -> p.setContrasenaHash(null))
                .collect(Collectors.toList());
    }

    /**
     * Crea un huesped manualmente (canal offline).
     *
     * Valida: nombre, email (obligatorio, formato valido, unico) y telefono.
     * Cedula es opcional. Genera una contrasena provisional aleatoria, la hashea
     * y la devuelve UNA sola vez en la respuesta junto al perfil creado.
     *
     * @return mapa con dos claves: "perfil" (Perfil sin hash) y
     *         "contrasenaProvisional" (String en texto plano, para comunicar al huesped).
     */
    public Map<String, Object> crearManualmente(String nombre, String email,
                                                String telefono, String cedula) {
        Map<String, String> errores = new LinkedHashMap<>();

        if (esVacio(nombre)) {
            errores.put("nombre", "El nombre es obligatorio.");
        }
        if (esVacio(email)) {
            errores.put("email", "El email es obligatorio.");
        } else if (!EMAIL.matcher(email).matches()) {
            errores.put("email", "El formato del email no es valido.");
        } else if (repository.buscarPorEmail(email).isPresent()) {
            errores.put("email", "Ya existe un huesped registrado con ese email.");
        }
        if (esVacio(telefono)) {
            errores.put("telefono", "El telefono es obligatorio.");
        }

        if (!errores.isEmpty()) {
            throw new ValidacionException(errores);
        }

        // Generar contrasena provisional: "Lobby" + 4 digitos aleatorios.
        String contrasenaProvisional = "Lobby" + String.format("%04d", SECURE_RANDOM.nextInt(10000));

        Perfil perfil = new Perfil();
        perfil.setId(UUID.randomUUID().toString());
        perfil.setNombre(nombre);
        perfil.setEmail(email);
        perfil.setTelefono(telefono);
        perfil.setCedula(cedula);
        perfil.setContrasenaHash(security.hashear(contrasenaProvisional));
        perfil.setRol(Perfil.Rol.CLIENTE);

        repository.guardar(perfil);

        perfil.setContrasenaHash(null);

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("perfil", perfil);
        resultado.put("contrasenaProvisional", contrasenaProvisional);
        return resultado;
    }

    /**
     * Actualiza las notas internas del staff para un perfil dado.
     *
     * @throws ValidacionException si el id no corresponde a ningun perfil.
     */
    public Perfil actualizarNotas(String id, String notasInternas) {
        Perfil perfil = repository.buscarPorId(id)
                .orElseThrow(() -> new ValidacionException(
                        Map.of("id", "No se encontro un perfil con ese id.")));
        perfil.setNotasInternas(notasInternas);
        return repository.guardar(perfil);
    }

    private boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
