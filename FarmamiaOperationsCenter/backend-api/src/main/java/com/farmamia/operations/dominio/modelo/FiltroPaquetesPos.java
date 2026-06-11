package com.farmamia.operations.dominio.modelo;

import java.time.OffsetDateTime;

public record FiltroPaquetesPos(
    String q,
    String estado,
    String version,
    OffsetDateTime cargadoDesde,
    OffsetDateTime cargadoHasta,
    int pagina,
    int tamano,
    String orden
) {
}
