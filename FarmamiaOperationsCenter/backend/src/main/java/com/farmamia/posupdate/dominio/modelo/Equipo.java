package com.farmamia.posupdate.dominio.modelo;

import java.util.UUID;
import java.time.OffsetDateTime;

public record Equipo(
    UUID id,
    UUID idSucursal,
    String codigoSucursal,
    String nombreSucursal,
    String nombreEquipo,
    String tipo,
    String codigoPdv,
    String direccionIp,
    String comunidadSnmp,
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
    public Equipo(
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
        this(
            id,
            idSucursal,
            codigoSucursal,
            nombreSucursal,
            nombreEquipo,
            "POS_TERMINAL",
            null,
            direccionIp,
            null,
            direccionMac,
            versionWindows,
            versionAgente,
            versionPos,
            rutaPos,
            estado,
            ultimoLatidoEn,
            registradoEn,
            actualizadoEn
        );
    }
}
