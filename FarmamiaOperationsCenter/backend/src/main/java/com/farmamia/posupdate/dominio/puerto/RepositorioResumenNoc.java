package com.farmamia.posupdate.dominio.puerto;

import com.farmamia.posupdate.dominio.modelo.ResumenNocDashboard;
import java.util.List;
import java.util.UUID;

public interface RepositorioResumenNoc {

    ResumenNocDashboard.EstadoRedNoc obtenerEstadoRed();

    List<ResumenNocDashboard.EnlaceCaidoNoc> obtenerEnlacesCaidos(int limite);

    ResumenNocDashboard.EstadoPosNoc obtenerEstadoPos();

    List<ResumenNocDashboard.EquipoSinActualizarNoc> obtenerEquiposSinActualizar(UUID idCampanaActiva, int limite);

    ResumenNocDashboard.CampanaActivaNoc obtenerCampanaActiva();

    List<ResumenNocDashboard.AlertaResumenNoc> obtenerAlertasRecientes(int limite);
}
