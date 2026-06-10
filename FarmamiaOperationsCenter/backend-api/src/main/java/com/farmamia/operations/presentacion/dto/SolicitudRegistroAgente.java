package com.farmamia.operations.presentacion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record SolicitudRegistroAgente(
    @JsonProperty("branchCode") @NotBlank String codigoSucursal,
    @JsonProperty("hostname") @NotBlank String nombreEquipo,
    @JsonProperty("ipAddress") String direccionIp,
    @JsonProperty("macAddress") String direccionMac,
    @JsonProperty("windowsVersion") String versionWindows,
    @JsonProperty("agentVersion") String versionAgente,
    @JsonProperty("posVersion") String versionPos,
    @JsonProperty("posPath") @NotBlank String rutaPos
) {
}
