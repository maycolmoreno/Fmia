package com.farmamia.posupdate.dominio.modelo;

import java.time.OffsetDateTime;
import java.util.UUID;

public record GrupoTrx(
    UUID id,
    String codigo,
    String nombre,
    String descripcion,
    EstadoGrupoTrx estado,
    int maximoEquipos,
    boolean activo,
    long equiposAsignados,
    long farmaciasInvolucradas,
    OffsetDateTime creadoEn,
    OffsetDateTime actualizadoEn
) {
}
