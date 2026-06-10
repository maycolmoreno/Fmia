package com.farmamia.operations.dominio.modelo;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AlertaRegistrada(
    UUID id,
    UUID idEquipo,
    String nombreEquipo,
    UUID idSucursal,
    String codigoSucursal,
    String severidad,
    String tipoAlerta,
    String titulo,
    String mensaje,
    String estado,
    OffsetDateTime abiertaEn,
    String reconocidaPor,
    OffsetDateTime reconocidaEn,
    String cerradaPor,
    OffsetDateTime cerradaEn
) {
}
