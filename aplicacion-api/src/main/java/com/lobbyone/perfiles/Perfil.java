package com.lobbyone.perfiles;

/**
 * Modelo de dominio: perfil de usuario (cliente o administrador).
 *
 * La contrasena se almacena siempre hasheada (Argon2 via SecurityComponent);
 * nunca en texto plano.
 */
public class Perfil {

    /** Rol del usuario dentro del sistema. */
    public enum Rol {
        CLIENTE,
        ADMINISTRADOR
    }

    private String id;
    private String nombre;
    private String email;
    private String telefono;
    private String cedula;
    private String contrasenaHash;
    private Rol rol;

    /** Constructor sin argumentos requerido por Jackson. */
    public Perfil() {
    }

    public Perfil(String id, String nombre, String email, String telefono, String cedula,
                  String contrasenaHash, Rol rol) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.telefono = telefono;
        this.cedula = cedula;
        this.contrasenaHash = contrasenaHash;
        this.rol = rol;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        this.cedula = cedula;
    }

    public String getContrasenaHash() {
        return contrasenaHash;
    }

    public void setContrasenaHash(String contrasenaHash) {
        this.contrasenaHash = contrasenaHash;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }
}
