package com.farmamia.operations.dominio.modelo;

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
