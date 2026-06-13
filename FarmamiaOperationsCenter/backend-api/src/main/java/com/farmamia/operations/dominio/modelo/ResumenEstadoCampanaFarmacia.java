package com.farmamia.operations.dominio.modelo;

import java.util.List;
import java.util.UUID;

public record ResumenEstadoCampanaFarmacia(
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
    int pagina,
    int tamano,
    long totalElementos,
    int totalPaginas,
    boolean tieneSiguiente,
    List<EstadoCampanaFarmacia> farmacias
) {
}
