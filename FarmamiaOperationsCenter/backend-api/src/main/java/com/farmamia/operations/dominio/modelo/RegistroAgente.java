package com.farmamia.operations.dominio.modelo;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RegistroAgente(
    UUID idEquipo,
    String tokenAgente,
    OffsetDateTime horaServidor
) {
}
