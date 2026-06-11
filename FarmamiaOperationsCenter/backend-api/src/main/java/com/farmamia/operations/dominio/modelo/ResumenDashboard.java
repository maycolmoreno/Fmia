package com.farmamia.operations.dominio.modelo;

public record ResumenDashboard(
    long totalEquipos,
    long equiposOnline,
    long totalPaquetes,
    long paquetesAprobados,
    long totalDespliegues,
    long desplieguesActivos,
    long totalEventos,
    long eventosCriticos,
    long totalAlertas,
    long alertasAbiertas,
    long alertasCriticas
) {
}
