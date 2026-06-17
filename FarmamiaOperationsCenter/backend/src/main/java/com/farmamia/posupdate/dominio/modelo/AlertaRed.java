package com.farmamia.posupdate.dominio.modelo;

public record AlertaRed(
    String codigoSucursal,
    String severidad,
    String tipoAlerta,
    String titulo,
    String mensaje
) {
}
