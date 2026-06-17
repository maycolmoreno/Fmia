package com.farmamia.posupdate.presentacion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RespuestaEquipo(
    @JsonProperty("id") UUID id,
    @JsonProperty("branchId") UUID idSucursal,
    @JsonProperty("branchCode") String codigoSucursal,
    @JsonProperty("branchName") String nombreSucursal,
    @JsonProperty("hostname") String nombreEquipo,
    @JsonProperty("ipAddress") String direccionIp,
    @JsonProperty("macAddress") String direccionMac,
    @JsonProperty("windowsVersion") String versionWindows,
    @JsonProperty("agentVersion") String versionAgente,
    @JsonProperty("posVersion") String versionPos,
    @JsonProperty("posPath") String rutaPos,
    @JsonProperty("status") String estado,
    @JsonProperty("lastHeartbeatAt") OffsetDateTime ultimoLatidoEn,
    @JsonProperty("registeredAt") OffsetDateTime registradoEn,
    @JsonProperty("updatedAt") OffsetDateTime actualizadoEn
) {
}
