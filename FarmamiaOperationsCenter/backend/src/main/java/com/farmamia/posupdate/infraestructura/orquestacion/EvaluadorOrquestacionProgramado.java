package com.farmamia.posupdate.infraestructura.orquestacion;

import com.farmamia.posupdate.aplicacion.casouso.OrquestarDesplieguesCasoUso;
import com.farmamia.posupdate.dominio.modelo.OleadaOrquestacion;
import com.farmamia.posupdate.dominio.modelo.PlanOrquestacionDespliegue;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.EstadoControlDespliegueEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.ObjetivoDespliegueEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.EstadoControlDespliegueRepositorioJpa;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.ObjetivoDespliegueRepositorioJpa;
import com.farmamia.posupdate.infraestructura.sse.CanalSseAgentes;
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

    private static final String ESTADO_OLEADA_RUNNING = "RUNNING";

    private final EstadoControlDespliegueRepositorioJpa controlRepositorioJpa;
    private final OrquestarDesplieguesCasoUso orquestarDesplieguesCasoUso;
    private final ObjetivoDespliegueRepositorioJpa objetivoRepositorioJpa;
    private final CanalSseAgentes canalSseAgentes;
    private final Counter ciclosEjecutados;
    private final Counter desplieguesEvaluados;
    private final Counter erroresEvaluacion;
    private final Counter notificacionesEnviadas;
    private final MeterRegistry meterRegistry;

    public EvaluadorOrquestacionProgramado(
        EstadoControlDespliegueRepositorioJpa controlRepositorioJpa,
        OrquestarDesplieguesCasoUso orquestarDesplieguesCasoUso,
        ObjetivoDespliegueRepositorioJpa objetivoRepositorioJpa,
        CanalSseAgentes canalSseAgentes,
        MeterRegistry meterRegistry
    ) {
        this.controlRepositorioJpa = controlRepositorioJpa;
        this.orquestarDesplieguesCasoUso = orquestarDesplieguesCasoUso;
        this.objetivoRepositorioJpa = objetivoRepositorioJpa;
        this.canalSseAgentes = canalSseAgentes;
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
        this.notificacionesEnviadas = Counter.builder("farmamia.orchestration.scheduler.notifications.sent.total")
            .description("Notificaciones SSE selectivas enviadas a agentes conectados")
            .register(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${farmamia.orchestration.scheduler.fixed-delay-ms:60000}")
    public void evaluarDesplieguesActivos() {
        ciclosEjecutados.increment();
        List<EstadoControlDespliegueEntidad> controles = controlRepositorioJpa.findByEstado(ESTADO_RUNNING);
        for (EstadoControlDespliegueEntidad control : controles) {
            UUID idDespliegue = control.getIdDespliegue();
            try {
                PlanOrquestacionDespliegue plan = orquestarDesplieguesCasoUso.evaluar(idDespliegue);
                desplieguesEvaluados.increment();
                notificarObjetivosConectados(plan);
            } catch (RuntimeException ex) {
                erroresEvaluacion.increment();
                LOG.warn("No se pudo evaluar orquestacion del despliegue {}", idDespliegue, ex);
                try {
                    registrarErrorPorDespliegue(idDespliegue);
                } catch (RuntimeException metricEx) {
                    LOG.trace("No se pudo registrar metrica de error para despliegue {}", idDespliegue, metricEx);
                }
            }
        }
    }

    /**
     * Para cada oleada RUNNING (no piloto) del plan, consulta sus objetivos
     * y notifica únicamente a los agentes que tienen una conexión SSE activa.
     * Los agentes offline conservan la instrucción encolada en BD y la recibirán
     * en su próxima reconexión al consumir el endpoint de instrucciones.
     */
    private void notificarObjetivosConectados(PlanOrquestacionDespliegue plan) {
        if (plan.oleadas() == null) return;

        List<UUID> oleadasActivas = plan.oleadas().stream()
            .filter(o -> ESTADO_OLEADA_RUNNING.equals(o.estado()) && !o.piloto())
            .map(OleadaOrquestacion::id)
            .toList();

        if (oleadasActivas.isEmpty()) return;

        for (UUID idOleada : oleadasActivas) {
            List<ObjetivoDespliegueEntidad> objetivos = objetivoRepositorioJpa.findByOleada_Id(idOleada);
            for (ObjetivoDespliegueEntidad objetivo : objetivos) {
                if (objetivo.getEquipo() == null || objetivo.getEquipo().getId() == null) {
                    LOG.trace("Objetivo de oleada {} sin equipo asociado; se omite notificacion SSE", idOleada);
                    continue;
                }
                UUID idEquipo = objetivo.getEquipo().getId();
                if (canalSseAgentes.estaAgenteConectado(idEquipo)) {
                    canalSseAgentes.notificarInstruccionDisponible(idEquipo);
                    notificacionesEnviadas.increment();
                    LOG.trace("Notificacion SSE enviada al agente {} (oleada {})", idEquipo, idOleada);
                } else {
                    LOG.trace("Agente {} offline — instruccion encolada en BD, se entregara al reconectar (oleada {})",
                        idEquipo, idOleada);
                }
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
