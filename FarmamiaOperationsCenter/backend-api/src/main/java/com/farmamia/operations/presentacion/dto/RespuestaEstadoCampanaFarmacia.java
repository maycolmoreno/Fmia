package com.farmamia.operations.presentacion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record RespuestaEstadoCampanaFarmacia(
    UUID farmaciaId,
    String codigoFarmacia,
    String nombreFarmacia,
    UUID campanaId,
    UUID grupoTrxId,
    String codigoGrupoTrx,
    boolean deTurno,
    int totalEquiposPos,
    int completados,
    int pendientes,
    int fallidos,
    int rollbacks,
    OffsetDateTime ultimoHeartbeatRelacionado,
    int alertasCriticas,
    int alertasAbiertas,
    String estadoTecnico,
    String estadoOperacional,
    String resumenRiesgo,
    @JsonProperty("devices") List<RespuestaEquipoEstadoCampanaFarmacia> equipos
) {
}
