package com.farmamia.operations.presentacion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record SolicitudResetContrasenaUsuario(
    @JsonProperty("newPassword") @NotBlank String contrasenaNueva
) {
}
