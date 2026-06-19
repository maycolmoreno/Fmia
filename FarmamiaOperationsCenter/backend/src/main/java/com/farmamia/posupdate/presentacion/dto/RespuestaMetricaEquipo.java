package com.farmamia.posupdate.presentacion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RespuestaMetricaEquipo(
    @JsonProperty("id") UUID id,
    @JsonProperty("posVersion") String versionPos,
    @JsonProperty("diskFreeMb") Integer discoLibreMb,
    @JsonProperty("diskTotalMb") Integer discoTotalMb,
    @JsonProperty("posProcessRunning") Boolean procesoPosEjecutandose,
    @JsonProperty("latencyMs") Integer latenciaMs,
    @JsonProperty("packetLossPercent") BigDecimal porcentajePerdidaPaquetes,
    @JsonProperty("agentStatus") String estadoAgente,
    @JsonProperty("usoCpuPorcentaje") Integer usoCpuPorcentaje,
    @JsonProperty("usoRamPorcentaje") Integer usoRamPorcentaje,
    @JsonProperty("responseTimeMs") Integer tiempoRespuestaMs,
    @JsonProperty("traficoInboundKbps") BigDecimal traficoInboundKbps,
    @JsonProperty("traficoOutboundKbps") BigDecimal traficoOutboundKbps,
    @JsonProperty("uptimeRouterTicks") Long uptimeRouterTicks,
    @JsonProperty("routerDescription") String descripcionRouter,
    @JsonProperty("collectedAt") OffsetDateTime recolectadoEn
) {
}
