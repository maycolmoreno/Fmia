package com.farmamia.operations.dominio.modelo;

import java.util.Map;
import java.util.UUID;

public record EventoActualizacion(
    UUID idEquipo,
    UUID idObjetivoDespliegue,
    String idempotencyKey,
    String tipoEvento,
    String mensajeEvento,
    String versionAnterior,
    String versionNueva,
    Map<String, Object> metadatos
) {
}
