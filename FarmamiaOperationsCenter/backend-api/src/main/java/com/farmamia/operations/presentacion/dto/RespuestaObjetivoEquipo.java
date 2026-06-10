package com.farmamia.operations.presentacion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RespuestaObjetivoEquipo(
    @JsonProperty("targetId") UUID idObjetivo,
    @JsonProperty("deploymentId") UUID idDespliegue,
    @JsonProperty("deploymentName") String nombreDespliegue,
    @JsonProperty("packageVersion") String versionPaquete,
    @JsonProperty("deploymentStatus") String estadoDespliegue,
    @JsonProperty("targetStatus") String estadoObjetivo,
    @JsonProperty("targetGroup") String grupoObjetivo,
    @JsonProperty("pilot") boolean piloto,
    @JsonProperty("oldVersion") String versionAnterior,
    @JsonProperty("newVersion") String versionNueva,
    @JsonProperty("lastError") String ultimoError,
    @JsonProperty("authorizedAt") OffsetDateTime autorizadoEn,
    @JsonProperty("startedAt") OffsetDateTime iniciadoEn,
    @JsonProperty("completedAt") OffsetDateTime completadoEn,
    @JsonProperty("updatedAt") OffsetDateTime actualizadoEn
) {
}
