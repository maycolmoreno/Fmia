package com.farmamia.operations.presentacion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import java.util.UUID;

public record RespuestaEstadoDespliegue(
    @JsonProperty("deploymentId") UUID idDespliegue,
    @JsonProperty("status") String estado,
    @JsonProperty("targetsByStatus") Map<String, Long> objetivosPorEstado
) {
}
