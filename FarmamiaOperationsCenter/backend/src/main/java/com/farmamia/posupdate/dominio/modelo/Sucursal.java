package com.farmamia.posupdate.dominio.modelo;

import java.time.OffsetDateTime;
import java.util.UUID;

public record Sucursal(
    UUID id,
    String codigo,
    String nombre,
    String ciudad,
    String zona,
    String direccion,
    Double latitud,
    Double longitud,
    boolean deTurno,
    boolean activa,
    OffsetDateTime creadoEn,
    OffsetDateTime actualizadoEn
) {
}
