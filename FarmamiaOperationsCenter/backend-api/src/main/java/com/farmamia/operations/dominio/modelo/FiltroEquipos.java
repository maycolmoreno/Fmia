package com.farmamia.operations.dominio.modelo;

import java.time.OffsetDateTime;

public record FiltroEquipos(
    String q,
    String estado,
    String codigoSucursal,
    String versionPos,
    String versionAgente,
    OffsetDateTime ultimoLatidoDesde,
    OffsetDateTime ultimoLatidoHasta,
    int pagina,
    int tamano,
    String orden
) {
}
