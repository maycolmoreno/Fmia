package com.farmamia.posupdate.presentacion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SolicitudActualizarUsuarioAdministrativo(
    @JsonProperty("fullName") @NotBlank String nombreCompleto,
    @JsonProperty("email") @Email String correo
) {
}
