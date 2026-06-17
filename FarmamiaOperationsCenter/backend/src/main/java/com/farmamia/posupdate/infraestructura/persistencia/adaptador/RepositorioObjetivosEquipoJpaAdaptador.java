package com.farmamia.posupdate.infraestructura.persistencia.adaptador;

import com.farmamia.posupdate.dominio.modelo.ObjetivoDespliegueEquipo;
import com.farmamia.posupdate.dominio.puerto.RepositorioObjetivosEquipo;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.DespliegueEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.ObjetivoDespliegueEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.ObjetivoDespliegueRepositorioJpa;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioObjetivosEquipoJpaAdaptador implements RepositorioObjetivosEquipo {

    private final ObjetivoDespliegueRepositorioJpa objetivoDespliegueRepositorioJpa;

    public RepositorioObjetivosEquipoJpaAdaptador(ObjetivoDespliegueRepositorioJpa objetivoDespliegueRepositorioJpa) {
        this.objetivoDespliegueRepositorioJpa = objetivoDespliegueRepositorioJpa;
    }

    @Override
    public List<ObjetivoDespliegueEquipo> listarPorEquipo(UUID idEquipo) {
        return objetivoDespliegueRepositorioJpa.findByEquipo_IdOrderByActualizadoEnDesc(idEquipo)
            .stream()
            .map(this::aDominio)
            .toList();
    }

    private ObjetivoDespliegueEquipo aDominio(ObjetivoDespliegueEntidad entidad) {
        DespliegueEntidad despliegue = entidad.getDespliegue();
        return new ObjetivoDespliegueEquipo(
            entidad.getId(),
            despliegue.getId(),
            despliegue.getNombre(),
            despliegue.getPaquete().getVersion(),
            despliegue.getEstado(),
            entidad.getEstado(),
            entidad.getGrupoObjetivo(),
            entidad.isPiloto(),
            entidad.getVersionAnterior(),
            entidad.getVersionNueva(),
            entidad.getUltimoError(),
            entidad.getAutorizadoEn(),
            entidad.getIniciadoEn(),
            entidad.getCompletadoEn(),
            entidad.getActualizadoEn()
        );
    }
}
