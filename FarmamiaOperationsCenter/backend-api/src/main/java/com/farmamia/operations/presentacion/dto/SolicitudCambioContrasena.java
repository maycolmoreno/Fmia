package com.farmamia.operations.presentacion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record SolicitudCambioContrasena(
    @JsonProperty("currentPassword") @NotBlank String contrasenaActual,
    @JsonProperty("newPassword") @NotBlank String contrasenaNueva
) {
}
