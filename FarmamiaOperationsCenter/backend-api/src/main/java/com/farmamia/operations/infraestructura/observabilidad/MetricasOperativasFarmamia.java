package com.farmamia.operations.infraestructura.observabilidad;

import com.farmamia.operations.infraestructura.persistencia.repositorio.AlertaRepositorioJpa;
import com.farmamia.operations.infraestructura.persistencia.repositorio.DespliegueRepositorioJpa;
import com.farmamia.operations.infraestructura.persistencia.repositorio.EquipoRepositorioJpa;
import com.farmamia.operations.infraestructura.persistencia.repositorio.EventoActualizacionRepositorioJpa;
import com.farmamia.operations.infraestructura.persistencia.repositorio.ObjetivoDespliegueRepositorioJpa;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

    private final MeterRegistry meterRegistry;
    private final Counter heartbeats;
    private final Counter descargasPaquetes;
    private final Map<String, Counter> eventosAgentePorTipo = new ConcurrentHashMap<>();
    private final Map<String, Counter> resultadosPorEstado = new ConcurrentHashMap<>();

    public MetricasOperativasFarmamia(
        MeterRegistry meterRegistry,
        EquipoRepositorioJpa equipos,
        DespliegueRepositorioJpa despliegues,
        EventoActualizacionRepositorioJpa eventos,
        AlertaRepositorioJpa alertas,
        ObjetivoDespliegueRepositorioJpa objetivos
    ) {
        this.meterRegistry = meterRegistry;
        this.heartbeats = Counter.builder("farmamia.agent.heartbeats.total")
            .description("Heartbeats aceptados desde agentes Windows")
            .register(meterRegistry);
        this.descargasPaquetes = Counter.builder("farmamia.package.downloads.total")
            .description("Descargas de paquetes POS aprobadas por la API")
            .register(meterRegistry);

        Gauge.builder("farmamia.devices.online", equipos, repositorio -> repositorio.countByEstado("ONLINE"))
            .description("Equipos Windows con estado ONLINE")
            .register(meterRegistry);

        Gauge.builder("farmamia.devices.offline", equipos, repositorio -> repositorio.countByEstado("OFFLINE"))
            .description("Equipos Windows con estado OFFLINE")
            .register(meterRegistry);

        Gauge.builder("farmamia.devices.total", equipos, EquipoRepositorioJpa::count)
            .description("Equipos Windows registrados")
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

        Gauge.builder("farmamia.orchestration.instruction.leases.expired", objetivos, repositorio -> repositorio.countByLeaseInstruccionHastaBefore(OffsetDateTime.now()))
            .description("Leases de instrucciones expirados y aun registrados en objetivos de despliegue")
            .register(meterRegistry);
    }

    public void registrarHeartbeat() {
        heartbeats.increment();
    }

    public void registrarEventoAgente(String tipoEvento) {
        contadorPorEtiqueta(
            eventosAgentePorTipo,
            "farmamia.agent.events.total",
            "event_type",
            tipoEvento,
            "Eventos operativos aceptados desde agentes Windows"
        ).increment();
    }

    public void registrarResultadoActualizacion(String estado) {
        contadorPorEtiqueta(
            resultadosPorEstado,
            "farmamia.agent.update.results.total",
            "status",
            estado,
            "Resultados de actualizacion aceptados desde agentes Windows"
        ).increment();
    }

    public void registrarDescargaPaquete() {
        descargasPaquetes.increment();
    }

    private Counter contadorPorEtiqueta(
        Map<String, Counter> cache,
        String nombre,
        String etiqueta,
        String valor,
        String descripcion
    ) {
        String valorNormalizado = normalizarEtiqueta(valor);
        return cache.computeIfAbsent(valorNormalizado, item -> Counter.builder(nombre)
            .tag(etiqueta, item)
            .description(descripcion)
            .register(meterRegistry));
    }

    private String normalizarEtiqueta(String valor) {
        if (valor == null || valor.isBlank()) {
            return "unknown";
        }
        return valor.trim().replaceAll("[^A-Za-z0-9_.-]", "_");
    }
}
