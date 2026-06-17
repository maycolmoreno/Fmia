package com.farmamia.posupdate.dominio.modelo;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

public record OleadaOrquestacion(
    UUID id,
    int numero,
    String nombre,
    String grupoObjetivo,
    boolean piloto,
    String estado,
    int objetivosPlanificados,
    long objetivosCompletados,
    long objetivosFallidos,
    long objetivosPendientes,
    long farmaciasTurno,
    double porcentajeFallo,
    LocalTime ventanaInicio,
    LocalTime ventanaFin,
    OffsetDateTime iniciadoEn,
    OffsetDateTime completadoEn
) {
}
