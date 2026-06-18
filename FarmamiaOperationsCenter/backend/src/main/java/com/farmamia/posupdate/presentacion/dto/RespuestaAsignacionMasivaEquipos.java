package com.farmamia.posupdate.presentacion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RespuestaAsignacionMasivaEquipos(
    @JsonProperty("assigned") int asignados,
    @JsonProperty("skipped") int omitidos
) {
}
