package com.farmamia.operations.dominio.modelo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PlanOrquestacionDespliegue(
    UUID idDespliegue,
    String estadoControl,
    BigDecimal porcentajeMaximoFallo,
    boolean pausaAutomaticaHabilitada,
    int limiteReintentos,
    int siguienteNumeroOleada,
    String motivoPausa,
    OffsetDateTime evaluadoEn,
    List<OleadaOrquestacion> oleadas
) {
}
