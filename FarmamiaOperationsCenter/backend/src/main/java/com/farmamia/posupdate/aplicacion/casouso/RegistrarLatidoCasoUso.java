package com.farmamia.posupdate.aplicacion.casouso;

import com.farmamia.posupdate.aplicacion.excepcion.RecursoNoEncontradoException;
import com.farmamia.posupdate.dominio.modelo.DatosLatido;
import com.farmamia.posupdate.dominio.modelo.MetricaEquipo;
import com.farmamia.posupdate.dominio.puerto.RepositorioEquipos;
import com.farmamia.posupdate.dominio.puerto.RepositorioMetricasEquipo;
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
            "ONLINE",
            null,
            null,
            datosLatido.latenciaMs(),
            null,
            null,
            null,
            null
        ));
    }
}
