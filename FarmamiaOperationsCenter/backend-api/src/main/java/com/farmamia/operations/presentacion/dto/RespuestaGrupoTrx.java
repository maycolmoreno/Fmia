package com.farmamia.operations.presentacion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record RespuestaGrupoTrx(
    @JsonProperty("id") UUID id,
    @JsonProperty("code") String codigo,
    @JsonProperty("name") String nombre,
    @JsonProperty("description") String descripcion,
    @JsonProperty("status") String estado,
    @JsonProperty("maxDevices") int maximoEquipos,
    @JsonProperty("active") boolean activo,
    @JsonProperty("assignedDevices") long equiposAsignados,
    @JsonProperty("involvedBranches") long farmaciasInvolucradas,
    @JsonProperty("createdAt") OffsetDateTime creadoEn,
    @JsonProperty("updatedAt") OffsetDateTime actualizadoEn,
    @JsonProperty("devices") List<RespuestaEquipoGrupoTrx> equipos,
    @JsonProperty("branchCodes") List<String> codigosFarmacia
) {
}
