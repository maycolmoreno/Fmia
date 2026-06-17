package com.farmamia.posupdate.infraestructura.orquestacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.farmamia.posupdate.aplicacion.casouso.OrquestarDesplieguesCasoUso;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.EstadoControlDespliegueEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.EstadoControlDespliegueRepositorioJpa;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EvaluadorOrquestacionProgramadoTest {

    @Test
    void evaluarDesplieguesActivosRegistraMetricasDeCicloExitosYErrores() {
        EstadoControlDespliegueRepositorioJpa repositorio = mock(EstadoControlDespliegueRepositorioJpa.class);
        OrquestarDesplieguesCasoUso casoUso = mock(OrquestarDesplieguesCasoUso.class);
        SimpleMeterRegistry metricas = new SimpleMeterRegistry();

        UUID idExitoso = UUID.randomUUID();
        UUID idFallido = UUID.randomUUID();
        EstadoControlDespliegueEntidad controlExitoso = mock(EstadoControlDespliegueEntidad.class);
        EstadoControlDespliegueEntidad controlFallido = mock(EstadoControlDespliegueEntidad.class);
        when(controlExitoso.getIdDespliegue()).thenReturn(idExitoso);
        when(controlFallido.getIdDespliegue()).thenReturn(idFallido);
        when(repositorio.findByEstado("RUNNING")).thenReturn(List.of(controlExitoso, controlFallido));
        doThrow(new IllegalStateException("fallo simulado")).when(casoUso).evaluar(idFallido);

        EvaluadorOrquestacionProgramado evaluador = new EvaluadorOrquestacionProgramado(
            repositorio,
            casoUso,
            metricas
        );

        evaluador.evaluarDesplieguesActivos();

        verify(casoUso).evaluar(idExitoso);
        verify(casoUso).evaluar(idFallido);
        assertEquals(1.0, metricas.counter("farmamia.orchestration.scheduler.cycles.total").count());
        assertEquals(1.0, metricas.counter("farmamia.orchestration.scheduler.deployments.evaluated.total").count());
        assertEquals(1.0, metricas.counter("farmamia.orchestration.scheduler.errors.total").count());
        assertEquals(
            1.0,
            metricas.counter(
                "farmamia.orchestration.scheduler.errors.by_deployment.total",
                "deployment_id",
                idFallido.toString()
            ).count()
        );
    }
}
