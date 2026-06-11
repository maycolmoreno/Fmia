package com.farmamia.operations.dominio.puerto;

import com.farmamia.operations.dominio.modelo.AlertaEquipo;
import com.farmamia.operations.dominio.modelo.AlertaRegistrada;
import com.farmamia.operations.dominio.modelo.FiltroAlertas;
import com.farmamia.operations.dominio.modelo.Pagina;
import java.util.List;
import java.util.UUID;

public interface RepositorioAlertas {

    void guardar(AlertaEquipo alerta);

    List<AlertaRegistrada> listarRecientes(int limite);

    List<AlertaRegistrada> listarConFiltros(FiltroAlertas filtro);

    Pagina<AlertaRegistrada> listarPaginado(FiltroAlertas filtro);

    AlertaRegistrada reconocer(UUID idAlerta, String usuarioActor);

    AlertaRegistrada cerrar(UUID idAlerta, String usuarioActor);
}
