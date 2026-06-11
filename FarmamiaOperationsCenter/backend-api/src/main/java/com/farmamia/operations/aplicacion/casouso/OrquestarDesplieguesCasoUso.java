package com.farmamia.operations.aplicacion.casouso;

import com.farmamia.operations.dominio.modelo.PlanOrquestacionDespliegue;
import com.farmamia.operations.dominio.modelo.SolicitudPlanOrquestacion;
import com.farmamia.operations.dominio.puerto.RepositorioOrquestacionDespliegues;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class OrquestarDesplieguesCasoUso {

    private final RepositorioOrquestacionDespliegues repositorioOrquestacion;

    public OrquestarDesplieguesCasoUso(RepositorioOrquestacionDespliegues repositorioOrquestacion) {
        this.repositorioOrquestacion = repositorioOrquestacion;
    }

    @Transactional
    public PlanOrquestacionDespliegue planificar(UUID idDespliegue, SolicitudPlanOrquestacion solicitud) {
        return repositorioOrquestacion.planificar(idDespliegue, normalizar(solicitud));
    }

    public PlanOrquestacionDespliegue obtenerPlan(UUID idDespliegue) {
        return repositorioOrquestacion.obtenerPlan(idDespliegue);
    }

    @Transactional
    public PlanOrquestacionDespliegue evaluar(UUID idDespliegue) {
        return repositorioOrquestacion.evaluar(idDespliegue);
    }

    @Transactional
    public PlanOrquestacionDespliegue iniciarOleada(UUID idDespliegue, UUID idOleada) {
        return repositorioOrquestacion.iniciarOleada(idDespliegue, idOleada);
    }

    @Transactional
    public PlanOrquestacionDespliegue pausarOleada(UUID idDespliegue, UUID idOleada) {
        return repositorioOrquestacion.pausarOleada(idDespliegue, idOleada);
    }

    @Transactional
    public PlanOrquestacionDespliegue reanudarOleada(UUID idDespliegue, UUID idOleada) {
        return repositorioOrquestacion.reanudarOleada(idDespliegue, idOleada);
    }

    private SolicitudPlanOrquestacion normalizar(SolicitudPlanOrquestacion solicitud) {
        BigDecimal porcentaje = solicitud.porcentajeMaximoFallo() == null
            ? BigDecimal.TEN
            : solicitud.porcentajeMaximoFallo().max(BigDecimal.ZERO).min(BigDecimal.valueOf(100));
        int limiteReintentos = Math.max(0, Math.min(solicitud.limiteReintentos(), 10));
        LocalTime ventanaInicio = solicitud.ventanaInicio();
        LocalTime ventanaFin = solicitud.ventanaFin();
        return new SolicitudPlanOrquestacion(
            porcentaje,
            solicitud.pausaAutomaticaHabilitada(),
            limiteReintentos,
            ventanaInicio,
            ventanaFin
        );
    }
}
