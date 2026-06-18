package com.farmamia.posupdate.aplicacion.casouso;

import com.farmamia.posupdate.dominio.modelo.Equipo;
import com.farmamia.posupdate.dominio.modelo.EventoActualizacionRegistrado;
import com.farmamia.posupdate.dominio.modelo.FiltroEquipos;
import com.farmamia.posupdate.dominio.modelo.FiltroEventosActualizacion;
import com.farmamia.posupdate.dominio.modelo.FiltroSucursales;
import com.farmamia.posupdate.dominio.modelo.Pagina;
import com.farmamia.posupdate.dominio.modelo.Sucursal;
import com.farmamia.posupdate.dominio.puerto.RepositorioEquipos;
import com.farmamia.posupdate.dominio.puerto.RepositorioEventosActualizacion;
import com.farmamia.posupdate.dominio.puerto.RepositorioSucursales;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultarCatalogoOperativoCasoUso {

    private final RepositorioEquipos repositorioEquipos;
    private final RepositorioSucursales repositorioSucursales;
    private final RepositorioEventosActualizacion repositorioEventosActualizacion;

    public ConsultarCatalogoOperativoCasoUso(
        RepositorioEquipos repositorioEquipos,
        RepositorioSucursales repositorioSucursales,
        RepositorioEventosActualizacion repositorioEventosActualizacion
    ) {
        this.repositorioEquipos = repositorioEquipos;
        this.repositorioSucursales = repositorioSucursales;
        this.repositorioEventosActualizacion = repositorioEventosActualizacion;
    }

    @Transactional(readOnly = true)
    public List<Equipo> listarEquipos() {
        return repositorioEquipos.listar();
    }

    @Transactional(readOnly = true)
    public List<Equipo> listarEquiposSinSucursal() {
        return repositorioEquipos.listarHuerfanos();
    }

    @Transactional(readOnly = true)
    public Pagina<Equipo> listarEquiposPaginado(FiltroEquipos filtro) {
        return repositorioEquipos.listarPaginado(normalizar(filtro));
    }

    @Transactional(readOnly = true)
    public List<Sucursal> listarSucursales() {
        return repositorioSucursales.listar();
    }

    @Transactional(readOnly = true)
    public Pagina<Sucursal> listarSucursalesPaginado(FiltroSucursales filtro) {
        return repositorioSucursales.listarPaginado(new FiltroSucursales(
            blancoANulo(filtro.q()),
            blancoANulo(filtro.codigo()),
            blancoANulo(filtro.ciudad()),
            blancoANulo(filtro.zona()),
            filtro.deTurno(),
            filtro.activa(),
            Math.max(0, filtro.pagina()),
            Math.max(1, Math.min(filtro.tamano(), 200)),
            blancoANulo(filtro.orden()) == null ? "codigo,asc" : filtro.orden()
        ));
    }

    @Transactional(readOnly = true)
    public List<EventoActualizacionRegistrado> listarEventosRecientes(int limite) {
        int limiteNormalizado = Math.max(1, Math.min(limite, 200));
        return repositorioEventosActualizacion.listarRecientes(limiteNormalizado);
    }

    @Transactional(readOnly = true)
    public Pagina<EventoActualizacionRegistrado> listarEventosPaginado(FiltroEventosActualizacion filtro) {
        return repositorioEventosActualizacion.listarPaginado(new FiltroEventosActualizacion(
            filtro.idEquipo(),
            filtro.idDespliegue(),
            blancoANulo(filtro.tipoEvento()),
            filtro.desde(),
            filtro.hasta(),
            Math.max(0, filtro.pagina()),
            Math.max(1, Math.min(filtro.tamano(), 200)),
            blancoANulo(filtro.orden()) == null ? "creadoEn,desc" : filtro.orden()
        ));
    }

    private FiltroEquipos normalizar(FiltroEquipos filtro) {
        return new FiltroEquipos(
            blancoANulo(filtro.q()),
            blancoANulo(filtro.estado()),
            blancoANulo(filtro.codigoSucursal()),
            blancoANulo(filtro.versionPos()),
            blancoANulo(filtro.versionAgente()),
            filtro.ultimoLatidoDesde(),
            filtro.ultimoLatidoHasta(),
            Math.max(0, filtro.pagina()),
            Math.max(1, Math.min(filtro.tamano(), 200)),
            blancoANulo(filtro.orden()) == null ? "nombreEquipo,asc" : filtro.orden()
        );
    }

    private String blancoANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
