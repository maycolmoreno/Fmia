package com.farmamia.posupdate.aplicacion.casouso;

import com.farmamia.posupdate.dominio.modelo.DatosCrearDespliegue;
import com.farmamia.posupdate.dominio.modelo.Despliegue;
import com.farmamia.posupdate.dominio.modelo.EstadoDespliegue;
import com.farmamia.posupdate.dominio.modelo.FiltroDespliegues;
import com.farmamia.posupdate.dominio.modelo.Pagina;
import com.farmamia.posupdate.dominio.puerto.RepositorioDespliegues;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GestionarDesplieguesCasoUso {

    private final RepositorioDespliegues repositorioDespliegues;

    public GestionarDesplieguesCasoUso(RepositorioDespliegues repositorioDespliegues) {
        this.repositorioDespliegues = repositorioDespliegues;
    }

    @Transactional
    public Despliegue crear(DatosCrearDespliegue datos) {
        return repositorioDespliegues.crear(datos);
    }

    public List<Despliegue> listar() {
        return repositorioDespliegues.listar();
    }

    public Pagina<Despliegue> listarPaginado(FiltroDespliegues filtro) {
        return repositorioDespliegues.listarPaginado(new FiltroDespliegues(
            blancoANulo(filtro.q()),
            blancoANulo(filtro.estado()),
            blancoANulo(filtro.versionPaquete()),
            filtro.creadoDesde(),
            filtro.creadoHasta(),
            Math.max(0, filtro.pagina()),
            Math.max(1, Math.min(filtro.tamano(), 200)),
            blancoANulo(filtro.orden()) == null ? "creadoEn,desc" : filtro.orden()
        ));
    }

    public Despliegue obtener(UUID id) {
        return repositorioDespliegues.obtener(id);
    }

    @Transactional
    public Despliegue programar(UUID id, OffsetDateTime programadoEn) {
        return repositorioDespliegues.programar(id, programadoEn);
    }

    @Transactional
    public Despliegue pausar(UUID id) {
        return repositorioDespliegues.pausar(id);
    }

    @Transactional
    public Despliegue reanudar(UUID id) {
        return repositorioDespliegues.reanudar(id);
    }

    @Transactional
    public Despliegue cancelar(UUID id) {
        return repositorioDespliegues.cancelar(id);
    }

    public EstadoDespliegue estado(UUID id) {
        return repositorioDespliegues.estado(id);
    }

    public int contarFarmaciasTurno(UUID id) {
        return repositorioDespliegues.contarFarmaciasTurno(id);
    }

    private String blancoANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
