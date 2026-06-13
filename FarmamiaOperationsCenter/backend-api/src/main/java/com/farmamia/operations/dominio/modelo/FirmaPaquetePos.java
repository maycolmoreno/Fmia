package com.farmamia.operations.dominio.modelo;

import java.time.OffsetDateTime;

public record FirmaPaquetePos(
    String firma,
    String algoritmo,
    String idClave,
    String clavePublicaPem,
    OffsetDateTime firmadoEn,
    String estado
) {
}
