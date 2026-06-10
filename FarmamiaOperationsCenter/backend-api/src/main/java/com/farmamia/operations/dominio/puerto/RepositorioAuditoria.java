package com.farmamia.operations.dominio.puerto;

import com.farmamia.operations.dominio.modelo.AuditoriaRegistrada;
import com.farmamia.operations.dominio.modelo.DatosAuditoria;
import com.farmamia.operations.dominio.modelo.FiltroAuditoria;
import java.util.List;

public interface RepositorioAuditoria {

    void registrar(DatosAuditoria datos);

    List<AuditoriaRegistrada> listarRecientes(int limite);

    List<AuditoriaRegistrada> listarConFiltros(FiltroAuditoria filtro);
}
