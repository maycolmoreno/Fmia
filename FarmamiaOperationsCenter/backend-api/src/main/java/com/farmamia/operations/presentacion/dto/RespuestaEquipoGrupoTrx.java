package com.farmamia.operations.presentacion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RespuestaEquipoGrupoTrx(
    @JsonProperty("deviceId") UUID idEquipo,
    @JsonProperty("hostname") String nombreEquipo,
    @JsonProperty("branchId") UUID idFarmacia,
    @JsonProperty("branchCode") String codigoFarmacia,
    @JsonProperty("branchName") String nombreFarmacia,
    @JsonProperty("posVersion") String versionPos,
    @JsonProperty("deviceStatus") String estadoEquipo,
    @JsonProperty("lastHeartbeatAt") OffsetDateTime ultimoLatidoEn,
    @JsonProperty("assignedAt") OffsetDateTime asignadoEn
) {
}
