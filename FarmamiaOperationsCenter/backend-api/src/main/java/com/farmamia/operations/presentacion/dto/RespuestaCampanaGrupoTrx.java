package com.farmamia.operations.presentacion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record RespuestaCampanaGrupoTrx(
    @JsonProperty("id") UUID id,
    @JsonProperty("campanaId") UUID campanaId,
    @JsonProperty("nombreCampana") String nombreCampana,
    @JsonProperty("versionPos") String versionPos,
    @JsonProperty("estadoCampana") String estadoCampana,
    @JsonProperty("grupoTrxId") UUID grupoTrxId,
    @JsonProperty("codigoGrupoTrx") String codigoGrupoTrx,
    @JsonProperty("nombreGrupoTrx") String nombreGrupoTrx,
    @JsonProperty("orden") int orden,
    @JsonProperty("estado") String estado,
    @JsonProperty("totalFarmacias") int totalFarmacias,
    @JsonProperty("farmaciasAfectadas") int farmaciasAfectadas,
    @JsonProperty("farmaciasTurnoAfectadas") int farmaciasTurnoAfectadas,
    @JsonProperty("farmaciasCriticas") int farmaciasCriticas,
    @JsonProperty("farmaciasPendientes") int farmaciasPendientes,
    @JsonProperty("farmaciasConFallos") int farmaciasConFallos,
    @JsonProperty("equiposPosTotales") int equiposPosTotales,
    @JsonProperty("equiposPosCompletados") int equiposPosCompletados,
    @JsonProperty("equiposPosPendientes") int equiposPosPendientes,
    @JsonProperty("equiposPosFallidos") int equiposPosFallidos,
    @JsonProperty("rollbacks") int rollbacks,
    @JsonProperty("motivoPausa") String motivoPausa,
    @JsonProperty("resumenRiesgo") String resumenRiesgo,
    @JsonProperty("iniciadoEn") OffsetDateTime iniciadoEn,
    @JsonProperty("finalizadoEn") OffsetDateTime finalizadoEn,
    @JsonProperty("createdAt") OffsetDateTime creadoEn,
    @JsonProperty("updatedAt") OffsetDateTime actualizadoEn,
    @JsonProperty("farmacias") List<RespuestaEstadoCampanaFarmacia> farmacias
) {
}
