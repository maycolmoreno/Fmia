package com.farmamia.posupdate.dominio.modelo;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RegistroAgente(
    UUID idEquipo,
    String tokenAgente,
    OffsetDateTime horaServidor
) {
}
