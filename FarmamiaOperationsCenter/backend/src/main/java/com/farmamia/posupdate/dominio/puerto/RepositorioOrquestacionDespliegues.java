package com.farmamia.posupdate.dominio.puerto;

import com.farmamia.posupdate.dominio.modelo.PlanOrquestacionDespliegue;
import com.farmamia.posupdate.dominio.modelo.SolicitudPlanOrquestacion;
import java.util.UUID;

public interface RepositorioOrquestacionDespliegues {

    PlanOrquestacionDespliegue planificar(UUID idDespliegue, SolicitudPlanOrquestacion solicitud);

    PlanOrquestacionDespliegue obtenerPlan(UUID idDespliegue);

    PlanOrquestacionDespliegue evaluar(UUID idDespliegue);

    PlanOrquestacionDespliegue iniciarOleada(UUID idDespliegue, UUID idOleada);

    PlanOrquestacionDespliegue pausarOleada(UUID idDespliegue, UUID idOleada);

    PlanOrquestacionDespliegue reanudarOleada(UUID idDespliegue, UUID idOleada);
}
