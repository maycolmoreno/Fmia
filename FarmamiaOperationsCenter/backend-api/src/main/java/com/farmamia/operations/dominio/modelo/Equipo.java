package com.farmamia.operations.dominio.modelo;

import java.util.UUID;
import java.time.OffsetDateTime;

public record Equipo(
    UUID id,
    UUID idSucursal,
    String codigoSucursal,
    String nombreSucursal,
    String nombreEquipo,
    String direccionIp,
    String direccionMac,
    String versionWindows,
    String versionAgente,
    String versionPos,
    String rutaPos,
    String estado,
    OffsetDateTime ultimoLatidoEn,
    OffsetDateTime registradoEn,
    OffsetDateTime actualizadoEn
) {
}
