package com.farmamia.operations.infraestructura.persistencia.adaptador;

import com.farmamia.operations.aplicacion.excepcion.RecursoNoEncontradoException;
import com.farmamia.operations.dominio.modelo.ResultadoActualizacion;
import com.farmamia.operations.dominio.puerto.RepositorioObjetivosDespliegue;
import com.farmamia.operations.infraestructura.persistencia.entidad.ObjetivoDespliegueEntidad;
import com.farmamia.operations.infraestructura.persistencia.repositorio.ObjetivoDespliegueRepositorioJpa;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioObjetivosDespliegueJpaAdaptador implements RepositorioObjetivosDespliegue {

    private final ObjetivoDespliegueRepositorioJpa objetivoDespliegueRepositorioJpa;

    public RepositorioObjetivosDespliegueJpaAdaptador(
        ObjetivoDespliegueRepositorioJpa objetivoDespliegueRepositorioJpa
    ) {
        this.objetivoDespliegueRepositorioJpa = objetivoDespliegueRepositorioJpa;
    }

    @Override
    public void validarPerteneceAEquipo(UUID idObjetivoDespliegue, UUID idEquipo) {
        buscarObjetivo(idObjetivoDespliegue, idEquipo);
    }

    @Override
    public void registrarResultado(UUID idEquipo, ResultadoActualizacion resultado) {
        ObjetivoDespliegueEntidad objetivo = buscarObjetivo(resultado.idObjetivoDespliegue(), idEquipo);
        objetivo.registrarResultado(
            resultado.estado(),
            resultado.versionAnterior(),
            resultado.versionNueva(),
            resultado.mensaje()
        );
    }

    private ObjetivoDespliegueEntidad buscarObjetivo(UUID idObjetivoDespliegue, UUID idEquipo) {
        return objetivoDespliegueRepositorioJpa.findByIdAndEquipo_Id(idObjetivoDespliegue, idEquipo)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "Objetivo de despliegue no encontrado para el equipo: " + idObjetivoDespliegue
            ));
    }
}
