package com.farmamia.operations.dominio.modelo;

public record DatosGrupoTrx(
    String codigo,
    String nombre,
    String descripcion,
    Integer maximoEquipos,
    Boolean activo
) {
}
