package com.farmamia.posupdate.dominio.puerto;

import com.farmamia.posupdate.dominio.modelo.AlertaEquipo;
import com.farmamia.posupdate.dominio.modelo.AlertaRed;
import com.farmamia.posupdate.dominio.modelo.AlertaRegistrada;
import com.farmamia.posupdate.dominio.modelo.FiltroAlertas;
import com.farmamia.posupdate.dominio.modelo.Pagina;
import java.util.List;
import java.util.UUID;

public interface RepositorioAlertas {

    void guardar(AlertaEquipo alerta);

    void guardarAlertaRed(AlertaRed alerta);

    List<AlertaRegistrada> listarRecientes(int limite);

    List<AlertaRegistrada> listarConFiltros(FiltroAlertas filtro);

    Pagina<AlertaRegistrada> listarPaginado(FiltroAlertas filtro);

    AlertaRegistrada reconocer(UUID idAlerta, String usuarioActor);

    AlertaRegistrada cerrar(UUID idAlerta, String usuarioActor);
}
