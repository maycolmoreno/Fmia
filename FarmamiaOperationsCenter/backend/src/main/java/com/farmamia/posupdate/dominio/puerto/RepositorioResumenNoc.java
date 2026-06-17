package com.farmamia.posupdate.dominio.puerto;

import com.farmamia.posupdate.dominio.modelo.ResumenNocDashboard;
import java.util.List;

public interface RepositorioResumenNoc {

    ResumenNocDashboard.EstadoRedNoc obtenerEstadoRed();

    ResumenNocDashboard.EstadoPosNoc obtenerEstadoPos();

    ResumenNocDashboard.CampanaActivaNoc obtenerCampanaActiva();

    List<ResumenNocDashboard.AlertaResumenNoc> obtenerAlertasRecientes(int limite);
}
