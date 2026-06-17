package com.farmamia.posupdate.presentacion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SolicitudCrearDespliegue(
    @JsonProperty("packageId") @NotNull UUID idPaquete,
    @JsonProperty("name") @NotBlank String nombre,
    @JsonProperty("description") String descripcion,
    @JsonProperty("scheduledAt") OffsetDateTime programadoEn,
    @JsonProperty("targetGroup") String grupoObjetivo,
    @JsonProperty("pilot") boolean piloto,
    @JsonProperty("deviceIds") @NotEmpty List<UUID> idsEquipos
) {
}
