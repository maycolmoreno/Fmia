package com.farmamia.operations.dominio.modelo;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FiltroAlertas(
    String estado,
    String severidad,
    String tipo,
    UUID idEquipo,
    UUID idSucursal,
    String codigoSucursal,
    String nombreEquipo,
    OffsetDateTime fechaDesde,
    OffsetDateTime fechaHasta,
    int pagina,
    int tamano,
    String orden
) {
}
