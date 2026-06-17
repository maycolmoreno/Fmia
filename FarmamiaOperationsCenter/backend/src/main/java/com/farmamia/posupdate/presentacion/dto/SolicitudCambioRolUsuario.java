package com.farmamia.posupdate.presentacion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record SolicitudCambioRolUsuario(
    @JsonProperty("role") @NotBlank String rol
) {
}
