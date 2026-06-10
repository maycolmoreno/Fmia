package com.farmamia.operations.dominio.modelo;

public record DatosRegistroAgente(
    String codigoSucursal,
    String nombreEquipo,
    String direccionIp,
    String direccionMac,
    String versionWindows,
    String versionAgente,
    String versionPos,
    String rutaPos
) {
}
