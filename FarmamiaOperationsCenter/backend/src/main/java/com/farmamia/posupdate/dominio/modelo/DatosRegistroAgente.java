package com.farmamia.posupdate.dominio.modelo;

public record DatosRegistroAgente(
    String codigoSucursal,
    String nombreEquipo,
    String direccionIp,
    String codigoPdv,
    String comunidadSnmp,
    String direccionMac,
    String versionWindows,
    String versionAgente,
    String versionPos,
    String rutaPos
) {
    public DatosRegistroAgente(
        String codigoSucursal,
        String nombreEquipo,
        String direccionIp,
        String direccionMac,
        String versionWindows,
        String versionAgente,
        String versionPos,
        String rutaPos
    ) {
        this(codigoSucursal, nombreEquipo, direccionIp, null, null, direccionMac, versionWindows, versionAgente, versionPos, rutaPos);
    }
}
