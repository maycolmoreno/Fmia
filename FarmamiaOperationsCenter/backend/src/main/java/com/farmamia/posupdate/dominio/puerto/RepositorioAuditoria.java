package com.farmamia.posupdate.dominio.puerto;

import com.farmamia.posupdate.dominio.modelo.AuditoriaRegistrada;
import com.farmamia.posupdate.dominio.modelo.DatosAuditoria;
import com.farmamia.posupdate.dominio.modelo.FiltroAuditoria;
import com.farmamia.posupdate.dominio.modelo.FiltroAuditoriaPaginada;
import com.farmamia.posupdate.dominio.modelo.Pagina;
import java.util.List;

public interface RepositorioAuditoria {

    void registrar(DatosAuditoria datos);

    List<AuditoriaRegistrada> listarRecientes(int limite);

    List<AuditoriaRegistrada> listarConFiltros(FiltroAuditoria filtro);

    Pagina<AuditoriaRegistrada> listarPaginado(FiltroAuditoriaPaginada filtro);
}
