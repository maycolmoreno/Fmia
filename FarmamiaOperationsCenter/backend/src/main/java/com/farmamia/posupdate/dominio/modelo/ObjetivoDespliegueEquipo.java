package com.farmamia.posupdate.dominio.modelo;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ObjetivoDespliegueEquipo(
    UUID idObjetivo,
    UUID idDespliegue,
    String nombreDespliegue,
    String versionPaquete,
    String estadoDespliegue,
    String estadoObjetivo,
    String grupoObjetivo,
    boolean piloto,
    String versionAnterior,
    String versionNueva,
    String ultimoError,
    OffsetDateTime autorizadoEn,
    OffsetDateTime iniciadoEn,
    OffsetDateTime completadoEn,
    OffsetDateTime actualizadoEn
) {
}
