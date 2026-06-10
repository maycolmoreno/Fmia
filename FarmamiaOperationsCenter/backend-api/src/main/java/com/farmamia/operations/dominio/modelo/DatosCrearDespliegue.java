package com.farmamia.operations.dominio.modelo;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record DatosCrearDespliegue(
    UUID idPaquete,
    String nombre,
    String descripcion,
    OffsetDateTime programadoEn,
    String grupoObjetivo,
    boolean piloto,
    List<UUID> idsEquipos
) {
}
