package com.farmamia.operations.infraestructura.observabilidad;

import com.farmamia.operations.infraestructura.persistencia.repositorio.AlertaRepositorioJpa;
import com.farmamia.operations.infraestructura.persistencia.repositorio.DespliegueRepositorioJpa;
import com.farmamia.operations.infraestructura.persistencia.repositorio.EquipoRepositorioJpa;
import com.farmamia.operations.infraestructura.persistencia.repositorio.EventoActualizacionRepositorioJpa;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MetricasOperativasFarmamia {

    private static final List<String> ESTADOS_DESPLIEGUE_ACTIVO = List.of(
        "SCHEDULED",
        "PILOT_RUNNING",
        "APPROVED",
        "RUNNING"
    );

    private static final List<String> EVENTOS_FALLO_DESPLIEGUE = List.of(
        "FAILED",
        "VALIDATION_FAILED",
        "UPDATE_FAILED",
        "ROLLBACK_FAILED"
    );

    private static final List<String> EVENTOS_ROLLBACK = List.of(
        "ROLLBACK_STARTED",
        "ROLLBACK_COMPLETED",
        "ROLLBACK_FAILED"
    );

    public MetricasOperativasFarmamia(
        MeterRegistry meterRegistry,
        EquipoRepositorioJpa equipos,
        DespliegueRepositorioJpa despliegues,
        EventoActualizacionRepositorioJpa eventos,
        AlertaRepositorioJpa alertas
    ) {
        Gauge.builder("farmamia.devices.online", equipos, repositorio -> repositorio.countByEstado("ONLINE"))
            .description("Equipos Windows con estado ONLINE")
            .register(meterRegistry);

        Gauge.builder("farmamia.deployments.active", despliegues, repositorio -> repositorio.countByEstadoIn(ESTADOS_DESPLIEGUE_ACTIVO))
            .description("Despliegues activos o autorizados")
            .register(meterRegistry);

        Gauge.builder("farmamia.deployment.failures.total", eventos, repositorio -> repositorio.countByTipoEventoIn(EVENTOS_FALLO_DESPLIEGUE))
            .description("Eventos historicos de fallo de despliegue")
            .register(meterRegistry);

        Gauge.builder("farmamia.deployment.rollbacks.total", eventos, repositorio -> repositorio.countByTipoEventoIn(EVENTOS_ROLLBACK))
            .description("Eventos historicos de rollback")
            .register(meterRegistry);

        Gauge.builder("farmamia.alerts.critical.open", alertas, repositorio -> repositorio.countByEstadoAndSeveridad("OPEN", "CRITICAL"))
            .description("Alertas criticas abiertas")
            .register(meterRegistry);
    }
}
