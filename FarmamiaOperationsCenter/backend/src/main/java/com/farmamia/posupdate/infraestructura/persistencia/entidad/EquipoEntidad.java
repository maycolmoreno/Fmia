package com.farmamia.posupdate.infraestructura.persistencia.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "devices")
public class EquipoEntidad {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
    private SucursalEntidad sucursal;

    @Column(name = "hostname", nullable = false, unique = true, length = 120)
    private String nombreEquipo;

    @Column(name = "ip_address", length = 45)
    private String direccionIp;

    @Column(name = "mac_address", unique = true, length = 32)
    private String direccionMac;

    @Column(name = "windows_version", length = 120)
    private String versionWindows;

    @Column(name = "agent_version", length = 40)
    private String versionAgente;

    @Column(name = "pos_version", length = 40)
    private String versionPos;

    @Column(name = "pos_path", nullable = false)
    private String rutaPos;

    @Column(name = "status", nullable = false, length = 40)
    private String estado = "REGISTERED";

    @Column(name = "last_heartbeat_at")
    private OffsetDateTime ultimoLatidoEn;

    @CreationTimestamp
    @Column(name = "registered_at", nullable = false, updatable = false)
    private OffsetDateTime registradoEn;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime actualizadoEn;

    protected EquipoEntidad() {
    }

    public EquipoEntidad(SucursalEntidad sucursal, String nombreEquipo, String rutaPos) {
        this.sucursal = sucursal;
        this.nombreEquipo = nombreEquipo;
        this.rutaPos = rutaPos;
    }

    public UUID getId() {
        return id;
    }

    public String getNombreEquipo() {
        return nombreEquipo;
    }

    public SucursalEntidad getSucursal() {
        return sucursal;
    }

    public String getDireccionIp() {
        return direccionIp;
    }

    public String getDireccionMac() {
        return direccionMac;
    }

    public String getVersionWindows() {
        return versionWindows;
    }

    public String getVersionAgente() {
        return versionAgente;
    }

    public String getVersionPos() {
        return versionPos;
    }

    public String getRutaPos() {
        return rutaPos;
    }

    public String getEstado() {
        return estado;
    }

    public OffsetDateTime getUltimoLatidoEn() {
        return ultimoLatidoEn;
    }

    public OffsetDateTime getRegistradoEn() {
        return registradoEn;
    }

    public OffsetDateTime getActualizadoEn() {
        return actualizadoEn;
    }

    public void actualizarRegistro(
        SucursalEntidad sucursal,
        String direccionIp,
        String direccionMac,
        String versionWindows,
        String versionAgente,
        String versionPos,
        String rutaPos
    ) {
        this.sucursal = sucursal;
        this.direccionIp = blancoANulo(direccionIp);
        this.direccionMac = blancoANulo(direccionMac);
        this.versionWindows = blancoANulo(versionWindows);
        this.versionAgente = blancoANulo(versionAgente);
        this.versionPos = blancoANulo(versionPos);
        this.rutaPos = rutaPos;
        this.estado = "REGISTERED";
    }

    public void registrarLatido(String versionPos) {
        this.versionPos = blancoANulo(versionPos);
        this.estado = "ONLINE";
        this.ultimoLatidoEn = OffsetDateTime.now();
    }

    public void actualizarVersionPos(String versionPos) {
        this.versionPos = blancoANulo(versionPos);
    }

    private String blancoANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor;
    }
}
