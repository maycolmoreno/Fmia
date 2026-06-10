package com.farmamia.operations.dominio.modelo;

import java.time.OffsetDateTime;
import java.util.UUID;

public record Despliegue(
    UUID id,
    UUID idPaquete,
    String versionPaquete,
    String nombre,
    String descripcion,
    String estado,
    OffsetDateTime programadoEn,
    OffsetDateTime creadoEn,
    long cantidadObjetivos
) {
}
