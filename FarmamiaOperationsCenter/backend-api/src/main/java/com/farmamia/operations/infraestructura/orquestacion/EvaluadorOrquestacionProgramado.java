package com.farmamia.operations.infraestructura.orquestacion;

import com.farmamia.operations.aplicacion.casouso.OrquestarDesplieguesCasoUso;
import com.farmamia.operations.infraestructura.persistencia.entidad.EstadoControlDespliegueEntidad;
import com.farmamia.operations.infraestructura.persistencia.repositorio.EstadoControlDespliegueRepositorioJpa;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    prefix = "farmamia.orchestration.scheduler",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class EvaluadorOrquestacionProgramado {

    private static final Logger LOG = LoggerFactory.getLogger(EvaluadorOrquestacionProgramado.class);
    private static final String ESTADO_RUNNING = "RUNNING";

    private final EstadoControlDespliegueRepositorioJpa controlRepositorioJpa;
    private final OrquestarDesplieguesCasoUso orquestarDesplieguesCasoUso;
    private final Counter ciclosEjecutados;
    private final Counter desplieguesEvaluados;
    private final Counter erroresEvaluacion;
    private final MeterRegistry meterRegistry;

    public EvaluadorOrquestacionProgramado(
        EstadoControlDespliegueRepositorioJpa controlRepositorioJpa,
        OrquestarDesplieguesCasoUso orquestarDesplieguesCasoUso,
        MeterRegistry meterRegistry
    ) {
        this.controlRepositorioJpa = controlRepositorioJpa;
        this.orquestarDesplieguesCasoUso = orquestarDesplieguesCasoUso;
        this.meterRegistry = meterRegistry;
        this.ciclosEjecutados = Counter.builder("farmamia.orchestration.scheduler.cycles.total")
            .description("Ciclos ejecutados por el evaluador programado de orquestacion")
            .register(meterRegistry);
        this.desplieguesEvaluados = Counter.builder("farmamia.orchestration.scheduler.deployments.evaluated.total")
            .description("Despliegues evaluados por el scheduler de orquestacion")
            .register(meterRegistry);
        this.erroresEvaluacion = Counter.builder("farmamia.orchestration.scheduler.errors.total")
            .description("Errores capturados al evaluar despliegues desde el scheduler")
            .register(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${farmamia.orchestration.scheduler.fixed-delay-ms:60000}")
    public void evaluarDesplieguesActivos() {
        ciclosEjecutados.increment();
        List<EstadoControlDespliegueEntidad> controles = controlRepositorioJpa.findByEstado(ESTADO_RUNNING);
        for (EstadoControlDespliegueEntidad control : controles) {
            UUID idDespliegue = control.getIdDespliegue();
            try {
                orquestarDesplieguesCasoUso.evaluar(idDespliegue);
                desplieguesEvaluados.increment();
            } catch (RuntimeException ex) {
                erroresEvaluacion.increment();
                registrarErrorPorDespliegue(idDespliegue);
                LOG.warn("No se pudo evaluar orquestacion del despliegue {}", idDespliegue, ex);
            }
        }
    }

    private void registrarErrorPorDespliegue(UUID idDespliegue) {
        Counter.builder("farmamia.orchestration.scheduler.errors.by_deployment.total")
            .tag("deployment_id", idDespliegue.toString())
            .description("Errores del scheduler de orquestacion agrupados por despliegue")
            .register(meterRegistry)
            .increment();
    }
}
