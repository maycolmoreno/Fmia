package com.farmamia.posupdate.dominio.modelo;

public record FiltroEstadoCampanaFarmacia(
    String estadoTecnico,
    String estadoOperacional,
    String grupoTrx,
    Boolean deTurno,
    String q,
    int pagina,
    int tamano,
    String orden
) {
}
