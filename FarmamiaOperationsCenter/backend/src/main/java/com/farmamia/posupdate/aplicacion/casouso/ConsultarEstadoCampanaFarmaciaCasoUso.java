package com.farmamia.posupdate.aplicacion.casouso;

import com.farmamia.posupdate.dominio.modelo.FiltroEstadoCampanaFarmacia;
import com.farmamia.posupdate.dominio.modelo.ResumenEstadoCampanaFarmacia;
import com.farmamia.posupdate.dominio.puerto.RepositorioEstadoCampanaFarmacia;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultarEstadoCampanaFarmaciaCasoUso {

    private final RepositorioEstadoCampanaFarmacia repositorioEstadoCampanaFarmacia;

    public ConsultarEstadoCampanaFarmaciaCasoUso(RepositorioEstadoCampanaFarmacia repositorioEstadoCampanaFarmacia) {
        this.repositorioEstadoCampanaFarmacia = repositorioEstadoCampanaFarmacia;
    }

    @Transactional(readOnly = true)
    public ResumenEstadoCampanaFarmacia consultar(UUID idCampana, FiltroEstadoCampanaFarmacia filtro) {
        return repositorioEstadoCampanaFarmacia.consultar(idCampana, new FiltroEstadoCampanaFarmacia(
            limpiar(filtro.estadoTecnico()),
            limpiar(filtro.estadoOperacional()),
            limpiar(filtro.grupoTrx()),
            filtro.deTurno(),
            limpiar(filtro.q()),
            Math.max(0, filtro.pagina()),
            Math.max(1, Math.min(filtro.tamano(), 200)),
            limpiar(filtro.orden()) == null ? "prioridad,asc" : filtro.orden()
        ));
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
