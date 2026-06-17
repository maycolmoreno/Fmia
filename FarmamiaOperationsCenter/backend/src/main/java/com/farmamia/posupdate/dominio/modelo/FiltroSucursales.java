package com.farmamia.posupdate.dominio.modelo;

public record FiltroSucursales(
    String q,
    String codigo,
    String ciudad,
    String zona,
    Boolean deTurno,
    Boolean activa,
    int pagina,
    int tamano,
    String orden
) {
}
