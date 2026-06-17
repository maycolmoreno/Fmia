package com.farmamia.posupdate.presentacion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record RespuestaDetalleEquipo(
    @JsonProperty("device") RespuestaEquipo equipo,
    @JsonProperty("lastMetric") RespuestaMetricaEquipo ultimaMetrica,
    @JsonProperty("recentEvents") List<RespuestaEventoActualizacion> eventosRecientes,
    @JsonProperty("deployments") List<RespuestaObjetivoEquipo> despliegues
) {
}
