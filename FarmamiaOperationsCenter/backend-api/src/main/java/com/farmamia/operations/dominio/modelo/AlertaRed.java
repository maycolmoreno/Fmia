package com.farmamia.operations.dominio.modelo;

public record AlertaRed(
    String codigoSucursal,
    String severidad,
    String tipoAlerta,
    String titulo,
    String mensaje
) {
}
