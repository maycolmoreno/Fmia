package com.farmamia.operations.aplicacion.casouso;

import com.farmamia.operations.aplicacion.excepcion.RecursoNoEncontradoException;
import com.farmamia.operations.dominio.modelo.DatosLatido;
import com.farmamia.operations.dominio.modelo.MetricaEquipo;
import com.farmamia.operations.dominio.puerto.RepositorioEquipos;
import com.farmamia.operations.dominio.puerto.RepositorioMetricasEquipo;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class RegistrarLatidoCasoUso {

    private final RepositorioEquipos repositorioEquipos;
    private final RepositorioMetricasEquipo repositorioMetricasEquipo;

    public RegistrarLatidoCasoUso(
        RepositorioEquipos repositorioEquipos,
        RepositorioMetricasEquipo repositorioMetricasEquipo
    ) {
        this.repositorioEquipos = repositorioEquipos;
        this.repositorioMetricasEquipo = repositorioMetricasEquipo;
    }

    @Transactional
    public void registrar(DatosLatido datosLatido) {
        repositorioEquipos.buscarPorId(datosLatido.idEquipo())
            .orElseThrow(() -> new RecursoNoEncontradoException("Equipo no encontrado: " + datosLatido.idEquipo()));

        repositorioEquipos.registrarLatido(datosLatido.idEquipo(), datosLatido.versionPos());
        repositorioMetricasEquipo.guardar(new MetricaEquipo(
            datosLatido.idEquipo(),
            datosLatido.versionPos(),
            datosLatido.discoLibreMb(),
            datosLatido.discoTotalMb(),
            datosLatido.procesoPosEjecutandose(),
            datosLatido.latenciaMs(),
            datosLatido.porcentajePerdidaPaquetes(),
            "ONLINE"
        ));
    }
}
