package com.farmamia.operations.presentacion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RespuestaOleadaOrquestacion(
    @JsonProperty("id") UUID id,
    @JsonProperty("number") int numero,
    @JsonProperty("name") String nombre,
    @JsonProperty("targetGroup") String grupoObjetivo,
    @JsonProperty("pilot") boolean piloto,
    @JsonProperty("status") String estado,
    @JsonProperty("plannedTargets") int objetivosPlanificados,
    @JsonProperty("completedTargets") long objetivosCompletados,
    @JsonProperty("failedTargets") long objetivosFallidos,
    @JsonProperty("pendingTargets") long objetivosPendientes,
    @JsonProperty("onDutyBranches") long farmaciasTurno,
    @JsonProperty("failurePercent") double porcentajeFallo,
    @JsonProperty("maintenanceWindowStart") LocalTime ventanaInicio,
    @JsonProperty("maintenanceWindowEnd") LocalTime ventanaFin,
    @JsonProperty("startedAt") OffsetDateTime iniciadoEn,
    @JsonProperty("completedAt") OffsetDateTime completadoEn
) {
}
