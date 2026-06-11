package com.farmamia.operations.presentacion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RespuestaResumenDashboard(
    @JsonProperty("totalDevices") long totalEquipos,
    @JsonProperty("onlineDevices") long equiposOnline,
    @JsonProperty("totalPackages") long totalPaquetes,
    @JsonProperty("approvedPackages") long paquetesAprobados,
    @JsonProperty("totalDeployments") long totalDespliegues,
    @JsonProperty("activeDeployments") long desplieguesActivos,
    @JsonProperty("totalEvents") long totalEventos,
    @JsonProperty("criticalEvents") long eventosCriticos,
    @JsonProperty("totalAlerts") long totalAlertas,
    @JsonProperty("openAlerts") long alertasAbiertas,
    @JsonProperty("criticalAlerts") long alertasCriticas
) {
}
