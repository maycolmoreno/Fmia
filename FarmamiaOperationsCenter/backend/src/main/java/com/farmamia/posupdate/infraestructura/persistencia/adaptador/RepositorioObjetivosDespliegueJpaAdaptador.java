package com.farmamia.posupdate.infraestructura.persistencia.adaptador;

import com.farmamia.posupdate.aplicacion.excepcion.RecursoNoEncontradoException;
import com.farmamia.posupdate.dominio.modelo.ResultadoActualizacion;
import com.farmamia.posupdate.dominio.puerto.RepositorioObjetivosDespliegue;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.ObjetivoDespliegueEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.ObjetivoDespliegueRepositorioJpa;
import java.math.BigDecimal;
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

    @Override
    public void actualizarProgresoDescarga(UUID idObjetivoDespliegue, UUID idEquipo, BigDecimal porcentaje) {
        ObjetivoDespliegueEntidad objetivo = buscarObjetivo(idObjetivoDespliegue, idEquipo);
        objetivo.registrarProgresoDescarga(porcentaje);
    }

    private ObjetivoDespliegueEntidad buscarObjetivo(UUID idObjetivoDespliegue, UUID idEquipo) {
        return objetivoDespliegueRepositorioJpa.findByIdAndEquipo_Id(idObjetivoDespliegue, idEquipo)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "Objetivo de despliegue no encontrado para el equipo: " + idObjetivoDespliegue
            ));
    }
}
