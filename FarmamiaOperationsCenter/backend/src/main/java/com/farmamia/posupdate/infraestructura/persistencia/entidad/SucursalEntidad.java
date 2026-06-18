package com.farmamia.posupdate.infraestructura.persistencia.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "branches")
public class SucursalEntidad {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "code", nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(name = "name", nullable = false, length = 160)
    private String nombre;

    @Column(name = "city", length = 120)
    private String ciudad;

    @Column(name = "zone", length = 120)
    private String zona;

    @Column(name = "address")
    private String direccion;

    @Column(name = "is_on_duty", nullable = false)
    private boolean deTurno;

    @Column(name = "is_active", nullable = false)
    private boolean activa = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "grupo_trx_id")
    private GrupoTrxEntidad grupoTrx;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime actualizadoEn;

    protected SucursalEntidad() {
    }

    public SucursalEntidad(String codigo, String nombre) {
        this.codigo = codigo;
        this.nombre = nombre;
    }

    public UUID getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCiudad() {
        return ciudad;
    }

    public String getZona() {
        return zona;
    }

    public String getDireccion() {
        return direccion;
    }

    public boolean isDeTurno() {
        return deTurno;
    }

    public boolean isActiva() {
        return activa;
    }

    public GrupoTrxEntidad getGrupoTrx() {
        return grupoTrx;
    }

    public OffsetDateTime getCreadoEn() {
        return creadoEn;
    }

    public OffsetDateTime getActualizadoEn() {
        return actualizadoEn;
    }
}
