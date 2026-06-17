package com.farmamia.posupdate.dominio.modelo;

import java.math.BigDecimal;
import java.util.UUID;

public record DatosLatido(
    UUID idEquipo,
    String versionPos,
    Integer discoLibreMb,
    Integer discoTotalMb,
    Boolean procesoPosEjecutandose,
    Integer latenciaMs,
    BigDecimal porcentajePerdidaPaquetes
) {
}
