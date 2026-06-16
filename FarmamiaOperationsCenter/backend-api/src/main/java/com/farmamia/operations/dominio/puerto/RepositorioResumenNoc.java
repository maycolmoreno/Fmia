package com.farmamia.operations.dominio.puerto;

import com.farmamia.operations.dominio.modelo.ResumenNocDashboard;
import java.util.List;

public interface RepositorioResumenNoc {

    ResumenNocDashboard.EstadoRedNoc obtenerEstadoRed();

    ResumenNocDashboard.EstadoPosNoc obtenerEstadoPos();

    ResumenNocDashboard.CampanaActivaNoc obtenerCampanaActiva();

    List<ResumenNocDashboard.AlertaResumenNoc> obtenerAlertasRecientes(int limite);
}
