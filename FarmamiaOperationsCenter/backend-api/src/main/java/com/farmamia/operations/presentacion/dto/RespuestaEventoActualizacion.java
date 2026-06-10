package com.farmamia.operations.presentacion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record RespuestaEventoActualizacion(
    @JsonProperty("id") UUID id,
    @JsonProperty("deviceId") UUID idEquipo,
    @JsonProperty("hostname") String nombreEquipo,
    @JsonProperty("deploymentId") UUID idDespliegue,
    @JsonProperty("deploymentTargetId") UUID idObjetivoDespliegue,
    @JsonProperty("eventType") String tipoEvento,
    @JsonProperty("eventMessage") String mensajeEvento,
    @JsonProperty("oldVersion") String versionAnterior,
    @JsonProperty("newVersion") String versionNueva,
    @JsonProperty("metadata") Map<String, Object> metadatos,
    @JsonProperty("createdAt") OffsetDateTime creadoEn
) {
}
