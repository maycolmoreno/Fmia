package com.farmamia.operations.infraestructura.persistencia.adaptador;

import com.farmamia.operations.dominio.modelo.InstruccionAgente;
import com.farmamia.operations.dominio.puerto.RepositorioInstruccionesAgente;
import com.farmamia.operations.infraestructura.persistencia.entidad.DespliegueEntidad;
import com.farmamia.operations.infraestructura.persistencia.entidad.ObjetivoDespliegueEntidad;
import com.farmamia.operations.infraestructura.persistencia.entidad.PaquetePosEntidad;
import com.farmamia.operations.infraestructura.persistencia.repositorio.ObjetivoDespliegueRepositorioJpa;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioInstruccionesAgenteJpaAdaptador implements RepositorioInstruccionesAgente {

    private static final String ESTADO_AUTORIZADO = "AUTHORIZED";
    private static final String TIPO_INSTRUCCION_ACTUALIZAR_POS = "UPDATE_POS";

    private final ObjetivoDespliegueRepositorioJpa objetivoDespliegueRepositorioJpa;

    public RepositorioInstruccionesAgenteJpaAdaptador(ObjetivoDespliegueRepositorioJpa objetivoDespliegueRepositorioJpa) {
        this.objetivoDespliegueRepositorioJpa = objetivoDespliegueRepositorioJpa;
    }

    @Override
    public Optional<InstruccionAgente> buscarSiguienteParaEquipo(UUID idEquipo) {
        return objetivoDespliegueRepositorioJpa
            .findFirstByEquipo_IdAndEstadoOrderByActualizadoEnDesc(idEquipo, ESTADO_AUTORIZADO)
            .filter(this::esEntregable)
            .map(this::aDominio);
    }

    private boolean esEntregable(ObjetivoDespliegueEntidad objetivo) {
        DespliegueEntidad despliegue = objetivo.getDespliegue();
        PaquetePosEntidad paquete = despliegue.getPaquete();

        return objetivo.estaAutorizado()
            && despliegue.puedeEntregarInstrucciones()
            && paquete.estaAprobado();
    }

    private InstruccionAgente aDominio(ObjetivoDespliegueEntidad objetivo) {
        DespliegueEntidad despliegue = objetivo.getDespliegue();
        PaquetePosEntidad paquete = despliegue.getPaquete();

        return new InstruccionAgente(
            true,
            TIPO_INSTRUCCION_ACTUALIZAR_POS,
            objetivo.getId(),
            paquete.getId(),
            paquete.getVersion(),
            "/api/packages/" + paquete.getId() + "/download",
            paquete.getChecksumSha256(),
            despliegue.getHoraOficialActualizacion(),
            despliegue.getHoraForzadaActualizacion(),
            List.of(LocalTime.of(0, 50), LocalTime.of(0, 55))
        );
    }
}
