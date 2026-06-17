package com.farmamia.posupdate.dominio.modelo;

import java.util.List;

public record DetalleEquipo(
    Equipo equipo,
    MetricaEquipoRegistrada ultimaMetrica,
    List<EventoActualizacionRegistrado> eventosRecientes,
    List<ObjetivoDespliegueEquipo> despliegues
) {
}
