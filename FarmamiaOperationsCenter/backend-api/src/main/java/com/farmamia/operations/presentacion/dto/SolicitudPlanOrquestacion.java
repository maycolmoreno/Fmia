package com.farmamia.operations.presentacion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalTime;

public record SolicitudPlanOrquestacion(
    @JsonProperty("maxFailurePercent") BigDecimal porcentajeMaximoFallo,
    @JsonProperty("autoPauseEnabled") Boolean pausaAutomaticaHabilitada,
    @JsonProperty("retryLimit") Integer limiteReintentos,
    @JsonProperty("maintenanceWindowStart") LocalTime ventanaInicio,
    @JsonProperty("maintenanceWindowEnd") LocalTime ventanaFin
) {
}
