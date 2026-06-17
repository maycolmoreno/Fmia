package com.farmamia.posupdate.presentacion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RespuestaSucursal(
    @JsonProperty("id") UUID id,
    @JsonProperty("code") String codigo,
    @JsonProperty("name") String nombre,
    @JsonProperty("city") String ciudad,
    @JsonProperty("zone") String zona,
    @JsonProperty("address") String direccion,
    @JsonProperty("onDuty") boolean deTurno,
    @JsonProperty("active") boolean activa,
    @JsonProperty("createdAt") OffsetDateTime creadoEn,
    @JsonProperty("updatedAt") OffsetDateTime actualizadoEn
) {
}
