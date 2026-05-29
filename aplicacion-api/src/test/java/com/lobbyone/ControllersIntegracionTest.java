package com.lobbyone;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lobbyone.notificationcomponent.NotificationComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integracion de los controllers REST (extremo a extremo) con
 * {@code @SpringBootTest} + {@code MockMvc}.
 *
 * Aislamiento:
 *  - Las rutas base de datos e imagenes se redirigen a un directorio temporal
 *    (estatico + {@link DynamicPropertySource}); nunca se tocan data/ ni images/.
 *  - Antes de cada test se limpian los JSON del almacen para que las aserciones
 *    sean deterministas (DataAccessComponent lee del archivo en cada llamada).
 *  - {@link NotificationComponent} se mockea con {@code @MockBean}: no se contacta
 *    ningun SMTP real; en el flujo de reserva se verifica que el envio se invoque.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ControllersIntegracionTest {

    /** Cabecera PNG valida (89 50 4E 47 0D 0A 1A 0A) para pasar la validacion de magic bytes. */
    private static final byte[] PNG = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    @TempDir
    static Path baseTemporal;

    @DynamicPropertySource
    static void rutasTemporales(DynamicPropertyRegistry registry) {
        registry.add("lobbyone.data.ruta-base", () -> baseTemporal.resolve("data").toString());
        registry.add("lobbyone.imagenes.ruta-base", () -> baseTemporal.resolve("images").toString());
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NotificationComponent notificationComponent;

    @Value("${lobbyone.data.ruta-base}")
    private String rutaDatos;

    /** Limpia los JSON del almacen temporal antes de cada test. */
    @BeforeEach
    void limpiarAlmacen() throws IOException {
        Path datos = Path.of(rutaDatos);
        if (Files.exists(datos)) {
            try (Stream<Path> archivos = Files.list(datos)) {
                archivos.filter(p -> p.toString().endsWith(".json")).forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
    }

    // =====================================================================
    // Perfiles
    // =====================================================================

    @Test
    @DisplayName("POST /perfiles -> 201 y la respuesta no expone contrasenaHash")
    void registrarPerfilDevuelve201SinExponerHash() throws Exception {
        MvcResult res = mockMvc.perform(post("/perfiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(perfilJson("ana@correo.com", "claveSegura1")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andReturn();

        assertNoExponeHash(res);
    }

    @Test
    @DisplayName("POST /perfiles -> 400 con email duplicado")
    void registrarPerfilEmailDuplicadoDevuelve400() throws Exception {
        mockMvc.perform(post("/perfiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(perfilJson("dup@correo.com", "claveSegura1")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/perfiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(perfilJson("dup@correo.com", "otraClave9")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.email").exists());
    }

    @Test
    @DisplayName("POST /perfiles -> 400 con contrasena corta")
    void registrarPerfilContrasenaCortaDevuelve400() throws Exception {
        mockMvc.perform(post("/perfiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(perfilJson("corta@correo.com", "1234")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.contrasena").exists());
    }

    @Test
    @DisplayName("POST /perfiles/login -> 200 con credenciales validas y sin exponer hash")
    void loginCorrectoDevuelve200() throws Exception {
        mockMvc.perform(post("/perfiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(perfilJson("login@correo.com", "claveSegura1")))
                .andExpect(status().isCreated());

        MvcResult res = mockMvc.perform(post("/perfiles/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"login@correo.com\",\"contrasena\":\"claveSegura1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("login@correo.com"))
                .andReturn();

        assertNoExponeHash(res);
    }

    @Test
    @DisplayName("POST /perfiles/login -> 401 con credenciales invalidas")
    void loginCredencialesInvalidasDevuelve401() throws Exception {
        mockMvc.perform(post("/perfiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(perfilJson("mallogin@correo.com", "claveSegura1")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/perfiles/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"mallogin@correo.com\",\"contrasena\":\"incorrecta9\"}"))
                .andExpect(status().isUnauthorized());
    }

    // =====================================================================
    // Habitaciones
    // =====================================================================

    @Test
    @DisplayName("POST /habitaciones (multipart) -> 201")
    void registrarHabitacionDevuelve201() throws Exception {
        mockMvc.perform(multipart("/habitaciones")
                        .file(parteJson("habitacion", habitacionJson(101, "SUITE", 100.0, 2, "DISPONIBLE")))
                        .file(parteImagen()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.imagenPortada").exists())
                .andExpect(jsonPath("$.numero").value(101));
    }

    @Test
    @DisplayName("POST /habitaciones -> 400 con datos invalidos")
    void registrarHabitacionInvalidaDevuelve400() throws Exception {
        // numero <= 0 y precio negativo
        mockMvc.perform(multipart("/habitaciones")
                        .file(parteJson("habitacion", habitacionJson(0, "SUITE", -5.0, 2, "DISPONIBLE")))
                        .file(parteImagen()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.numero").exists())
                .andExpect(jsonPath("$.errores.precioPorNoche").exists());
    }

    @Test
    @DisplayName("GET /habitaciones con y sin filtros")
    void consultarHabitacionesConFiltros() throws Exception {
        crearHabitacion(201, "SUITE", 100.0, 2, "DISPONIBLE");
        crearHabitacion(202, "DOBLE", 80.0, 4, "MANTENIMIENTO");

        mockMvc.perform(get("/habitaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/habitaciones").param("estado", "DISPONIBLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].numero").value(201));

        mockMvc.perform(get("/habitaciones").param("tipo", "DOBLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].numero").value(202));

        mockMvc.perform(get("/habitaciones").param("capacidad", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].numero").value(202));
    }

    // =====================================================================
    // Servicios
    // =====================================================================

    @Test
    @DisplayName("POST /servicios (multipart) -> 201")
    void crearServicioDevuelve201() throws Exception {
        mockMvc.perform(multipart("/servicios")
                        .file(parteJson("servicio", servicioJson("Spa", "Bienestar", 50.0, "ACTIVO")))
                        .file(parteImagen()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.nombre").value("Spa"));
    }

    @Test
    @DisplayName("POST /servicios -> 400 con datos invalidos")
    void crearServicioInvalidoDevuelve400() throws Exception {
        // precio <= 0 -> 400
        mockMvc.perform(multipart("/servicios")
                        .file(parteJson("servicio", servicioJson("Gimnasio", "Bienestar", 0.0, "ACTIVO")))
                        .file(parteImagen()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.precio").exists());
    }

    @Test
    @DisplayName("GET /servicios -> solo los ACTIVOS")
    void consultarServiciosSoloActivos() throws Exception {
        crearServicio("Spa", "Bienestar", 50.0, "ACTIVO");
        crearServicio("Sauna", "Bienestar", 30.0, "INACTIVO");

        mockMvc.perform(get("/servicios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Spa"));
    }

    // =====================================================================
    // Ofertas
    // =====================================================================

    @Test
    @DisplayName("POST /ofertas (multipart) -> 201")
    void crearOfertaDevuelve201() throws Exception {
        crearHabitacion(301, "SUITE", 120.0, 2, "DISPONIBLE");
        String servicioId = crearServicio("Desayuno", "Gastronomia", 25.0, "ACTIVO");

        mockMvc.perform(multipart("/ofertas")
                        .file(parteJson("oferta", ofertaJson("Paquete Suite", "SUITE",
                                List.of(servicioId), 150.0, LocalDate.now(), LocalDate.now().plusMonths(2), "ACTIVA")))
                        .file(parteImagen()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.nombre").value("Paquete Suite"));
    }

    @Test
    @DisplayName("POST /ofertas -> 400 con servicio incluido inactivo")
    void crearOfertaServicioInactivoDevuelve400() throws Exception {
        crearHabitacion(302, "SUITE", 120.0, 2, "DISPONIBLE");
        String servicioInactivo = crearServicio("Tour", "Experiencias", 40.0, "INACTIVO");

        mockMvc.perform(multipart("/ofertas")
                        .file(parteJson("oferta", ofertaJson("Paquete Tour", "SUITE",
                                List.of(servicioInactivo), 150.0, LocalDate.now(), LocalDate.now().plusMonths(2), "ACTIVA")))
                        .file(parteImagen()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.serviciosIncluidos").exists());
    }

    @Test
    @DisplayName("POST /ofertas -> 400 con tipo de habitacion inexistente en el catalogo")
    void crearOfertaTipoInexistenteDevuelve400() throws Exception {
        // Hay un servicio activo pero NO existe ninguna habitacion DOBLE en el catalogo.
        String servicioId = crearServicio("Masaje", "Bienestar", 60.0, "ACTIVO");

        mockMvc.perform(multipart("/ofertas")
                        .file(parteJson("oferta", ofertaJson("Paquete Doble", "DOBLE",
                                List.of(servicioId), 150.0, LocalDate.now(), LocalDate.now().plusMonths(2), "ACTIVA")))
                        .file(parteImagen()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.tipoHabitacion").exists());
    }

    @Test
    @DisplayName("POST /ofertas -> 400 con vigencias invalidas")
    void crearOfertaFechasInvalidasDevuelve400() throws Exception {
        crearHabitacion(303, "SUITE", 120.0, 2, "DISPONIBLE");
        String servicioId = crearServicio("Cena", "Gastronomia", 70.0, "ACTIVO");

        // vigenciaHasta anterior a vigenciaDesde
        mockMvc.perform(multipart("/ofertas")
                        .file(parteJson("oferta", ofertaJson("Paquete Malo", "SUITE",
                                List.of(servicioId), 150.0, LocalDate.now().plusDays(10), LocalDate.now().plusDays(5), "ACTIVA")))
                        .file(parteImagen()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.vigenciaHasta").exists());
    }

    @Test
    @DisplayName("GET /ofertas -> solo las ACTIVAS")
    void consultarOfertasSoloActivas() throws Exception {
        crearHabitacion(304, "SUITE", 120.0, 2, "DISPONIBLE");
        String servicioId = crearServicio("Piscina", "Bienestar", 20.0, "ACTIVO");
        crearOferta("Oferta Activa", "SUITE", List.of(servicioId), 150.0, "ACTIVA");
        crearOferta("Oferta Inactiva", "SUITE", List.of(servicioId), 160.0, "INACTIVA");

        mockMvc.perform(get("/ofertas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Oferta Activa"));
    }

    // =====================================================================
    // Reservas
    // =====================================================================

    @Test
    @DisplayName("GET /reservas/disponibilidad -> habitaciones disponibles para las fechas")
    void disponibilidadDevuelveHabitaciones() throws Exception {
        crearHabitacion(401, "SUITE", 100.0, 2, "DISPONIBLE");
        LocalDate entrada = LocalDate.now().plusDays(10);
        LocalDate salida = entrada.plusDays(3);

        mockMvc.perform(get("/reservas/disponibilidad")
                        .param("fechaEntrada", entrada.toString())
                        .param("fechaSalida", salida.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].numero").value(401));
    }

    @Test
    @DisplayName("POST /reservas/cotizar -> calcula el total y NO persiste")
    void cotizarNoPersiste() throws Exception {
        String perfilId = crearPerfil("cotiza@correo.com", "claveSegura1");
        String habitacionId = crearHabitacion(402, "SUITE", 100.0, 2, "DISPONIBLE");
        String servicioId = crearServicio("Spa", "Bienestar", 50.0, "ACTIVO");
        LocalDate entrada = LocalDate.now().plusDays(10);
        LocalDate salida = entrada.plusDays(3); // 3 noches

        MvcResult res = mockMvc.perform(post("/reservas/cotizar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cotizarJson(perfilId, habitacionId, entrada, salida, List.of(servicioId), null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noches").value(3))
                .andReturn();

        // 3 noches x 100 = 300 + servicio 50 = 350
        assertEquals(0, new BigDecimal("350.00").compareTo(montoTotal(res)));
        // No debe haberse creado el archivo de reservas.
        assertTrue(Files.notExists(Path.of(rutaDatos, "reservas.json")),
                "cotizar no debe persistir reservas");
    }

    @Test
    @DisplayName("POST /reservas -> 201, persiste e invoca NotificationComponent")
    void crearReservaDevuelve201EInvocaNotificacion() throws Exception {
        String perfilId = crearPerfil("reserva@correo.com", "claveSegura1");
        String habitacionId = crearHabitacion(403, "SUITE", 100.0, 2, "DISPONIBLE");
        LocalDate entrada = LocalDate.now().plusDays(10);
        LocalDate salida = entrada.plusDays(2); // 2 noches

        mockMvc.perform(post("/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(crearReservaJson(perfilId, habitacionId, entrada, salida,
                                List.of(), null, "reserva@correo.com")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.estado").value("ACTIVA"));

        // 2 noches x 100 = 200, sin servicios
        assertTrue(Files.exists(Path.of(rutaDatos, "reservas.json")), "la reserva debe persistirse");
        verify(notificationComponent).enviarConfirmacionReserva(
                eq("reserva@correo.com"), anyString(), anyString(), eq(entrada), eq(salida));
    }

    // =====================================================================
    // Flujo extremo a extremo
    // =====================================================================

    @Test
    @DisplayName("E2E: perfil -> habitacion -> servicio -> oferta -> disponibilidad -> cotizar -> reservar")
    void flujoCompletoDeReserva() throws Exception {
        // 1) Registrar perfil (cliente).
        String perfilId = crearPerfil("e2e@correo.com", "claveSegura1");

        // 2) Registrar habitacion SUITE disponible a 100/noche.
        String habitacionId = crearHabitacion(501, "SUITE", 100.0, 2, "DISPONIBLE");

        // 3) Crear servicio activo.
        String servicioId = crearServicio("Spa", "Bienestar", 50.0, "ACTIVO");

        // 4) Crear oferta (paquete) SUITE a 150/noche con el spa incluido.
        crearOferta("Paquete E2E", "SUITE", List.of(servicioId), 150.0, "ACTIVA");
        String ofertaId = idDeOfertaActiva();

        LocalDate entrada = LocalDate.now().plusDays(15);
        LocalDate salida = entrada.plusDays(3); // 3 noches

        // 5) Consultar disponibilidad aplicando la oferta (fuerza tipo SUITE).
        mockMvc.perform(get("/reservas/disponibilidad")
                        .param("fechaEntrada", entrada.toString())
                        .param("fechaSalida", salida.toString())
                        .param("ofertaId", ofertaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].numero").value(501));

        // 6) Cotizar: 150/noche x 3 = 450; el spa va en el bundle (no se cobra) -> 450.
        MvcResult cotizacion = mockMvc.perform(post("/reservas/cotizar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cotizarJson(perfilId, habitacionId, entrada, salida, List.of(servicioId), ofertaId)))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals(0, new BigDecimal("450.00").compareTo(montoTotal(cotizacion)));

        // 7) Crear la reserva: 201, ACTIVA, montoTotal 450, correo enviado.
        MvcResult creacion = mockMvc.perform(post("/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(crearReservaJson(perfilId, habitacionId, entrada, salida,
                                List.of(servicioId), ofertaId, "e2e@correo.com")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ACTIVA"))
                .andReturn();

        assertEquals(0, new BigDecimal("450.00").compareTo(montoTotal(creacion)));
        verify(notificationComponent).enviarConfirmacionReserva(
                eq("e2e@correo.com"), anyString(), anyString(), eq(entrada), eq(salida));
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private String perfilJson(String email, String contrasena) throws Exception {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("nombre", "Cliente Prueba");
        p.put("email", email);
        p.put("telefono", "04140000000");
        p.put("cedula", "V12345678");
        p.put("contrasenaHash", contrasena); // en el registro viaja la contrasena en texto plano
        return objectMapper.writeValueAsString(p);
    }

    private String habitacionJson(int numero, String tipo, double precio, int capacidad, String estado) throws Exception {
        Map<String, Object> h = new LinkedHashMap<>();
        h.put("numero", numero);
        h.put("piso", 1);
        h.put("tipo", tipo);
        h.put("configuracionCamas", "1 cama king");
        h.put("capacidad", capacidad);
        h.put("tamanoM2", 35.0);
        h.put("precioPorNoche", precio);
        h.put("estado", estado);
        return objectMapper.writeValueAsString(h);
    }

    private String servicioJson(String nombre, String categoria, double precio, String estado) throws Exception {
        Map<String, Object> horario = new LinkedHashMap<>();
        horario.put("dia", "LUNES");
        horario.put("horaInicio", "09:00");
        horario.put("horaFin", "18:00");
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("nombre", nombre);
        s.put("categoria", categoria);
        s.put("descripcionCorta", "Servicio de prueba");
        s.put("unidadCobro", "por persona");
        s.put("precio", precio);
        s.put("estado", estado);
        s.put("disponibilidadHorarios", List.of(horario));
        return objectMapper.writeValueAsString(s);
    }

    private String ofertaJson(String nombre, String tipoHabitacion, List<String> serviciosIncluidos,
                              double precio, LocalDate desde, LocalDate hasta, String estado) throws Exception {
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("nombre", nombre);
        o.put("descripcion", "Oferta de prueba");
        o.put("tipoHabitacion", tipoHabitacion);
        o.put("serviciosIncluidos", serviciosIncluidos);
        o.put("precio", precio);
        o.put("vigenciaDesde", desde.toString());
        o.put("vigenciaHasta", hasta.toString());
        o.put("estado", estado);
        return objectMapper.writeValueAsString(o);
    }

    private String cotizarJson(String perfilId, String habitacionId, LocalDate entrada, LocalDate salida,
                               List<String> serviciosIds, String ofertaId) throws Exception {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("perfilId", perfilId);
        c.put("habitacionId", habitacionId);
        c.put("fechaEntrada", entrada.toString());
        c.put("fechaSalida", salida.toString());
        c.put("serviciosIds", serviciosIds);
        c.put("ofertaId", ofertaId);
        return objectMapper.writeValueAsString(c);
    }

    private String crearReservaJson(String perfilId, String habitacionId, LocalDate entrada, LocalDate salida,
                                    List<String> serviciosIds, String ofertaId, String emailContacto) throws Exception {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("perfilId", perfilId);
        r.put("habitacionId", habitacionId);
        r.put("fechaEntrada", entrada.toString());
        r.put("fechaSalida", salida.toString());
        r.put("serviciosIds", serviciosIds);
        r.put("ofertaId", ofertaId);
        r.put("emailContacto", emailContacto);
        return objectMapper.writeValueAsString(r);
    }

    private MockMultipartFile parteJson(String nombre, String json) {
        return new MockMultipartFile(nombre, nombre + ".json",
                MediaType.APPLICATION_JSON_VALUE, json.getBytes(StandardCharsets.UTF_8));
    }

    private MockMultipartFile parteImagen() {
        return new MockMultipartFile("imagenPortada", "portada.png", MediaType.IMAGE_PNG_VALUE, PNG);
    }

    private String crearPerfil(String email, String contrasena) throws Exception {
        MvcResult res = mockMvc.perform(post("/perfiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(perfilJson(email, contrasena)))
                .andExpect(status().isCreated())
                .andReturn();
        return idDe(res);
    }

    private String crearHabitacion(int numero, String tipo, double precio, int capacidad, String estado) throws Exception {
        MvcResult res = mockMvc.perform(multipart("/habitaciones")
                        .file(parteJson("habitacion", habitacionJson(numero, tipo, precio, capacidad, estado)))
                        .file(parteImagen()))
                .andExpect(status().isCreated())
                .andReturn();
        return idDe(res);
    }

    private String crearServicio(String nombre, String categoria, double precio, String estado) throws Exception {
        MvcResult res = mockMvc.perform(multipart("/servicios")
                        .file(parteJson("servicio", servicioJson(nombre, categoria, precio, estado)))
                        .file(parteImagen()))
                .andExpect(status().isCreated())
                .andReturn();
        return idDe(res);
    }

    private void crearOferta(String nombre, String tipo, List<String> serviciosIncluidos,
                             double precio, String estado) throws Exception {
        mockMvc.perform(multipart("/ofertas")
                        .file(parteJson("oferta", ofertaJson(nombre, tipo, serviciosIncluidos, precio,
                                LocalDate.now(), LocalDate.now().plusMonths(2), estado)))
                        .file(parteImagen()))
                .andExpect(status().isCreated());
    }

    /** Devuelve el id de la (unica) oferta activa registrada (para el flujo E2E). */
    private String idDeOfertaActiva() throws Exception {
        MvcResult res = mockMvc.perform(get("/ofertas"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get(0).get("id").asText();
    }

    private String idDe(MvcResult res) throws Exception {
        return objectMapper.readTree(res.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("id").asText();
    }

    private BigDecimal montoTotal(MvcResult res) throws Exception {
        return objectMapper.readTree(res.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("montoTotal").decimalValue();
    }

    /** Verifica que la respuesta no exponga el hash de la contrasena (ni clave presente con valor). */
    private void assertNoExponeHash(MvcResult res) throws Exception {
        var nodo = objectMapper.readTree(res.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("contrasenaHash");
        // Aceptable: la clave no aparece, o aparece como null. Nunca un hash real.
        if (nodo != null) {
            assertTrue(nodo.isNull(), "La respuesta no debe exponer contrasenaHash");
        } else {
            assertNull(nodo);
        }
    }
}
