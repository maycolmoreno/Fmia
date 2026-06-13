package com.farmamia.operations.dominio.modelo;

public record FiltroGruposTrx(
    String codigo,
    String estado,
    Boolean activo,
    int pagina,
    int tamano,
    String orden
) {
}
