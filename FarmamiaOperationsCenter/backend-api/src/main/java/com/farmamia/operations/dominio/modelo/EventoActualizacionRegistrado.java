package com.farmamia.operations.dominio.modelo;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record EventoActualizacionRegistrado(
    UUID id,
    UUID idEquipo,
    String nombreEquipo,
    UUID idDespliegue,
    UUID idObjetivoDespliegue,
    String tipoEvento,
    String mensajeEvento,
    String versionAnterior,
    String versionNueva,
    Map<String, Object> metadatos,
    OffsetDateTime creadoEn
) {
}
