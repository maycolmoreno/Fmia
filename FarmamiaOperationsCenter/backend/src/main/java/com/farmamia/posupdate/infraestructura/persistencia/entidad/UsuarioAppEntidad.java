package com.farmamia.posupdate.infraestructura.persistencia.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "app_users")
public class UsuarioAppEntidad {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "username", nullable = false, unique = true, length = 80)
    private String usuario;

    @Column(name = "password_hash", nullable = false)
    private String hashContrasena;

    @Column(name = "full_name", nullable = false, length = 160)
    private String nombreCompleto;

    @Column(name = "email", length = 180)
    private String correo;

    @Column(name = "role", nullable = false, length = 40)
    private String rol;

    @Column(name = "is_active", nullable = false)
    private boolean activo;

    @Column(name = "failed_login_attempts", nullable = false)
    private int intentosFallidosLogin;

    @Column(name = "locked_until")
    private OffsetDateTime bloqueadoHasta;

    @Column(name = "last_login_at")
    private OffsetDateTime ultimoAccesoEn;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime actualizadoEn;

    protected UsuarioAppEntidad() {
    }

    public UsuarioAppEntidad(
        String usuario,
        String hashContrasena,
        String nombreCompleto,
        String correo,
        String rol,
        boolean activo
    ) {
        this.usuario = usuario;
        this.hashContrasena = hashContrasena;
        this.nombreCompleto = nombreCompleto;
        this.correo = correo;
        this.rol = rol;
        this.activo = activo;
    }

    public UUID getId() {
        return id;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getHashContrasena() {
        return hashContrasena;
    }

    public void cambiarHashContrasena(String hashContrasena) {
        this.hashContrasena = hashContrasena;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public String getCorreo() {
        return correo;
    }

    public String getRol() {
        return rol;
    }

    public boolean isActivo() {
        return activo;
    }

    public int getIntentosFallidosLogin() {
        return intentosFallidosLogin;
    }

    public OffsetDateTime getBloqueadoHasta() {
        return bloqueadoHasta;
    }

    public OffsetDateTime getUltimoAccesoEn() {
        return ultimoAccesoEn;
    }

    public OffsetDateTime getCreadoEn() {
        return creadoEn;
    }

    public OffsetDateTime getActualizadoEn() {
        return actualizadoEn;
    }

    public boolean estaBloqueado() {
        return bloqueadoHasta != null && bloqueadoHasta.isAfter(OffsetDateTime.now());
    }

    public void registrarLoginCorrecto() {
        this.intentosFallidosLogin = 0;
        this.bloqueadoHasta = null;
        this.ultimoAccesoEn = OffsetDateTime.now();
    }

    public boolean registrarLoginFallido(int maxIntentos, java.time.Duration duracionBloqueo) {
        this.intentosFallidosLogin++;
        if (this.intentosFallidosLogin >= maxIntentos) {
            this.bloqueadoHasta = OffsetDateTime.now().plus(duracionBloqueo);
            return true;
        }
        return false;
    }

    public void actualizarPerfil(String nombreCompleto, String correo) {
        this.nombreCompleto = nombreCompleto;
        this.correo = correo;
    }

    public void cambiarRol(String rol) {
        this.rol = rol;
    }

    public void activar() {
        this.activo = true;
    }

    public void desactivar() {
        this.activo = false;
    }
}
