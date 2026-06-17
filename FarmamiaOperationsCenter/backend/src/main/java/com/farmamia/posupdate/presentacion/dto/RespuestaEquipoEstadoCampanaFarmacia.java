package com.farmamia.posupdate.presentacion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RespuestaEquipoEstadoCampanaFarmacia(
    @JsonProperty("deviceId") UUID equipoId,
    @JsonProperty("hostname") String nombreEquipo,
    @JsonProperty("deviceStatus") String estadoEquipo,
    @JsonProperty("targetStatus") String estadoObjetivo,
    @JsonProperty("grupoTrx") String codigoGrupoTrx,
    @JsonProperty("oldVersion") String versionAnterior,
    @JsonProperty("newVersion") String versionNueva,
    @JsonProperty("lastError") String ultimoError,
    @JsonProperty("lastHeartbeatAt") OffsetDateTime ultimoHeartbeatEn,
    @JsonProperty("rollback") boolean rollback
) {
}
