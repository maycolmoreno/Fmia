package com.farmamia.posupdate.infraestructura.retencion;

import com.farmamia.posupdate.infraestructura.persistencia.repositorio.AuditoriaRepositorioJpa;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.EventoActualizacionRepositorioJpa;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.MetricaEquipoRepositorioJpa;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RetencionOperativaProgramada {

    private static final Logger LOG = LoggerFactory.getLogger(RetencionOperativaProgramada.class);

    private final EventoActualizacionRepositorioJpa eventos;
    private final MetricaEquipoRepositorioJpa metricas;
    private final AuditoriaRepositorioJpa auditoria;
    private final Clock clock;
    private final boolean habilitada;
    private final Duration retencionEventos;
    private final Duration retencionMetricas;
    private final Duration retencionAuditoria;
    private final Counter eventosEliminados;
    private final Counter metricasEliminadas;
    private final Counter auditoriaEliminada;

    @Autowired
    public RetencionOperativaProgramada(
        EventoActualizacionRepositorioJpa eventos,
        MetricaEquipoRepositorioJpa metricas,
        AuditoriaRepositorioJpa auditoria,
        MeterRegistry meterRegistry,
        @Value("${farmamia.retention.enabled:true}") boolean habilitada,
        @Value("${farmamia.retention.events-days:90}") int diasEventos,
        @Value("${farmamia.retention.metrics-days:30}") int diasMetricas,
        @Value("${farmamia.retention.audit-days:365}") int diasAuditoria
    ) {
        this(eventos, metricas, auditoria, meterRegistry, Clock.systemUTC(), habilitada, diasEventos, diasMetricas, diasAuditoria);
    }

    RetencionOperativaProgramada(
        EventoActualizacionRepositorioJpa eventos,
        MetricaEquipoRepositorioJpa metricas,
        AuditoriaRepositorioJpa auditoria,
        MeterRegistry meterRegistry,
        Clock clock,
        boolean habilitada,
        int diasEventos,
        int diasMetricas,
        int diasAuditoria
    ) {
        this.eventos = eventos;
        this.metricas = metricas;
        this.auditoria = auditoria;
        this.clock = clock;
        this.habilitada = habilitada;
        this.retencionEventos = Duration.ofDays(Math.max(1, diasEventos));
        this.retencionMetricas = Duration.ofDays(Math.max(1, diasMetricas));
        this.retencionAuditoria = Duration.ofDays(Math.max(1, diasAuditoria));
        this.eventosEliminados = contador(meterRegistry, "events");
        this.metricasEliminadas = contador(meterRegistry, "metrics");
        this.auditoriaEliminada = contador(meterRegistry, "audit");
    }

    @Scheduled(cron = "${farmamia.retention.cron:0 20 2 * * *}")
    @Transactional
    public void ejecutar() {
        if (!habilitada) {
            LOG.debug("Retencion operativa deshabilitada.");
            return;
        }

        OffsetDateTime ahora = OffsetDateTime.now(clock);
        int eventosPurgados = eventos.eliminarAnterioresA(ahora.minus(retencionEventos));
        int metricasPurgadas = metricas.eliminarAnterioresA(ahora.minus(retencionMetricas));
        int auditoriaPurgada = auditoria.eliminarAnterioresA(ahora.minus(retencionAuditoria));

        eventosEliminados.increment(eventosPurgados);
        metricasEliminadas.increment(metricasPurgadas);
        auditoriaEliminada.increment(auditoriaPurgada);

        if (eventosPurgados > 0 || metricasPurgadas > 0 || auditoriaPurgada > 0) {
            LOG.info(
                "Retencion operativa purgo eventos={}, metricas={}, auditoria={}.",
                eventosPurgados,
                metricasPurgadas,
                auditoriaPurgada
            );
        }
    }

    private Counter contador(MeterRegistry meterRegistry, String dataset) {
        return Counter.builder("farmamia.retention.deleted.total")
            .description("Registros eliminados por politica de retencion operativa")
            .tag("dataset", dataset)
            .register(meterRegistry);
    }
}
