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
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "campana_grupo_trx")
public class CampanaGrupoTrxEntidad {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campana_id", nullable = false)
    private DespliegueEntidad campana;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "grupo_trx_id", nullable = false)
    private GrupoTrxEntidad grupoTrx;

    @Column(name = "orden", nullable = false)
    private int orden = 1;

    @Column(name = "estado", nullable = false, length = 40)
    private String estado = "PENDIENTE";

    @Column(name = "motivo_pausa")
    private String motivoPausa;

    @Column(name = "iniciado_en")
    private OffsetDateTime iniciadoEn;

    @Column(name = "finalizado_en")
    private OffsetDateTime finalizadoEn;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime actualizadoEn;

    protected CampanaGrupoTrxEntidad() {
    }

    public CampanaGrupoTrxEntidad(DespliegueEntidad campana, GrupoTrxEntidad grupoTrx, int orden) {
        this.campana = campana;
        this.grupoTrx = grupoTrx;
        this.orden = orden;
    }

    public UUID getId() {
        return id;
    }

    public DespliegueEntidad getCampana() {
        return campana;
    }

    public GrupoTrxEntidad getGrupoTrx() {
        return grupoTrx;
    }

    public int getOrden() {
        return orden;
    }

    public String getEstado() {
        return estado;
    }

    public String getMotivoPausa() {
        return motivoPausa;
    }

    public OffsetDateTime getIniciadoEn() {
        return iniciadoEn;
    }

    public OffsetDateTime getFinalizadoEn() {
        return finalizadoEn;
    }

    public OffsetDateTime getCreadoEn() {
        return creadoEn;
    }

    public OffsetDateTime getActualizadoEn() {
        return actualizadoEn;
    }

    public void pausar(String motivo) {
        this.estado = "PAUSADO";
        this.motivoPausa = motivo;
    }

    public void reanudar() {
        this.estado = "PENDIENTE";
        this.motivoPausa = null;
        this.finalizadoEn = null;
    }

    public void actualizarEstadoCalculado(String estadoCalculado) {
        if ("PAUSADO".equals(this.estado)) {
            return;
        }
        this.estado = estadoCalculado;
        if ("EN_EJECUCION".equals(estadoCalculado) && iniciadoEn == null) {
            iniciadoEn = OffsetDateTime.now();
        }
        if (estadoCalculado.startsWith("COMPLETADO") || "FALLIDO".equals(estadoCalculado)) {
            finalizadoEn = finalizadoEn == null ? OffsetDateTime.now() : finalizadoEn;
        }
    }
}
