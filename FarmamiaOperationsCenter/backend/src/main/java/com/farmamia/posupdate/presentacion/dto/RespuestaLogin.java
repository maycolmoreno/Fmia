package com.farmamia.posupdate.presentacion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

public record RespuestaLogin(
    @JsonProperty("accessToken") String tokenAcceso,
    @JsonProperty("tokenType") String tipoToken,
    @JsonProperty("expiresAt") OffsetDateTime expiraEn,
    @JsonProperty("username") String usuario,
    @JsonProperty("fullName") String nombreCompleto,
    @JsonProperty("role") String rol
) {
}
