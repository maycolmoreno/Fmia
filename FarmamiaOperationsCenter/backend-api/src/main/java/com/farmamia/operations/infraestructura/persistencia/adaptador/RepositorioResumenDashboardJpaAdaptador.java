package com.farmamia.operations.infraestructura.persistencia.adaptador;

import com.farmamia.operations.dominio.modelo.ResumenDashboard;
import com.farmamia.operations.dominio.puerto.RepositorioResumenDashboard;
import com.farmamia.operations.infraestructura.persistencia.repositorio.AlertaRepositorioJpa;
import com.farmamia.operations.infraestructura.persistencia.repositorio.DespliegueRepositorioJpa;
import com.farmamia.operations.infraestructura.persistencia.repositorio.EquipoRepositorioJpa;
import com.farmamia.operations.infraestructura.persistencia.repositorio.EventoActualizacionRepositorioJpa;
import com.farmamia.operations.infraestructura.persistencia.repositorio.PaquetePosRepositorioJpa;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioResumenDashboardJpaAdaptador implements RepositorioResumenDashboard {

    private static final List<String> ESTADOS_DESPLIEGUE_ACTIVO = List.of(
        "SCHEDULED",
        "PILOT_RUNNING",
        "APPROVED",
        "RUNNING"
    );

    private static final List<String> EVENTOS_CRITICOS = List.of(
        "FAILED",
        "VALIDATION_FAILED",
        "ROLLBACK_STARTED",
        "ROLLBACK_COMPLETED",
        "ROLLBACK_FAILED"
    );

    private final EquipoRepositorioJpa equipoRepositorioJpa;
    private final PaquetePosRepositorioJpa paquetePosRepositorioJpa;
    private final DespliegueRepositorioJpa despliegueRepositorioJpa;
    private final EventoActualizacionRepositorioJpa eventoActualizacionRepositorioJpa;
    private final AlertaRepositorioJpa alertaRepositorioJpa;

    public RepositorioResumenDashboardJpaAdaptador(
        EquipoRepositorioJpa equipoRepositorioJpa,
        PaquetePosRepositorioJpa paquetePosRepositorioJpa,
        DespliegueRepositorioJpa despliegueRepositorioJpa,
        EventoActualizacionRepositorioJpa eventoActualizacionRepositorioJpa,
        AlertaRepositorioJpa alertaRepositorioJpa
    ) {
        this.equipoRepositorioJpa = equipoRepositorioJpa;
        this.paquetePosRepositorioJpa = paquetePosRepositorioJpa;
        this.despliegueRepositorioJpa = despliegueRepositorioJpa;
        this.eventoActualizacionRepositorioJpa = eventoActualizacionRepositorioJpa;
        this.alertaRepositorioJpa = alertaRepositorioJpa;
    }

    @Override
    public ResumenDashboard obtener() {
        return new ResumenDashboard(
            equipoRepositorioJpa.count(),
            equipoRepositorioJpa.countByEstado("ONLINE"),
            paquetePosRepositorioJpa.count(),
            paquetePosRepositorioJpa.countByEstado("APPROVED"),
            despliegueRepositorioJpa.count(),
            despliegueRepositorioJpa.countByEstadoIn(ESTADOS_DESPLIEGUE_ACTIVO),
            eventoActualizacionRepositorioJpa.count(),
            eventoActualizacionRepositorioJpa.countByTipoEventoIn(EVENTOS_CRITICOS),
            alertaRepositorioJpa.count(),
            alertaRepositorioJpa.countByEstado("OPEN"),
            alertaRepositorioJpa.countBySeveridad("CRITICAL")
        );
    }
}
