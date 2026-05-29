package com.lobbyone.servicios;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

/**
 * Modelo de dominio: servicio contratable del hotel (spa, desayuno, traslado, etc.).
 */
public class Servicio {

    /** Estado de publicacion del servicio. */
    public enum EstadoServicio {
        ACTIVO,
        INACTIVO
    }

    /**
     * Franja horaria de disponibilidad: un dia y un rango horario valido.
     */
    public static class Horario {
        private String dia;
        private LocalTime horaInicio;
        private LocalTime horaFin;

        public Horario() {
        }

        public Horario(String dia, LocalTime horaInicio, LocalTime horaFin) {
            this.dia = dia;
            this.horaInicio = horaInicio;
            this.horaFin = horaFin;
        }

        public String getDia() {
            return dia;
        }

        public void setDia(String dia) {
            this.dia = dia;
        }

        public LocalTime getHoraInicio() {
            return horaInicio;
        }

        public void setHoraInicio(LocalTime horaInicio) {
            this.horaInicio = horaInicio;
        }

        public LocalTime getHoraFin() {
            return horaFin;
        }

        public void setHoraFin(LocalTime horaFin) {
            this.horaFin = horaFin;
        }
    }

    private String id;
    private String nombre;
    private String descripcionCorta;
    private String categoria;
    /** Unidad sobre la que se cobra (p.ej. "por persona", "por noche", "por uso"). */
    private String unidadCobro;
    private BigDecimal precio;
    private EstadoServicio estado;
    /** Franjas de disponibilidad (al menos un dia con un horario valido). */
    private List<Horario> disponibilidadHorarios;
    /** Imagen principal del servicio, gestionada por ImageStorageComponent. */
    private String imagenPortada;

    // --- Atributos opcionales ---
    private String descripcionDetallada;
    private String ubicacion;
    private Integer capacidadMaxima;
    private String requisitosRestricciones;
    private List<String> imagenesAdicionales;

    /** Constructor sin argumentos requerido por Jackson. */
    public Servicio() {
    }

    public Servicio(String id, String nombre, String descripcionCorta, String categoria, String unidadCobro,
                    BigDecimal precio, EstadoServicio estado, List<Horario> disponibilidadHorarios,
                    String imagenPortada, String descripcionDetallada, String ubicacion, Integer capacidadMaxima,
                    String requisitosRestricciones, List<String> imagenesAdicionales) {
        this.id = id;
        this.nombre = nombre;
        this.descripcionCorta = descripcionCorta;
        this.categoria = categoria;
        this.unidadCobro = unidadCobro;
        this.precio = precio;
        this.estado = estado;
        this.disponibilidadHorarios = disponibilidadHorarios;
        this.imagenPortada = imagenPortada;
        this.descripcionDetallada = descripcionDetallada;
        this.ubicacion = ubicacion;
        this.capacidadMaxima = capacidadMaxima;
        this.requisitosRestricciones = requisitosRestricciones;
        this.imagenesAdicionales = imagenesAdicionales;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcionCorta() {
        return descripcionCorta;
    }

    public void setDescripcionCorta(String descripcionCorta) {
        this.descripcionCorta = descripcionCorta;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getUnidadCobro() {
        return unidadCobro;
    }

    public void setUnidadCobro(String unidadCobro) {
        this.unidadCobro = unidadCobro;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public EstadoServicio getEstado() {
        return estado;
    }

    public void setEstado(EstadoServicio estado) {
        this.estado = estado;
    }

    public List<Horario> getDisponibilidadHorarios() {
        return disponibilidadHorarios;
    }

    public void setDisponibilidadHorarios(List<Horario> disponibilidadHorarios) {
        this.disponibilidadHorarios = disponibilidadHorarios;
    }

    public String getImagenPortada() {
        return imagenPortada;
    }

    public void setImagenPortada(String imagenPortada) {
        this.imagenPortada = imagenPortada;
    }

    public String getDescripcionDetallada() {
        return descripcionDetallada;
    }

    public void setDescripcionDetallada(String descripcionDetallada) {
        this.descripcionDetallada = descripcionDetallada;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }

    public Integer getCapacidadMaxima() {
        return capacidadMaxima;
    }

    public void setCapacidadMaxima(Integer capacidadMaxima) {
        this.capacidadMaxima = capacidadMaxima;
    }

    public String getRequisitosRestricciones() {
        return requisitosRestricciones;
    }

    public void setRequisitosRestricciones(String requisitosRestricciones) {
        this.requisitosRestricciones = requisitosRestricciones;
    }

    public List<String> getImagenesAdicionales() {
        return imagenesAdicionales;
    }

    public void setImagenesAdicionales(List<String> imagenesAdicionales) {
        this.imagenesAdicionales = imagenesAdicionales;
    }
}
