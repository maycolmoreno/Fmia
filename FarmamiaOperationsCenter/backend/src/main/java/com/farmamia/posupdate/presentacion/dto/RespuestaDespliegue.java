package com.farmamia.posupdate.presentacion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RespuestaDespliegue(
    @JsonProperty("id") UUID id,
    @JsonProperty("packageId") UUID idPaquete,
    @JsonProperty("packageVersion") String versionPaquete,
    @JsonProperty("name") String nombre,
    @JsonProperty("description") String descripcion,
    @JsonProperty("status") String estado,
    @JsonProperty("scheduledAt") OffsetDateTime programadoEn,
    @JsonProperty("createdAt") OffsetDateTime creadoEn,
    @JsonProperty("targetCount") long cantidadObjetivos
) {
}
