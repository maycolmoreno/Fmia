package com.farmamia.posupdate.presentacion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SolicitudRegistroEquipoTecnico(
    @JsonProperty("codigoPdv")
    @NotBlank
    @Pattern(regexp = "^[A-Z]{2}\\d{3}$")
    String codigoPdv,

    @JsonProperty("direccionIp")
    @NotBlank
    @Pattern(regexp = "^(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}$")
    String direccionIp,

    @JsonProperty("comunidadSnmp")
    @NotBlank
    String comunidadSnmp
) {
}
