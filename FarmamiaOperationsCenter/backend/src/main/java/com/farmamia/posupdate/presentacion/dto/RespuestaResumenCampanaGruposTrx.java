package com.farmamia.posupdate.presentacion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.UUID;

public record RespuestaResumenCampanaGruposTrx(
    @JsonProperty("campanaId") UUID campanaId,
    @JsonProperty("nombreCampana") String nombreCampana,
    @JsonProperty("versionPos") String versionPos,
    @JsonProperty("estadoCampana") String estadoCampana,
    @JsonProperty("totalGrupos") int totalGrupos,
    @JsonProperty("gruposEnRiesgo") int gruposEnRiesgo,
    @JsonProperty("gruposPausados") int gruposPausados,
    @JsonProperty("farmaciasAfectadas") int farmaciasAfectadas,
    @JsonProperty("farmaciasTurnoAfectadas") int farmaciasTurnoAfectadas,
    @JsonProperty("farmaciasCriticas") int farmaciasCriticas,
    @JsonProperty("grupos") List<RespuestaCampanaGrupoTrx> grupos
) {
}
