package com.lobbyone.notificationcomponent;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Componente transversal de notificaciones.
 *
 * Envia correos via SMTP (Mailtrap en desarrollo) usando Spring Mail.
 * Usado por el modulo de reservas.
 *
 * Recibe datos simples (no el modelo Reserva) para no invertir la dependencia
 * del diagrama: reservas depende de notificationcomponent, no al reves.
 */
@Component
public class NotificationComponent {

    private final JavaMailSender mailSender;
    private final String remitente;

    public NotificationComponent(JavaMailSender mailSender,
                                 @Value("${lobbyone.notificaciones.remitente:no-reply@lobbyone.com}") String remitente) {
        this.mailSender = mailSender;
        this.remitente = remitente;
    }

    /**
     * Envia el correo de confirmacion de una reserva recien creada.
     *
     * @param destinatario  email del cliente.
     * @param nombreCliente nombre del cliente para el saludo.
     * @param idReserva     identificador de la reserva.
     * @param fechaEntrada  fecha de entrada (check-in).
     * @param fechaSalida   fecha de salida (check-out).
     */
    public void enviarConfirmacionReserva(String destinatario, String nombreCliente, String idReserva,
                                          LocalDate fechaEntrada, LocalDate fechaSalida) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(destinatario);
        mensaje.setSubject("Confirmacion de tu reserva en Lobby One");
        mensaje.setText(
                "Hola " + nombreCliente + ",\n\n"
                        + "Tu reserva ha sido confirmada.\n\n"
                        + "Numero de reserva: " + idReserva + "\n"
                        + "Fecha de entrada: " + fechaEntrada + "\n"
                        + "Fecha de salida: " + fechaSalida + "\n\n"
                        + "Gracias por elegir Lobby One.");
        mailSender.send(mensaje);
    }
}
