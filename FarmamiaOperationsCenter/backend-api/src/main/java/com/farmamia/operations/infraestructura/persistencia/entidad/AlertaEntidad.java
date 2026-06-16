package com.farmamia.operations.infraestructura.persistencia.entidad;

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

@Entity
@Table(name = "alerts")
public class AlertaEntidad {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private EquipoEntidad equipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private SucursalEntidad sucursal;

    @Column(name = "severity", nullable = false, length = 20)
    private String severidad;

    @Column(name = "alert_type", nullable = false, length = 60)
    private String tipoAlerta;

    @Column(name = "title", nullable = false, length = 180)
    private String titulo;

    @Column(name = "message")
    private String mensaje;

    @Column(name = "status", nullable = false, length = 30)
    private String estado = "OPEN";

    @Column(name = "network_event", nullable = false)
    private boolean eventoDeRed;

    @Column(name = "branch_code_red", length = 30)
    private String codigoSucursalRed;

    @CreationTimestamp
    @Column(name = "opened_at", nullable = false, updatable = false)
    private OffsetDateTime abiertaEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acknowledged_by")
    private UsuarioAppEntidad reconocidaPor;

    @Column(name = "acknowledged_at")
    private OffsetDateTime reconocidaEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closed_by")
    private UsuarioAppEntidad cerradaPor;

    @Column(name = "closed_at")
    private OffsetDateTime cerradaEn;

    protected AlertaEntidad() {
    }

    public AlertaEntidad(EquipoEntidad equipo, String severidad, String tipoAlerta, String titulo, String mensaje) {
        this.equipo = equipo;
        this.severidad = severidad;
        this.tipoAlerta = tipoAlerta;
        this.titulo = titulo;
        this.mensaje = mensaje;
    }

    public AlertaEntidad(SucursalEntidad sucursal, String severidad, String tipoAlerta, String titulo, String mensaje) {
        this.sucursal = sucursal;
        this.severidad = severidad;
        this.tipoAlerta = tipoAlerta;
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.eventoDeRed = true;
        this.codigoSucursalRed = sucursal != null ? sucursal.getCodigo() : null;
    }

    public UUID getId() {
        return id;
    }

    public EquipoEntidad getEquipo() {
        return equipo;
    }

    public SucursalEntidad getSucursal() {
        return sucursal;
    }

    public boolean isEventoDeRed() {
        return eventoDeRed;
    }

    public String getCodigoSucursalRed() {
        return codigoSucursalRed;
    }

    public String getSeveridad() {
        return severidad;
    }

    public String getTipoAlerta() {
        return tipoAlerta;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public String getEstado() {
        return estado;
    }

    public OffsetDateTime getAbiertaEn() {
        return abiertaEn;
    }

    public UsuarioAppEntidad getReconocidaPor() {
        return reconocidaPor;
    }

    public OffsetDateTime getReconocidaEn() {
        return reconocidaEn;
    }

    public UsuarioAppEntidad getCerradaPor() {
        return cerradaPor;
    }

    public OffsetDateTime getCerradaEn() {
        return cerradaEn;
    }

    public void reconocer(UsuarioAppEntidad usuario) {
        if ("CLOSED".equals(estado)) {
            throw new IllegalArgumentException("No se puede reconocer una alerta cerrada.");
        }
        this.estado = "ACKNOWLEDGED";
        this.reconocidaPor = usuario;
        this.reconocidaEn = OffsetDateTime.now();
    }

    public void cerrar(UsuarioAppEntidad usuario) {
        this.estado = "CLOSED";
        this.cerradaPor = usuario;
        this.cerradaEn = OffsetDateTime.now();
        if (this.reconocidaPor == null) {
            this.reconocidaPor = usuario;
            this.reconocidaEn = this.cerradaEn;
        }
    }
}
