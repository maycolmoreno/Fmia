package com.farmamia.operations.presentacion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record SolicitudProgramarDespliegue(
    @JsonProperty("scheduledAt") @NotNull OffsetDateTime programadoEn
) {
}
