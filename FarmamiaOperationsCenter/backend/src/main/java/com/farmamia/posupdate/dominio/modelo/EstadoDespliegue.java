package com.farmamia.posupdate.dominio.modelo;

import java.util.Map;
import java.util.UUID;

public record EstadoDespliegue(
    UUID idDespliegue,
    String estado,
    long totalObjetivos,
    long objetivosCompletados,
    long objetivosFallidos,
    long objetivosPendientes,
    double porcentajeAvance,
    double porcentajeFallo,
    Map<String, Long> objetivosPorEstado
) {
}
