package com.farmamia.posupdate.infraestructura.persistencia.adaptador;

import com.farmamia.posupdate.aplicacion.excepcion.RecursoNoEncontradoException;
import com.farmamia.posupdate.dominio.modelo.MetricaEquipo;
import com.farmamia.posupdate.dominio.modelo.MetricaEquipoRegistrada;
import com.farmamia.posupdate.dominio.puerto.RepositorioMetricasEquipo;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.EquipoEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.MetricaEquipoEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.EquipoRepositorioJpa;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.MetricaEquipoRepositorioJpa;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import org.springframework.data.domain.PageRequest;
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
            metrica.estadoAgente(),
            metrica.usoCpuPorcentaje(),
            metrica.usoRamPorcentaje(),
            metrica.tiempoRespuestaMs(),
            metrica.traficoInboundKbps(),
            metrica.traficoOutboundKbps(),
            metrica.uptimeRouterTicks(),
            metrica.descripcionRouter()
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
            entidad.getUsoCpuPorcentaje(),
            entidad.getUsoRamPorcentaje(),
            entidad.getTiempoRespuestaMs(),
            entidad.getTraficoInboundKbps(),
            entidad.getTraficoOutboundKbps(),
            entidad.getUptimeRouterTicks(),
            entidad.getDescripcionRouter(),
            entidad.getRecolectadoEn()
        );
    }

    @Override
    public List<MetricaEquipoRegistrada> listarUltimasPorEquipo(UUID idEquipo, int limite) {
        return metricaEquipoRepositorioJpa.findByEquipo_IdOrderByRecolectadoEnDesc(idEquipo, PageRequest.of(0, limite))
            .stream()
            .map(this::aDominio)
            .toList();
    }
}
