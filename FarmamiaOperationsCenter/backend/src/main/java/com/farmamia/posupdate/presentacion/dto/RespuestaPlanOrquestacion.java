package com.farmamia.posupdate.presentacion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record RespuestaPlanOrquestacion(
    @JsonProperty("deploymentId") UUID idDespliegue,
    @JsonProperty("controlStatus") String estadoControl,
    @JsonProperty("maxFailurePercent") BigDecimal porcentajeMaximoFallo,
    @JsonProperty("autoPauseEnabled") boolean pausaAutomaticaHabilitada,
    @JsonProperty("retryLimit") int limiteReintentos,
    @JsonProperty("nextWaveNumber") int siguienteNumeroOleada,
    @JsonProperty("pausedReason") String motivoPausa,
    @JsonProperty("lastEvaluatedAt") OffsetDateTime evaluadoEn,
    @JsonProperty("waves") List<RespuestaOleadaOrquestacion> oleadas
) {
}
