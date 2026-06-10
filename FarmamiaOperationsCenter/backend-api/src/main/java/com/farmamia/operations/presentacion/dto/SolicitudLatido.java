package com.farmamia.operations.presentacion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;

public record SolicitudLatido(
    @JsonProperty("deviceId") @NotNull UUID idEquipo,
    @JsonProperty("posVersion") String versionPos,
    @JsonProperty("diskFreeMb") @PositiveOrZero Integer discoLibreMb,
    @JsonProperty("diskTotalMb") @PositiveOrZero Integer discoTotalMb,
    @JsonProperty("posProcessRunning") Boolean procesoPosEjecutandose,
    @JsonProperty("latencyMs") @PositiveOrZero Integer latenciaMs,
    @JsonProperty("packetLossPercent") @DecimalMin("0.0") @DecimalMax("100.0") Double porcentajePerdidaPaquetes
) {
}
