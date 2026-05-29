package com.lobbyone.ofertas;

import com.lobbyone.habitaciones.Habitacion.TipoHabitacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Modelo de dominio: oferta del hotel.
 *
 * Una oferta es un PAQUETE (no un descuento porcentual): asocia un tipo de
 * habitacion con un conjunto de servicios incluidos a un precio cerrado, vigente
 * durante un periodo.
 */
public class Oferta {

    /** Estado de publicacion de la oferta. */
    public enum EstadoOferta {
        ACTIVA,
        INACTIVA
    }

    private String id;
    /** Nombre de la oferta (unico; la unicidad la valida el Service). */
    private String nombre;
    private String descripcion;
    /** Tipo de habitacion al que aplica el paquete. */
    private TipoHabitacion tipoHabitacion;
    /** Ids de los servicios incluidos en el paquete (al menos uno). */
    private List<String> serviciosIncluidos;
    /** Precio cerrado del paquete (mayor que 0). */
    private BigDecimal precio;
    private LocalDate vigenciaDesde;
    private LocalDate vigenciaHasta;
    private EstadoOferta estado;
    /** Imagen principal de la oferta, gestionada por ImageStorageComponent. */
    private String imagenPortada;

    /** Constructor sin argumentos requerido por Jackson. */
    public Oferta() {
    }

    public Oferta(String id, String nombre, String descripcion, TipoHabitacion tipoHabitacion,
                  List<String> serviciosIncluidos, BigDecimal precio, LocalDate vigenciaDesde,
                  LocalDate vigenciaHasta, EstadoOferta estado, String imagenPortada) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.tipoHabitacion = tipoHabitacion;
        this.serviciosIncluidos = serviciosIncluidos;
        this.precio = precio;
        this.vigenciaDesde = vigenciaDesde;
        this.vigenciaHasta = vigenciaHasta;
        this.estado = estado;
        this.imagenPortada = imagenPortada;
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

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public TipoHabitacion getTipoHabitacion() {
        return tipoHabitacion;
    }

    public void setTipoHabitacion(TipoHabitacion tipoHabitacion) {
        this.tipoHabitacion = tipoHabitacion;
    }

    public List<String> getServiciosIncluidos() {
        return serviciosIncluidos;
    }

    public void setServiciosIncluidos(List<String> serviciosIncluidos) {
        this.serviciosIncluidos = serviciosIncluidos;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public LocalDate getVigenciaDesde() {
        return vigenciaDesde;
    }

    public void setVigenciaDesde(LocalDate vigenciaDesde) {
        this.vigenciaDesde = vigenciaDesde;
    }

    public LocalDate getVigenciaHasta() {
        return vigenciaHasta;
    }

    public void setVigenciaHasta(LocalDate vigenciaHasta) {
        this.vigenciaHasta = vigenciaHasta;
    }

    public EstadoOferta getEstado() {
        return estado;
    }

    public void setEstado(EstadoOferta estado) {
        this.estado = estado;
    }

    public String getImagenPortada() {
        return imagenPortada;
    }

    public void setImagenPortada(String imagenPortada) {
        this.imagenPortada = imagenPortada;
    }
}
