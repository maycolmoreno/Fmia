package com.farmamia.operations.aplicacion.casouso;

import com.farmamia.operations.dominio.modelo.AlertaRegistrada;
import com.farmamia.operations.dominio.modelo.FiltroAlertas;
import com.farmamia.operations.dominio.modelo.Pagina;
import com.farmamia.operations.dominio.puerto.RepositorioAlertas;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultarAlertasCasoUso {

    private final RepositorioAlertas repositorioAlertas;

    public ConsultarAlertasCasoUso(RepositorioAlertas repositorioAlertas) {
        this.repositorioAlertas = repositorioAlertas;
    }

    @Transactional(readOnly = true)
    public List<AlertaRegistrada> listarRecientes(int limite) {
        int limiteNormalizado = Math.max(1, Math.min(limite, 200));
        return repositorioAlertas.listarRecientes(limiteNormalizado);
    }

    @Transactional(readOnly = true)
    public List<AlertaRegistrada> listarConFiltros(FiltroAlertas filtro) {
        FiltroAlertas normalizado = normalizar(filtro);
        return repositorioAlertas.listarConFiltros(normalizado);
    }

    @Transactional(readOnly = true)
    public Pagina<AlertaRegistrada> listarPaginado(FiltroAlertas filtro) {
        return repositorioAlertas.listarPaginado(normalizar(filtro));
    }

    @Transactional
    public AlertaRegistrada reconocer(UUID idAlerta, String usuarioActor) {
        return repositorioAlertas.reconocer(idAlerta, usuarioActor);
    }

    @Transactional
    public AlertaRegistrada cerrar(UUID idAlerta, String usuarioActor) {
        return repositorioAlertas.cerrar(idAlerta, usuarioActor);
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private FiltroAlertas normalizar(FiltroAlertas filtro) {
        return new FiltroAlertas(
            limpiar(filtro.estado()),
            limpiar(filtro.severidad()),
            limpiar(filtro.tipo()),
            filtro.idEquipo(),
            filtro.idSucursal(),
            limpiar(filtro.codigoSucursal()),
            limpiar(filtro.nombreEquipo()),
            filtro.fechaDesde(),
            filtro.fechaHasta(),
            Math.max(0, filtro.pagina()),
            Math.max(1, Math.min(filtro.tamano(), 200)),
            limpiar(filtro.orden()),
            filtro.eventoDeRed()
        );
    }
}
