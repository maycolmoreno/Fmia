package com.farmamia.operations.dominio.modelo;

import java.util.UUID;

public record AlertaEquipo(
    UUID idEquipo,
    String severidad,
    String tipoAlerta,
    String titulo,
    String mensaje
) {
}
