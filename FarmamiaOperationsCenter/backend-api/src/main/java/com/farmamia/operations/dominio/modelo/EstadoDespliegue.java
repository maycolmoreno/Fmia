package com.farmamia.operations.dominio.modelo;

import java.util.Map;
import java.util.UUID;

public record EstadoDespliegue(
    UUID idDespliegue,
    String estado,
    Map<String, Long> objetivosPorEstado
) {
}
