package com.farmamia.operations.dominio.modelo;

import java.util.UUID;

public record ResultadoActualizacion(
    UUID idObjetivoDespliegue,
    String idempotencyKey,
    String estado,
    String versionAnterior,
    String versionNueva,
    String mensaje
) {
}
