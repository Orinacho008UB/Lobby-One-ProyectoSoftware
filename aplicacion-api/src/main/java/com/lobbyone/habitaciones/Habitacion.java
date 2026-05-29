package com.lobbyone.habitaciones;

import java.math.BigDecimal;
import java.util.List;

/**
 * Modelo de dominio: habitacion del hotel.
 */
public class Habitacion {

    /** Tipo de habitacion del catalogo. */
    public enum TipoHabitacion {
        INDIVIDUAL,
        DOBLE,
        SUITE
    }

    /** Estado operativo de la habitacion. */
    public enum EstadoHabitacion {
        DISPONIBLE,
        MANTENIMIENTO,
        FUERA_DE_SERVICIO,
        OCUPADA
    }

    private String id;
    private int numero;
    /** Piso (obligatorio por presencia; admite 0/negativos, sin restriccion de rango). */
    private Integer piso;
    private TipoHabitacion tipo;
    private String descripcion;
    private String configuracionCamas;
    private int capacidad;
    private double tamanoM2;
    private BigDecimal precioPorNoche;
    private EstadoHabitacion estado;
    /** Imagen principal (obligatoria), gestionada por ImageStorageComponent. */
    private String imagenPortada;
    /** Imagenes adicionales (opcional). */
    private List<String> imagenesAdicionales;
    /** Amenidades de la habitacion (opcional). */
    private List<String> amenidades;

    /** Constructor sin argumentos requerido por Jackson. */
    public Habitacion() {
    }

    public Habitacion(String id, int numero, Integer piso, TipoHabitacion tipo, String descripcion,
                      String configuracionCamas, int capacidad, double tamanoM2, BigDecimal precioPorNoche,
                      EstadoHabitacion estado, String imagenPortada, List<String> imagenesAdicionales,
                      List<String> amenidades) {
        this.id = id;
        this.numero = numero;
        this.piso = piso;
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.configuracionCamas = configuracionCamas;
        this.capacidad = capacidad;
        this.tamanoM2 = tamanoM2;
        this.precioPorNoche = precioPorNoche;
        this.estado = estado;
        this.imagenPortada = imagenPortada;
        this.imagenesAdicionales = imagenesAdicionales;
        this.amenidades = amenidades;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public Integer getPiso() {
        return piso;
    }

    public void setPiso(Integer piso) {
        this.piso = piso;
    }

    public TipoHabitacion getTipo() {
        return tipo;
    }

    public void setTipo(TipoHabitacion tipo) {
        this.tipo = tipo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getConfiguracionCamas() {
        return configuracionCamas;
    }

    public void setConfiguracionCamas(String configuracionCamas) {
        this.configuracionCamas = configuracionCamas;
    }

    public int getCapacidad() {
        return capacidad;
    }

    public void setCapacidad(int capacidad) {
        this.capacidad = capacidad;
    }

    public double getTamanoM2() {
        return tamanoM2;
    }

    public void setTamanoM2(double tamanoM2) {
        this.tamanoM2 = tamanoM2;
    }

    public BigDecimal getPrecioPorNoche() {
        return precioPorNoche;
    }

    public void setPrecioPorNoche(BigDecimal precioPorNoche) {
        this.precioPorNoche = precioPorNoche;
    }

    public EstadoHabitacion getEstado() {
        return estado;
    }

    public void setEstado(EstadoHabitacion estado) {
        this.estado = estado;
    }

    public String getImagenPortada() {
        return imagenPortada;
    }

    public void setImagenPortada(String imagenPortada) {
        this.imagenPortada = imagenPortada;
    }

    public List<String> getImagenesAdicionales() {
        return imagenesAdicionales;
    }

    public void setImagenesAdicionales(List<String> imagenesAdicionales) {
        this.imagenesAdicionales = imagenesAdicionales;
    }

    public List<String> getAmenidades() {
        return amenidades;
    }

    public void setAmenidades(List<String> amenidades) {
        this.amenidades = amenidades;
    }
}
