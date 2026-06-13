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
@Table(name = "equipo_pos_grupo_trx")
public class EquipoGrupoTrxEntidad {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipo_id", nullable = false)
    private EquipoEntidad equipo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "grupo_trx_id", nullable = false)
    private GrupoTrxEntidad grupoTrx;

    @CreationTimestamp
    @Column(name = "asignado_en", nullable = false, updatable = false)
    private OffsetDateTime asignadoEn;

    protected EquipoGrupoTrxEntidad() {
    }

    public EquipoGrupoTrxEntidad(EquipoEntidad equipo, GrupoTrxEntidad grupoTrx) {
        this.equipo = equipo;
        this.grupoTrx = grupoTrx;
    }

    public UUID getId() {
        return id;
    }

    public EquipoEntidad getEquipo() {
        return equipo;
    }

    public GrupoTrxEntidad getGrupoTrx() {
        return grupoTrx;
    }

    public OffsetDateTime getAsignadoEn() {
        return asignadoEn;
    }

    public void cambiarGrupo(GrupoTrxEntidad grupoTrx) {
        this.grupoTrx = grupoTrx;
        this.asignadoEn = OffsetDateTime.now();
    }
}
