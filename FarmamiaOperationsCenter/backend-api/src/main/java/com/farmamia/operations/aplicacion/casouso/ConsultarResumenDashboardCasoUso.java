package com.farmamia.operations.aplicacion.casouso;

import com.farmamia.operations.dominio.modelo.ResumenDashboard;
import com.farmamia.operations.dominio.puerto.RepositorioResumenDashboard;
import org.springframework.stereotype.Service;

@Service
public class ConsultarResumenDashboardCasoUso {

    private final RepositorioResumenDashboard repositorioResumenDashboard;

    public ConsultarResumenDashboardCasoUso(RepositorioResumenDashboard repositorioResumenDashboard) {
        this.repositorioResumenDashboard = repositorioResumenDashboard;
    }

    public ResumenDashboard obtener() {
        return repositorioResumenDashboard.obtener();
    }
}
