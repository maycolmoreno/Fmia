package com.farmamia.posupdate.presentacion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import java.util.UUID;

public record SolicitudEventoAgente(
    @JsonProperty("deploymentTargetId") UUID idObjetivoDespliegue,
    @JsonProperty("idempotencyKey") String idempotencyKey,
    @JsonProperty("eventType") @NotBlank String tipoEvento,
    @JsonProperty("eventMessage") String mensajeEvento,
    @JsonProperty("oldVersion") String versionAnterior,
    @JsonProperty("newVersion") String versionNueva,
    @JsonProperty("metadata") Map<String, Object> metadatos
) {
}
