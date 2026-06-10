package com.farmamia.operations.presentacion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RespuestaAlerta(
    @JsonProperty("id") UUID id,
    @JsonProperty("deviceId") UUID idEquipo,
    @JsonProperty("hostname") String nombreEquipo,
    @JsonProperty("branchId") UUID idSucursal,
    @JsonProperty("branchCode") String codigoSucursal,
    @JsonProperty("severity") String severidad,
    @JsonProperty("alertType") String tipoAlerta,
    @JsonProperty("title") String titulo,
    @JsonProperty("message") String mensaje,
    @JsonProperty("status") String estado,
    @JsonProperty("openedAt") OffsetDateTime abiertaEn,
    @JsonProperty("acknowledgedBy") String reconocidaPor,
    @JsonProperty("acknowledgedAt") OffsetDateTime reconocidaEn,
    @JsonProperty("closedBy") String cerradaPor,
    @JsonProperty("closedAt") OffsetDateTime cerradaEn
) {
}
