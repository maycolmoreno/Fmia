package com.farmamia.operations.presentacion.dto;

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
    @JsonProperty("collectedAt") OffsetDateTime recolectadoEn
) {
}
