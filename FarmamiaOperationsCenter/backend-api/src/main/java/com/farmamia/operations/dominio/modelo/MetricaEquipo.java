package com.farmamia.operations.dominio.modelo;

import java.math.BigDecimal;
import java.util.UUID;

public record MetricaEquipo(
    UUID idEquipo,
    String versionPos,
    Integer discoLibreMb,
    Integer discoTotalMb,
    Boolean procesoPosEjecutandose,
    Integer latenciaMs,
    BigDecimal porcentajePerdidaPaquetes,
    String estadoAgente
) {
}
