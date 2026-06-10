package com.farmamia.operations.infraestructura.persistencia.adaptador;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.farmamia.operations.aplicacion.excepcion.RecursoNoEncontradoException;
import com.farmamia.operations.dominio.modelo.EventoActualizacion;
import com.farmamia.operations.dominio.modelo.EventoActualizacionRegistrado;
import com.farmamia.operations.dominio.puerto.RepositorioEventosActualizacion;
import com.farmamia.operations.infraestructura.persistencia.entidad.DespliegueEntidad;
import com.farmamia.operations.infraestructura.persistencia.entidad.EquipoEntidad;
import com.farmamia.operations.infraestructura.persistencia.entidad.EventoActualizacionEntidad;
import com.farmamia.operations.infraestructura.persistencia.entidad.ObjetivoDespliegueEntidad;
import com.farmamia.operations.infraestructura.persistencia.repositorio.EquipoRepositorioJpa;
import com.farmamia.operations.infraestructura.persistencia.repositorio.EventoActualizacionRepositorioJpa;
import com.farmamia.operations.infraestructura.persistencia.repositorio.ObjetivoDespliegueRepositorioJpa;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioEventosActualizacionJpaAdaptador implements RepositorioEventosActualizacion {

    private final EquipoRepositorioJpa equipoRepositorioJpa;
    private final ObjetivoDespliegueRepositorioJpa objetivoDespliegueRepositorioJpa;
    private final EventoActualizacionRepositorioJpa eventoActualizacionRepositorioJpa;
    private final ObjectMapper objectMapper;

    public RepositorioEventosActualizacionJpaAdaptador(
        EquipoRepositorioJpa equipoRepositorioJpa,
        ObjetivoDespliegueRepositorioJpa objetivoDespliegueRepositorioJpa,
        EventoActualizacionRepositorioJpa eventoActualizacionRepositorioJpa,
        ObjectMapper objectMapper
    ) {
        this.equipoRepositorioJpa = equipoRepositorioJpa;
        this.objetivoDespliegueRepositorioJpa = objetivoDespliegueRepositorioJpa;
        this.eventoActualizacionRepositorioJpa = eventoActualizacionRepositorioJpa;
        this.objectMapper = objectMapper;
    }

    @Override
    public void guardar(EventoActualizacion evento) {
        EquipoEntidad equipo = equipoRepositorioJpa.findById(evento.idEquipo())
            .orElseThrow(() -> new RecursoNoEncontradoException("Equipo no encontrado: " + evento.idEquipo()));
        ObjetivoDespliegueEntidad objetivo = buscarObjetivo(evento);

        eventoActualizacionRepositorioJpa.save(new EventoActualizacionEntidad(
            objetivo,
            equipo,
            evento.tipoEvento(),
            evento.mensajeEvento(),
            evento.versionAnterior(),
            evento.versionNueva(),
            aJson(evento.metadatos())
        ));
    }

    @Override
    public List<EventoActualizacionRegistrado> listarRecientes(int limite) {
        return eventoActualizacionRepositorioJpa.findByOrderByCreadoEnDesc(PageRequest.of(0, limite))
            .stream()
            .map(this::aDominio)
            .toList();
    }

    @Override
    public List<EventoActualizacionRegistrado> listarRecientesPorEquipo(UUID idEquipo, int limite) {
        return eventoActualizacionRepositorioJpa.findByEquipo_IdOrderByCreadoEnDesc(idEquipo, PageRequest.of(0, limite))
            .stream()
            .map(this::aDominio)
            .toList();
    }

    private ObjetivoDespliegueEntidad buscarObjetivo(EventoActualizacion evento) {
        if (evento.idObjetivoDespliegue() == null) {
            return null;
        }

        return objetivoDespliegueRepositorioJpa.findByIdAndEquipo_Id(evento.idObjetivoDespliegue(), evento.idEquipo())
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "Objetivo de despliegue no encontrado para el equipo: " + evento.idObjetivoDespliegue()
            ));
    }

    private String aJson(Map<String, Object> metadatos) {
        if (metadatos == null || metadatos.isEmpty()) {
            return "{}";
        }

        try {
            return objectMapper.writeValueAsString(metadatos);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Metadatos de evento invalidos", ex);
        }
    }

    private EventoActualizacionRegistrado aDominio(EventoActualizacionEntidad entidad) {
        EquipoEntidad equipo = entidad.getEquipo();
        DespliegueEntidad despliegue = entidad.getDespliegue();
        ObjetivoDespliegueEntidad objetivo = entidad.getObjetivoDespliegue();

        return new EventoActualizacionRegistrado(
            entidad.getId(),
            equipo.getId(),
            equipo.getNombreEquipo(),
            idDespliegue(despliegue),
            objetivo == null ? null : objetivo.getId(),
            entidad.getTipoEvento(),
            entidad.getMensajeEvento(),
            entidad.getVersionAnterior(),
            entidad.getVersionNueva(),
            aMapa(entidad.getMetadatosJson()),
            entidad.getCreadoEn()
        );
    }

    private UUID idDespliegue(DespliegueEntidad despliegue) {
        return despliegue == null ? null : despliegue.getId();
    }

    private Map<String, Object> aMapa(String metadatosJson) {
        if (metadatosJson == null || metadatosJson.isBlank()) {
            return Map.of();
        }

        try {
            return objectMapper.readValue(metadatosJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException ex) {
            return Map.of("raw", metadatosJson);
        }
    }
}
