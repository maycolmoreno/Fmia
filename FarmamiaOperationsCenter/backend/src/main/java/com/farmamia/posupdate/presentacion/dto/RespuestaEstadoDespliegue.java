package com.farmamia.posupdate.presentacion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import java.util.UUID;

public record RespuestaEstadoDespliegue(
    @JsonProperty("deploymentId") UUID idDespliegue,
    @JsonProperty("status") String estado,
    @JsonProperty("totalTargets") long totalObjetivos,
    @JsonProperty("completedTargets") long objetivosCompletados,
    @JsonProperty("failedTargets") long objetivosFallidos,
    @JsonProperty("pendingTargets") long objetivosPendientes,
    @JsonProperty("progressPercent") double porcentajeAvance,
    @JsonProperty("failurePercent") double porcentajeFallo,
    @JsonProperty("targetsByStatus") Map<String, Long> objetivosPorEstado
) {
}
