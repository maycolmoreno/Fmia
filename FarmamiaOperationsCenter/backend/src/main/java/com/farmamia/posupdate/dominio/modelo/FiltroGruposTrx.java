package com.farmamia.posupdate.dominio.modelo;

public record FiltroGruposTrx(
    String codigo,
    String estado,
    Boolean activo,
    int pagina,
    int tamano,
    String orden
) {
}
