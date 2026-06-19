package com.farmamia.posupdate.infraestructura.persistencia.entidad;

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

    @Column(name = "cpu_usage_percent")
    private Integer usoCpuPorcentaje;

    @Column(name = "ram_usage_percent")
    private Integer usoRamPorcentaje;

    @Column(name = "response_time_ms")
    private Integer tiempoRespuestaMs;

    @Column(name = "inbound_traffic_kbps", precision = 12, scale = 2)
    private BigDecimal traficoInboundKbps;

    @Column(name = "outbound_traffic_kbps", precision = 12, scale = 2)
    private BigDecimal traficoOutboundKbps;

    @Column(name = "router_uptime_ticks")
    private Long uptimeRouterTicks;

    @Column(name = "router_sys_desc")
    private String descripcionRouter;

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
        String estadoAgente,
        Integer usoCpuPorcentaje,
        Integer usoRamPorcentaje,
        Integer tiempoRespuestaMs,
        BigDecimal traficoInboundKbps,
        BigDecimal traficoOutboundKbps,
        Long uptimeRouterTicks,
        String descripcionRouter
    ) {
        this.equipo = equipo;
        this.versionPos = versionPos;
        this.discoLibreMb = discoLibreMb;
        this.discoTotalMb = discoTotalMb;
        this.procesoPosEjecutandose = procesoPosEjecutandose;
        this.latenciaMs = latenciaMs;
        this.porcentajePerdidaPaquetes = porcentajePerdidaPaquetes;
        this.estadoAgente = estadoAgente;
        this.usoCpuPorcentaje = usoCpuPorcentaje;
        this.usoRamPorcentaje = usoRamPorcentaje;
        this.tiempoRespuestaMs = tiempoRespuestaMs;
        this.traficoInboundKbps = traficoInboundKbps;
        this.traficoOutboundKbps = traficoOutboundKbps;
        this.uptimeRouterTicks = uptimeRouterTicks;
        this.descripcionRouter = descripcionRouter;
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

    public Integer getUsoCpuPorcentaje() {
        return usoCpuPorcentaje;
    }

    public Integer getUsoRamPorcentaje() {
        return usoRamPorcentaje;
    }

    public Integer getTiempoRespuestaMs() {
        return tiempoRespuestaMs;
    }

    public BigDecimal getTraficoInboundKbps() {
        return traficoInboundKbps;
    }

    public BigDecimal getTraficoOutboundKbps() {
        return traficoOutboundKbps;
    }

    public Long getUptimeRouterTicks() {
        return uptimeRouterTicks;
    }

    public String getDescripcionRouter() {
        return descripcionRouter;
    }

    public OffsetDateTime getRecolectadoEn() {
        return recolectadoEn;
    }
}
