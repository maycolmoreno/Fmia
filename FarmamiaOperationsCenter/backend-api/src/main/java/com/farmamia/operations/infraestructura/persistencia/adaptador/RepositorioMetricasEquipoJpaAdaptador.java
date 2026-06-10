package com.farmamia.operations.infraestructura.persistencia.adaptador;

import com.farmamia.operations.aplicacion.excepcion.RecursoNoEncontradoException;
import com.farmamia.operations.dominio.modelo.MetricaEquipo;
import com.farmamia.operations.dominio.modelo.MetricaEquipoRegistrada;
import com.farmamia.operations.dominio.puerto.RepositorioMetricasEquipo;
import com.farmamia.operations.infraestructura.persistencia.entidad.EquipoEntidad;
import com.farmamia.operations.infraestructura.persistencia.entidad.MetricaEquipoEntidad;
import com.farmamia.operations.infraestructura.persistencia.repositorio.EquipoRepositorioJpa;
import com.farmamia.operations.infraestructura.persistencia.repositorio.MetricaEquipoRepositorioJpa;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioMetricasEquipoJpaAdaptador implements RepositorioMetricasEquipo {

    private final MetricaEquipoRepositorioJpa metricaEquipoRepositorioJpa;
    private final EquipoRepositorioJpa equipoRepositorioJpa;

    public RepositorioMetricasEquipoJpaAdaptador(
        MetricaEquipoRepositorioJpa metricaEquipoRepositorioJpa,
        EquipoRepositorioJpa equipoRepositorioJpa
    ) {
        this.metricaEquipoRepositorioJpa = metricaEquipoRepositorioJpa;
        this.equipoRepositorioJpa = equipoRepositorioJpa;
    }

    @Override
    public void guardar(MetricaEquipo metrica) {
        EquipoEntidad equipo = equipoRepositorioJpa.findById(metrica.idEquipo())
            .orElseThrow(() -> new RecursoNoEncontradoException("Equipo no encontrado: " + metrica.idEquipo()));

        metricaEquipoRepositorioJpa.save(new MetricaEquipoEntidad(
            equipo,
            metrica.versionPos(),
            metrica.discoLibreMb(),
            metrica.discoTotalMb(),
            metrica.procesoPosEjecutandose(),
            metrica.latenciaMs(),
            metrica.porcentajePerdidaPaquetes(),
            metrica.estadoAgente()
        ));
    }

    @Override
    public Optional<MetricaEquipoRegistrada> buscarUltimaPorEquipo(UUID idEquipo) {
        return metricaEquipoRepositorioJpa.findFirstByEquipo_IdOrderByRecolectadoEnDesc(idEquipo)
            .map(this::aDominio);
    }

    private MetricaEquipoRegistrada aDominio(MetricaEquipoEntidad entidad) {
        return new MetricaEquipoRegistrada(
            entidad.getId(),
            entidad.getVersionPos(),
            entidad.getDiscoLibreMb(),
            entidad.getDiscoTotalMb(),
            entidad.getProcesoPosEjecutandose(),
            entidad.getLatenciaMs(),
            entidad.getPorcentajePerdidaPaquetes(),
            entidad.getEstadoAgente(),
            entidad.getRecolectadoEn()
        );
    }
}
