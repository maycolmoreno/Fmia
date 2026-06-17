package com.farmamia.posupdate.dominio.modelo;

public record DatosGrupoTrx(
    String codigo,
    String nombre,
    String descripcion,
    Integer maximoEquipos,
    Boolean activo
) {
}
