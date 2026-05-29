package com.lobbyone.perfiles;

import com.lobbyone.common.ValidacionException;
import com.lobbyone.securitycomponent.SecurityComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de la logica de negocio de perfiles (registrar y login).
 * El repositorio se mockea; SecurityComponent es real para ejercitar el
 * hashing y la validacion de credenciales de extremo a extremo.
 */
@ExtendWith(MockitoExtension.class)
class PerfilesServiceTest {

    @Mock
    private PerfilesRepository repository;

    private PerfilesService service;
    private SecurityComponent security;

    @BeforeEach
    void inicializar() {
        security = new SecurityComponent();
        service = new PerfilesService(repository, security);
    }

    private Perfil perfilValidoParaRegistro() {
        Perfil p = new Perfil();
        p.setNombre("Ana Perez");
        p.setEmail("ana@correo.com");
        p.setTelefono("04141234567");
        p.setCedula("V-12345678");
        p.setContrasenaHash("claveSegura1"); // contrasena en texto plano al registrar
        return p;
    }

    @Test
    void registrarPerfilValidoLoHasheaLeAsignaIdYRolCliente() {
        when(repository.buscarPorEmail("ana@correo.com")).thenReturn(Optional.empty());
        when(repository.guardar(any(Perfil.class))).thenAnswer(inv -> inv.getArgument(0));

        Perfil resultado = service.registrar(perfilValidoParaRegistro());

        assertNotNull(resultado.getId());
        assertEquals(Perfil.Rol.CLIENTE, resultado.getRol());
        assertNotEquals("claveSegura1", resultado.getContrasenaHash());
        assertTrue(resultado.getContrasenaHash().startsWith("$argon2"));
        assertTrue(security.validar("claveSegura1", resultado.getContrasenaHash()));
        verify(repository).guardar(any(Perfil.class));
    }

    @Test
    void registrarConEmailDuplicadoLanzaValidacion() {
        when(repository.buscarPorEmail("ana@correo.com"))
                .thenReturn(Optional.of(new Perfil()));

        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> service.registrar(perfilValidoParaRegistro()));

        assertTrue(ex.getErrores().containsKey("email"));
        verify(repository, never()).guardar(any());
    }

    @Test
    void registrarConEmailDeFormatoInvalidoLanzaValidacion() {
        Perfil p = perfilValidoParaRegistro();
        p.setEmail("correo-invalido");

        ValidacionException ex = assertThrows(ValidacionException.class, () -> service.registrar(p));

        assertTrue(ex.getErrores().containsKey("email"));
        verify(repository, never()).guardar(any());
    }

    @Test
    void registrarConContrasenaCortaLanzaValidacion() {
        when(repository.buscarPorEmail("ana@correo.com")).thenReturn(Optional.empty());
        Perfil p = perfilValidoParaRegistro();
        p.setContrasenaHash("corta");

        ValidacionException ex = assertThrows(ValidacionException.class, () -> service.registrar(p));

        assertTrue(ex.getErrores().containsKey("contrasena"));
        verify(repository, never()).guardar(any());
    }

    @Test
    void registrarConCamposObligatoriosVaciosReportaCadaCampo() {
        Perfil vacio = new Perfil(); // todo nulo

        ValidacionException ex = assertThrows(ValidacionException.class, () -> service.registrar(vacio));

        assertTrue(ex.getErrores().containsKey("nombre"));
        assertTrue(ex.getErrores().containsKey("email"));
        assertTrue(ex.getErrores().containsKey("telefono"));
        assertTrue(ex.getErrores().containsKey("cedula"));
        assertTrue(ex.getErrores().containsKey("contrasena"));
        verify(repository, never()).guardar(any());
    }

    @Test
    void loginConCredencialesCorrectasDevuelveElPerfil() {
        Perfil almacenado = new Perfil();
        almacenado.setId("1");
        almacenado.setEmail("ana@correo.com");
        almacenado.setContrasenaHash(security.hashear("claveSegura1"));
        when(repository.buscarPorEmail("ana@correo.com")).thenReturn(Optional.of(almacenado));

        Perfil resultado = service.login("ana@correo.com", "claveSegura1");

        assertEquals("1", resultado.getId());
    }

    @Test
    void loginConEmailInexistenteLanzaCredencialesInvalidas() {
        when(repository.buscarPorEmail("nadie@correo.com")).thenReturn(Optional.empty());

        assertThrows(CredencialesInvalidasException.class,
                () -> service.login("nadie@correo.com", "claveSegura1"));
    }

    @Test
    void loginConContrasenaIncorrectaLanzaCredencialesInvalidas() {
        Perfil almacenado = new Perfil();
        almacenado.setEmail("ana@correo.com");
        almacenado.setContrasenaHash(security.hashear("claveSegura1"));
        when(repository.buscarPorEmail("ana@correo.com")).thenReturn(Optional.of(almacenado));

        assertThrows(CredencialesInvalidasException.class,
                () -> service.login("ana@correo.com", "claveEquivocada"));
    }
}
