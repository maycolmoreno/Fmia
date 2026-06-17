package com.farmamia.posupdate.dominio.modelo;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FiltroEventosActualizacion(
    UUID idEquipo,
    UUID idDespliegue,
    String tipoEvento,
    OffsetDateTime desde,
    OffsetDateTime hasta,
    int pagina,
    int tamano,
    String orden
) {
}
