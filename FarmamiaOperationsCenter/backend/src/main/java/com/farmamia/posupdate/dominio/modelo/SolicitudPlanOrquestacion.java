package com.farmamia.posupdate.dominio.modelo;

import java.math.BigDecimal;
import java.time.LocalTime;

public record SolicitudPlanOrquestacion(
    BigDecimal porcentajeMaximoFallo,
    boolean pausaAutomaticaHabilitada,
    int limiteReintentos,
    int maximoEquiposParalelos,
    LocalTime ventanaInicio,
    LocalTime ventanaFin
) {
}
