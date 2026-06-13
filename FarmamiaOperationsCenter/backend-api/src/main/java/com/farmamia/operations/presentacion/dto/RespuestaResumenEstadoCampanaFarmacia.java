package com.farmamia.operations.presentacion.dto;

import java.util.List;
import java.util.UUID;

public record RespuestaResumenEstadoCampanaFarmacia(
    UUID campanaId,
    String nombreCampana,
    String versionPos,
    String estadoCampana,
    int totalFarmacias,
    int farmaciasCompletadas,
    int farmaciasPendientes,
    int farmaciasEnProgreso,
    int farmaciasConErrores,
    int farmaciasEnRiesgo,
    int farmaciasCriticas,
    int farmaciasTurnoEnRiesgo,
    double avancePorcentaje,
    double exitoPorcentaje,
    String grupoTrxPeorEstado,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean hasNext,
    List<RespuestaEstadoCampanaFarmacia> farmacias
) {
}
