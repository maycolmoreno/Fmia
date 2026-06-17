package com.farmamia.posupdate.dominio.modelo;

import java.time.OffsetDateTime;

public record FiltroDespliegues(
    String q,
    String estado,
    String versionPaquete,
    OffsetDateTime creadoDesde,
    OffsetDateTime creadoHasta,
    int pagina,
    int tamano,
    String orden
) {
}
