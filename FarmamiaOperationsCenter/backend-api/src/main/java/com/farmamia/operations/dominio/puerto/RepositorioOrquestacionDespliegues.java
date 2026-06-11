package com.farmamia.operations.dominio.puerto;

import com.farmamia.operations.dominio.modelo.PlanOrquestacionDespliegue;
import com.farmamia.operations.dominio.modelo.SolicitudPlanOrquestacion;
import java.util.UUID;

public interface RepositorioOrquestacionDespliegues {

    PlanOrquestacionDespliegue planificar(UUID idDespliegue, SolicitudPlanOrquestacion solicitud);

    PlanOrquestacionDespliegue obtenerPlan(UUID idDespliegue);

    PlanOrquestacionDespliegue evaluar(UUID idDespliegue);

    PlanOrquestacionDespliegue iniciarOleada(UUID idDespliegue, UUID idOleada);

    PlanOrquestacionDespliegue pausarOleada(UUID idDespliegue, UUID idOleada);

    PlanOrquestacionDespliegue reanudarOleada(UUID idDespliegue, UUID idOleada);
}
