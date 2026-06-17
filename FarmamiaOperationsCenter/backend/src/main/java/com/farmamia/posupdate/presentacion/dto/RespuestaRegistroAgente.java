package com.farmamia.posupdate.presentacion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RespuestaRegistroAgente(
    @JsonProperty("deviceId") UUID idEquipo,
    @JsonProperty("agentToken") String tokenAgente,
    @JsonProperty("serverTime") OffsetDateTime horaServidor
) {
}
