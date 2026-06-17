package com.farmamia.posupdate.presentacion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SolicitudResultadoActualizacion(
    @JsonProperty("deploymentTargetId") @NotNull UUID idObjetivoDespliegue,
    @JsonProperty("idempotencyKey") String idempotencyKey,
    @JsonProperty("status") @NotBlank String estado,
    @JsonProperty("oldVersion") String versionAnterior,
    @JsonProperty("newVersion") String versionNueva,
    @JsonProperty("message") String mensaje
) {
}
