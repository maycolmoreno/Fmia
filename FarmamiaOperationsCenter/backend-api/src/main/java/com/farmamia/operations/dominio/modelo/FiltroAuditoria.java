package com.farmamia.operations.dominio.modelo;

import java.time.OffsetDateTime;

public record FiltroAuditoria(
    String accion,
    String tipoEntidad,
    String usuarioActor,
    OffsetDateTime desde,
    OffsetDateTime hasta,
    int limite
) {
}
