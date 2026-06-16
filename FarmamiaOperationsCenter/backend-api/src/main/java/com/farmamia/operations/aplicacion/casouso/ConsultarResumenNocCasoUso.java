package com.farmamia.operations.aplicacion.casouso;

import com.farmamia.operations.dominio.modelo.EstadoOperacionalFarmacia;
import com.farmamia.operations.dominio.modelo.ResumenNocDashboard;
import com.farmamia.operations.dominio.modelo.ResumenNocDashboard.FarmaciaCriticaNoc;
import com.farmamia.operations.dominio.puerto.RepositorioEstadoFarmacias;
import com.farmamia.operations.dominio.puerto.RepositorioResumenNoc;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultarResumenNocCasoUso {

    private static final int MAX_CRITICAS = 10;
    private static final int MAX_EN_RIESGO = 5;
    private static final int ALERTAS_RECIENTES = 10;

    private final RepositorioEstadoFarmacias repositorioEstadoFarmacias;
    private final RepositorioResumenNoc repositorioResumenNoc;

    public ConsultarResumenNocCasoUso(
        RepositorioEstadoFarmacias repositorioEstadoFarmacias,
        RepositorioResumenNoc repositorioResumenNoc
    ) {
        this.repositorioEstadoFarmacias = repositorioEstadoFarmacias;
        this.repositorioResumenNoc = repositorioResumenNoc;
    }

    @Transactional(readOnly = true)
    public ResumenNocDashboard obtener() {
        List<EstadoOperacionalFarmacia> estados = repositorioEstadoFarmacias.listar();

        List<FarmaciaCriticaNoc> criticas = estados.stream()
            .filter(EstadoOperacionalFarmacia::critica)
            .limit(MAX_CRITICAS)
            .map(this::aFarmaciaNoc)
            .toList();

        List<FarmaciaCriticaNoc> turnoEnRiesgo = estados.stream()
            .filter(e -> e.turnoEnRiesgo() && !e.critica())
            .limit(MAX_EN_RIESGO)
            .map(this::aFarmaciaNoc)
            .toList();

        return new ResumenNocDashboard(
            criticas,
            turnoEnRiesgo,
            repositorioResumenNoc.obtenerEstadoRed(),
            repositorioResumenNoc.obtenerEstadoPos(),
            repositorioResumenNoc.obtenerCampanaActiva(),
            repositorioResumenNoc.obtenerAlertasRecientes(ALERTAS_RECIENTES),
            OffsetDateTime.now()
        );
    }

    private FarmaciaCriticaNoc aFarmaciaNoc(EstadoOperacionalFarmacia e) {
        return new FarmaciaCriticaNoc(
            e.idFarmacia(),
            e.codigoFarmacia(),
            e.nombreFarmacia(),
            e.deTurno(),
            e.estadoOperacional(),
            e.critica(),
            e.turnoEnRiesgo(),
            e.alertasCriticas(),
            e.resumenRiesgo()
        );
    }
}
