package com.farmamia.operations.infraestructura.retencion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.farmamia.operations.infraestructura.persistencia.repositorio.AuditoriaRepositorioJpa;
import com.farmamia.operations.infraestructura.persistencia.repositorio.EventoActualizacionRepositorioJpa;
import com.farmamia.operations.infraestructura.persistencia.repositorio.MetricaEquipoRepositorioJpa;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class RetencionOperativaProgramadaTest {

    private final EventoActualizacionRepositorioJpa eventos = mock(EventoActualizacionRepositorioJpa.class);
    private final MetricaEquipoRepositorioJpa metricas = mock(MetricaEquipoRepositorioJpa.class);
    private final AuditoriaRepositorioJpa auditoria = mock(AuditoriaRepositorioJpa.class);
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-12T07:00:00Z"), ZoneOffset.UTC);

    @Test
    void eliminaRegistrosAnterioresALaPoliticaConfigurada() {
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        when(eventos.eliminarAnterioresA(ahora.minusDays(90))).thenReturn(3);
        when(metricas.eliminarAnterioresA(ahora.minusDays(30))).thenReturn(5);
        when(auditoria.eliminarAnterioresA(ahora.minusDays(365))).thenReturn(1);

        RetencionOperativaProgramada retencion = new RetencionOperativaProgramada(
            eventos,
            metricas,
            auditoria,
            meterRegistry,
            clock,
            true,
            90,
            30,
            365
        );

        retencion.ejecutar();

        verify(eventos).eliminarAnterioresA(ahora.minusDays(90));
        verify(metricas).eliminarAnterioresA(ahora.minusDays(30));
        verify(auditoria).eliminarAnterioresA(ahora.minusDays(365));
        assertThat(contador("events")).isEqualTo(3.0);
        assertThat(contador("metrics")).isEqualTo(5.0);
        assertThat(contador("audit")).isEqualTo(1.0);
    }

    @Test
    void noEliminaRegistrosCuandoEstaDeshabilitada() {
        RetencionOperativaProgramada retencion = new RetencionOperativaProgramada(
            eventos,
            metricas,
            auditoria,
            meterRegistry,
            clock,
            false,
            90,
            30,
            365
        );

        retencion.ejecutar();

        verify(eventos, never()).eliminarAnterioresA(org.mockito.ArgumentMatchers.any());
        verify(metricas, never()).eliminarAnterioresA(org.mockito.ArgumentMatchers.any());
        verify(auditoria, never()).eliminarAnterioresA(org.mockito.ArgumentMatchers.any());
    }

    private double contador(String dataset) {
        return meterRegistry
            .get("farmamia.retention.deleted.total")
            .tag("dataset", dataset)
            .counter()
            .count();
    }
}
