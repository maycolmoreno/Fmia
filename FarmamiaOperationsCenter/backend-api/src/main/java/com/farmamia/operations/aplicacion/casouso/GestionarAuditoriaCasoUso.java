package com.farmamia.operations.aplicacion.casouso;

import com.farmamia.operations.dominio.modelo.AuditoriaRegistrada;
import com.farmamia.operations.dominio.modelo.DatosAuditoria;
import com.farmamia.operations.dominio.modelo.FiltroAuditoria;
import com.farmamia.operations.dominio.modelo.FiltroAuditoriaPaginada;
import com.farmamia.operations.dominio.modelo.Pagina;
import com.farmamia.operations.dominio.puerto.RepositorioAuditoria;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GestionarAuditoriaCasoUso {

    private final RepositorioAuditoria repositorioAuditoria;

    public GestionarAuditoriaCasoUso(RepositorioAuditoria repositorioAuditoria) {
        this.repositorioAuditoria = repositorioAuditoria;
    }

    @Transactional
    public void registrar(DatosAuditoria datos) {
        repositorioAuditoria.registrar(datos);
    }

    @Transactional(readOnly = true)
    public List<AuditoriaRegistrada> listarRecientes(int limite) {
        int limiteNormalizado = Math.max(1, Math.min(limite, 200));
        return repositorioAuditoria.listarRecientes(limiteNormalizado);
    }

    @Transactional(readOnly = true)
    public List<AuditoriaRegistrada> listarConFiltros(FiltroAuditoria filtro) {
        FiltroAuditoria normalizado = new FiltroAuditoria(
            limpiar(filtro.accion()),
            limpiar(filtro.tipoEntidad()),
            limpiar(filtro.usuarioActor()),
            filtro.desde(),
            filtro.hasta(),
            Math.max(1, Math.min(filtro.limite(), 200))
        );
        return repositorioAuditoria.listarConFiltros(normalizado);
    }

    @Transactional(readOnly = true)
    public Pagina<AuditoriaRegistrada> listarPaginado(FiltroAuditoriaPaginada filtro) {
        return repositorioAuditoria.listarPaginado(new FiltroAuditoriaPaginada(
            limpiar(filtro.accion()),
            limpiar(filtro.tipoEntidad()),
            limpiar(filtro.usuarioActor()),
            filtro.desde(),
            filtro.hasta(),
            Math.max(0, filtro.pagina()),
            Math.max(1, Math.min(filtro.tamano(), 200)),
            limpiar(filtro.orden()) == null ? "creadoEn,desc" : filtro.orden()
        ));
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
