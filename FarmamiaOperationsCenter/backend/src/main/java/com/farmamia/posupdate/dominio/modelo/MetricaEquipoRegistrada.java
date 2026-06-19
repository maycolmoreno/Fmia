package com.farmamia.posupdate.dominio.modelo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MetricaEquipoRegistrada(
    UUID id,
    String versionPos,
    Integer discoLibreMb,
    Integer discoTotalMb,
    Boolean procesoPosEjecutandose,
    Integer latenciaMs,
    BigDecimal porcentajePerdidaPaquetes,
    String estadoAgente,
    Integer usoCpuPorcentaje,
    Integer usoRamPorcentaje,
    Integer tiempoRespuestaMs,
    BigDecimal traficoInboundKbps,
    BigDecimal traficoOutboundKbps,
    Long uptimeRouterTicks,
    String descripcionRouter,
    OffsetDateTime recolectadoEn
) {
}
