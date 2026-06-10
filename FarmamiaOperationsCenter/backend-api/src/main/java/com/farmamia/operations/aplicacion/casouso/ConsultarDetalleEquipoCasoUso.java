package com.farmamia.operations.aplicacion.casouso;

import com.farmamia.operations.aplicacion.excepcion.RecursoNoEncontradoException;
import com.farmamia.operations.dominio.modelo.DetalleEquipo;
import com.farmamia.operations.dominio.modelo.Equipo;
import com.farmamia.operations.dominio.puerto.RepositorioEquipos;
import com.farmamia.operations.dominio.puerto.RepositorioEventosActualizacion;
import com.farmamia.operations.dominio.puerto.RepositorioMetricasEquipo;
import com.farmamia.operations.dominio.puerto.RepositorioObjetivosEquipo;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultarDetalleEquipoCasoUso {

    private final RepositorioEquipos repositorioEquipos;
    private final RepositorioMetricasEquipo repositorioMetricasEquipo;
    private final RepositorioEventosActualizacion repositorioEventosActualizacion;
    private final RepositorioObjetivosEquipo repositorioObjetivosEquipo;

    public ConsultarDetalleEquipoCasoUso(
        RepositorioEquipos repositorioEquipos,
        RepositorioMetricasEquipo repositorioMetricasEquipo,
        RepositorioEventosActualizacion repositorioEventosActualizacion,
        RepositorioObjetivosEquipo repositorioObjetivosEquipo
    ) {
        this.repositorioEquipos = repositorioEquipos;
        this.repositorioMetricasEquipo = repositorioMetricasEquipo;
        this.repositorioEventosActualizacion = repositorioEventosActualizacion;
        this.repositorioObjetivosEquipo = repositorioObjetivosEquipo;
    }

    @Transactional(readOnly = true)
    public DetalleEquipo consultar(UUID idEquipo) {
        Equipo equipo = repositorioEquipos.buscarPorId(idEquipo)
            .orElseThrow(() -> new RecursoNoEncontradoException("Equipo no encontrado: " + idEquipo));

        return new DetalleEquipo(
            equipo,
            repositorioMetricasEquipo.buscarUltimaPorEquipo(idEquipo).orElse(null),
            repositorioEventosActualizacion.listarRecientesPorEquipo(idEquipo, 20),
            repositorioObjetivosEquipo.listarPorEquipo(idEquipo)
        );
    }
}
