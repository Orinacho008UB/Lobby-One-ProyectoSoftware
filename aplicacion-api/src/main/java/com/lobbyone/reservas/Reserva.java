package com.lobbyone.reservas;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Modelo de dominio: reserva de una habitacion por un cliente, con servicios
 * contratados y, opcionalmente, una oferta aplicada.
 *
 * Las referencias a otras entidades se guardan por id (las entidades viven en
 * sus propios archivos JSON).
 */
public class Reserva {

    /** Estado del ciclo de vida de la reserva. */
    public enum EstadoReserva {
        ACTIVA,
        MODIFICADA,
        CANCELADA
    }

    private String id;
    /** Id del perfil (cliente) que realiza la reserva. */
    private String perfilId;
    /** Id de la habitacion reservada. */
    private String habitacionId;
    /** Ids de los servicios contratados junto con la estadia. */
    private List<String> serviciosContratados;
    /** Id de la oferta aplicada (puede ser null si no hay oferta). */
    private String ofertaAplicada;
    private LocalDate fechaEntrada;
    private LocalDate fechaSalida;
    private EstadoReserva estado;
    private BigDecimal montoTotal;
    /** Email al que se envian las notificaciones de la reserva. */
    private String emailContacto;
    private LocalDate fechaCreacion;

    /** Constructor sin argumentos requerido por Jackson. */
    public Reserva() {
    }

    public Reserva(String id, String perfilId, String habitacionId, List<String> serviciosContratados,
                   String ofertaAplicada, LocalDate fechaEntrada, LocalDate fechaSalida,
                   EstadoReserva estado, BigDecimal montoTotal, String emailContacto, LocalDate fechaCreacion) {
        this.id = id;
        this.perfilId = perfilId;
        this.habitacionId = habitacionId;
        this.serviciosContratados = serviciosContratados;
        this.ofertaAplicada = ofertaAplicada;
        this.fechaEntrada = fechaEntrada;
        this.fechaSalida = fechaSalida;
        this.estado = estado;
        this.montoTotal = montoTotal;
        this.emailContacto = emailContacto;
        this.fechaCreacion = fechaCreacion;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPerfilId() {
        return perfilId;
    }

    public void setPerfilId(String perfilId) {
        this.perfilId = perfilId;
    }

    public String getHabitacionId() {
        return habitacionId;
    }

    public void setHabitacionId(String habitacionId) {
        this.habitacionId = habitacionId;
    }

    public List<String> getServiciosContratados() {
        return serviciosContratados;
    }

    public void setServiciosContratados(List<String> serviciosContratados) {
        this.serviciosContratados = serviciosContratados;
    }

    public String getOfertaAplicada() {
        return ofertaAplicada;
    }

    public void setOfertaAplicada(String ofertaAplicada) {
        this.ofertaAplicada = ofertaAplicada;
    }

    public LocalDate getFechaEntrada() {
        return fechaEntrada;
    }

    public void setFechaEntrada(LocalDate fechaEntrada) {
        this.fechaEntrada = fechaEntrada;
    }

    public LocalDate getFechaSalida() {
        return fechaSalida;
    }

    public void setFechaSalida(LocalDate fechaSalida) {
        this.fechaSalida = fechaSalida;
    }

    public EstadoReserva getEstado() {
        return estado;
    }

    public void setEstado(EstadoReserva estado) {
        this.estado = estado;
    }

    public BigDecimal getMontoTotal() {
        return montoTotal;
    }

    public void setMontoTotal(BigDecimal montoTotal) {
        this.montoTotal = montoTotal;
    }

    public String getEmailContacto() {
        return emailContacto;
    }

    public void setEmailContacto(String emailContacto) {
        this.emailContacto = emailContacto;
    }

    public LocalDate getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDate fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}
