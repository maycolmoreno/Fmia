package com.farmamia.posupdate.presentacion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RespuestaUsuarioAdministrativo(
    @JsonProperty("id") UUID id,
    @JsonProperty("username") String usuario,
    @JsonProperty("fullName") String nombreCompleto,
    @JsonProperty("email") String correo,
    @JsonProperty("role") String rol,
    @JsonProperty("active") boolean activo,
    @JsonProperty("failedLoginAttempts") int intentosFallidosLogin,
    @JsonProperty("lockedUntil") OffsetDateTime bloqueadoHasta,
    @JsonProperty("lastLoginAt") OffsetDateTime ultimoAccesoEn,
    @JsonProperty("createdAt") OffsetDateTime creadoEn,
    @JsonProperty("updatedAt") OffsetDateTime actualizadoEn
) {
}
