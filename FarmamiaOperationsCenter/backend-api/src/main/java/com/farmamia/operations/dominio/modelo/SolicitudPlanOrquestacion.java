package com.farmamia.operations.dominio.modelo;

import java.math.BigDecimal;
import java.time.LocalTime;

public record SolicitudPlanOrquestacion(
    BigDecimal porcentajeMaximoFallo,
    boolean pausaAutomaticaHabilitada,
    int limiteReintentos,
    LocalTime ventanaInicio,
    LocalTime ventanaFin
) {
}
