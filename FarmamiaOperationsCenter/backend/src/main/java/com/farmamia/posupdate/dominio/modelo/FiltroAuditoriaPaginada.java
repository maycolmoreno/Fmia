package com.farmamia.posupdate.dominio.modelo;

import java.time.OffsetDateTime;

public record FiltroAuditoriaPaginada(
    String accion,
    String tipoEntidad,
    String usuarioActor,
    OffsetDateTime desde,
    OffsetDateTime hasta,
    int pagina,
    int tamano,
    String orden
) {
}
