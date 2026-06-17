package com.farmamia.posupdate.dominio.modelo;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CampanaGrupoTrx(
    UUID id,
    UUID campanaId,
    String nombreCampana,
    String versionPos,
    String estadoCampana,
    UUID grupoTrxId,
    String codigoGrupoTrx,
    String nombreGrupoTrx,
    int orden,
    EstadoCampanaGrupoTrx estado,
    int totalFarmacias,
    int farmaciasAfectadas,
    int farmaciasTurnoAfectadas,
    int farmaciasCriticas,
    int farmaciasPendientes,
    int farmaciasConFallos,
    int equiposPosTotales,
    int equiposPosCompletados,
    int equiposPosPendientes,
    int equiposPosFallidos,
    int rollbacks,
    String motivoPausa,
    String resumenRiesgo,
    OffsetDateTime iniciadoEn,
    OffsetDateTime finalizadoEn,
    OffsetDateTime creadoEn,
    OffsetDateTime actualizadoEn,
    List<EstadoCampanaFarmacia> farmacias
) {
}
