package com.farmamia.operations.presentacion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SolicitudCrearUsuarioAdministrativo(
    @JsonProperty("username") @NotBlank String usuario,
    @JsonProperty("password") @NotBlank String contrasena,
    @JsonProperty("fullName") @NotBlank String nombreCompleto,
    @JsonProperty("email") @Email String correo,
    @JsonProperty("role") @NotBlank String rol
) {
}
