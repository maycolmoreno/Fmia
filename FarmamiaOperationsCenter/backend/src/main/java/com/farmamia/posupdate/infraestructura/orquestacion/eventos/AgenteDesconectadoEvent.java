package com.farmamia.posupdate.infraestructura.orquestacion.eventos;

import java.time.Instant;
import java.util.UUID;

public record AgenteDesconectadoEvent(
    UUID idEquipo,
    Instant momento
) {
}
