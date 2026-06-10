package com.farmamia.operations.infraestructura.persistencia.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "device_metrics")
public class MetricaEquipoEntidad {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private EquipoEntidad equipo;

    @Column(name = "pos_version", length = 40)
    private String versionPos;

    @Column(name = "disk_free_mb")
    private Integer discoLibreMb;

    @Column(name = "disk_total_mb")
    private Integer discoTotalMb;

    @Column(name = "pos_process_running")
    private Boolean procesoPosEjecutandose;

    @Column(name = "latency_ms")
    private Integer latenciaMs;

    @Column(name = "packet_loss_percent", precision = 5, scale = 2)
    private BigDecimal porcentajePerdidaPaquetes;

    @Column(name = "agent_status", length = 40)
    private String estadoAgente;

    @CreationTimestamp
    @Column(name = "collected_at", nullable = false, updatable = false)
    private OffsetDateTime recolectadoEn;

    protected MetricaEquipoEntidad() {
    }

    public MetricaEquipoEntidad(
        EquipoEntidad equipo,
        String versionPos,
        Integer discoLibreMb,
        Integer discoTotalMb,
        Boolean procesoPosEjecutandose,
        Integer latenciaMs,
        BigDecimal porcentajePerdidaPaquetes,
        String estadoAgente
    ) {
        this.equipo = equipo;
        this.versionPos = versionPos;
        this.discoLibreMb = discoLibreMb;
        this.discoTotalMb = discoTotalMb;
        this.procesoPosEjecutandose = procesoPosEjecutandose;
        this.latenciaMs = latenciaMs;
        this.porcentajePerdidaPaquetes = porcentajePerdidaPaquetes;
        this.estadoAgente = estadoAgente;
    }

    public UUID getId() {
        return id;
    }

    public String getVersionPos() {
        return versionPos;
    }

    public Integer getDiscoLibreMb() {
        return discoLibreMb;
    }

    public Integer getDiscoTotalMb() {
        return discoTotalMb;
    }

    public Boolean getProcesoPosEjecutandose() {
        return procesoPosEjecutandose;
    }

    public Integer getLatenciaMs() {
        return latenciaMs;
    }

    public BigDecimal getPorcentajePerdidaPaquetes() {
        return porcentajePerdidaPaquetes;
    }

    public String getEstadoAgente() {
        return estadoAgente;
    }

    public OffsetDateTime getRecolectadoEn() {
        return recolectadoEn;
    }
}
