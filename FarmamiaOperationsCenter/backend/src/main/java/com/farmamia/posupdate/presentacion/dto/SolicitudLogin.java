package com.farmamia.posupdate.presentacion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record SolicitudLogin(
    @JsonProperty("username") @NotBlank String usuario,
    @JsonProperty("password") @NotBlank String contrasena
) {
}
