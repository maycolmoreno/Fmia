package com.farmamia.posupdate.presentacion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RespuestaEquipoHuerfano(
    @JsonProperty("deviceId") UUID idEquipo,
    @JsonProperty("hostname") String nombreEquipo,
    @JsonProperty("ipAddress") String direccionIp,
    @JsonProperty("agentVersion") String versionAgente,
    @JsonProperty("posVersion") String versionPos,
    @JsonProperty("registeredAt") OffsetDateTime registradoEn,
    @JsonProperty("suggestionStatus") String estadoSugerencia,
    @JsonProperty("suggestedBranchId") UUID idSucursalSugerida,
    @JsonProperty("suggestedBranchCode") String codigoSucursalSugerida,
    @JsonProperty("suggestedBranchName") String nombreSucursalSugerida,
    @JsonProperty("suggestedGrupoTrxCode") String codigoGrupoTrxSugerido
) {
}
