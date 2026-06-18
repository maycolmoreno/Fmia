package com.farmamia.posupdate.presentacion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record SolicitudAsignacionMasivaEquipos(
    @JsonProperty("assignments")
    @NotEmpty(message = "Debe incluir al menos una asignacion")
    List<@Valid ItemAsignacion> asignaciones
) {

    public record ItemAsignacion(
        @JsonProperty("deviceId")
        @NotNull(message = "deviceId es requerido")
        UUID idEquipo,

        @JsonProperty("branchId")
        @NotNull(message = "branchId es requerido")
        UUID idSucursal
    ) {
    }
}
