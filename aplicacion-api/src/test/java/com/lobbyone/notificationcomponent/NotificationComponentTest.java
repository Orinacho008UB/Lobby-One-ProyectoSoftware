package com.lobbyone.notificationcomponent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Pruebas del componente de notificaciones. El envio de correo (JavaMailSender)
 * se mockea: no se contacta ningun servidor SMTP real.
 */
@ExtendWith(MockitoExtension.class)
class NotificationComponentTest {

    @Mock
    private JavaMailSender mailSender;

    @Captor
    private ArgumentCaptor<SimpleMailMessage> mensajeCaptor;

    @Test
    void enviarConfirmacionReservaEnviaUnCorreoConLosDatosCorrectos() {
        NotificationComponent notificaciones =
                new NotificationComponent(mailSender, "no-reply@lobbyone.com");

        notificaciones.enviarConfirmacionReserva(
                "cliente@correo.com", "Ana", "RSV-001",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 5));

        verify(mailSender, times(1)).send(mensajeCaptor.capture());
        SimpleMailMessage enviado = mensajeCaptor.getValue();

        assertEquals("no-reply@lobbyone.com", enviado.getFrom());
        assertNotNull(enviado.getTo());
        assertEquals("cliente@correo.com", enviado.getTo()[0]);
        assertEquals("Confirmacion de tu reserva en Lobby One", enviado.getSubject());

        String cuerpo = enviado.getText();
        assertNotNull(cuerpo);
        assertTrue(cuerpo.contains("Ana"), "el cuerpo debe saludar al cliente");
        assertTrue(cuerpo.contains("RSV-001"), "el cuerpo debe incluir el numero de reserva");
        assertTrue(cuerpo.contains("2026-07-01"), "el cuerpo debe incluir la fecha de entrada");
        assertTrue(cuerpo.contains("2026-07-05"), "el cuerpo debe incluir la fecha de salida");
    }
}
