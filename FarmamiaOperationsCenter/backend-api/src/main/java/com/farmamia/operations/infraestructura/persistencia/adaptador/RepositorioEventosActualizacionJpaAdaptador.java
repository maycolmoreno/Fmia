package com.farmamia.operations.infraestructura.persistencia.adaptador;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.farmamia.operations.aplicacion.excepcion.RecursoNoEncontradoException;
import com.farmamia.operations.dominio.modelo.EventoActualizacion;
import com.farmamia.operations.dominio.modelo.EventoActualizacionRegistrado;
import com.farmamia.operations.dominio.modelo.FiltroEventosActualizacion;
import com.farmamia.operations.dominio.modelo.Pagina;
import com.farmamia.operations.dominio.puerto.RepositorioEventosActualizacion;
import com.farmamia.operations.infraestructura.persistencia.entidad.DespliegueEntidad;
import com.farmamia.operations.infraestructura.persistencia.entidad.EquipoEntidad;
import com.farmamia.operations.infraestructura.persistencia.entidad.EventoActualizacionEntidad;
import com.farmamia.operations.infraestructura.persistencia.entidad.ObjetivoDespliegueEntidad;
import com.farmamia.operations.infraestructura.persistencia.repositorio.EquipoRepositorioJpa;
import com.farmamia.operations.infraestructura.persistencia.repositorio.EventoActualizacionRepositorioJpa;
import com.farmamia.operations.infraestructura.persistencia.repositorio.ObjetivoDespliegueRepositorioJpa;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioEventosActualizacionJpaAdaptador implements RepositorioEventosActualizacion {

    private static final OffsetDateTime FECHA_NEUTRA = OffsetDateTime.parse("1970-01-01T00:00:00Z");
    private static final UUID UUID_NEUTRO = new UUID(0L, 0L);

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
            evento.idempotencyKey(),
            evento.mensajeEvento(),
            evento.versionAnterior(),
            evento.versionNueva(),
            aJson(evento.metadatos())
        ));
    }

    @Override
    public boolean existeConIdempotencia(UUID idEquipo, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return false;
        }

        return eventoActualizacionRepositorioJpa.existsByEquipo_IdAndIdempotencyKey(idEquipo, idempotencyKey);
    }

    @Override
    public boolean coincideConIdempotencia(EventoActualizacion evento) {
        if (evento.idempotencyKey() == null || evento.idempotencyKey().isBlank()) {
            return false;
        }

        return eventoActualizacionRepositorioJpa
            .findByEquipo_IdAndIdempotencyKey(evento.idEquipo(), evento.idempotencyKey())
            .map(existente -> coincide(existente, evento))
            .orElse(false);
    }

    @Override
    public List<EventoActualizacionRegistrado> listarRecientes(int limite) {
        return eventoActualizacionRepositorioJpa.findByOrderByCreadoEnDesc(PageRequest.of(0, limite))
            .stream()
            .map(this::aDominio)
            .toList();
    }

    @Override
    public Pagina<EventoActualizacionRegistrado> listarPaginado(FiltroEventosActualizacion filtro) {
        String tipoEvento = minusculaANulo(filtro.tipoEvento());
        org.springframework.data.domain.Page<EventoActualizacionEntidad> pagina = eventoActualizacionRepositorioJpa.buscarConFiltros(
            filtro.idEquipo() != null,
            filtro.idEquipo() == null ? UUID_NEUTRO : filtro.idEquipo(),
            filtro.idDespliegue() != null,
            filtro.idDespliegue() == null ? UUID_NEUTRO : filtro.idDespliegue(),
            tipoEvento != null,
            nuloAValor(tipoEvento),
            filtro.desde() != null,
            filtro.desde() == null ? FECHA_NEUTRA : filtro.desde(),
            filtro.hasta() != null,
            filtro.hasta() == null ? FECHA_NEUTRA : filtro.hasta(),
            PageRequest.of(filtro.pagina(), filtro.tamano(), aOrden(filtro.orden()))
        );

        return new Pagina<>(
            pagina.getContent().stream().map(this::aDominio).toList(),
            pagina.getNumber(),
            pagina.getSize(),
            pagina.getTotalElements(),
            pagina.getTotalPages(),
            pagina.hasNext()
        );
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

    private boolean coincide(EventoActualizacionEntidad existente, EventoActualizacion evento) {
        return iguales(existente.getTipoEvento(), evento.tipoEvento())
            && iguales(existente.getMensajeEvento(), evento.mensajeEvento())
            && iguales(existente.getVersionAnterior(), evento.versionAnterior())
            && iguales(existente.getVersionNueva(), evento.versionNueva())
            && iguales(existente.getMetadatosJson(), aJson(evento.metadatos()))
            && iguales(
                existente.getObjetivoDespliegue() == null ? null : existente.getObjetivoDespliegue().getId(),
                evento.idObjetivoDespliegue()
            );
    }

    private boolean iguales(Object actual, Object esperado) {
        return actual == null ? esperado == null : actual.equals(esperado);
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

    private Sort aOrden(String orden) {
        String[] partes = orden == null ? new String[0] : orden.split(",", 2);
        String campo = partes.length > 0 ? partes[0] : "creadoEn";
        Sort.Direction direccion = partes.length > 1 && "asc".equalsIgnoreCase(partes[1])
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;

        return Sort.by(direccion, switch (campo) {
            case "createdAt", "creadoEn" -> "creadoEn";
            case "eventType", "tipoEvento" -> "tipoEvento";
            case "oldVersion", "versionAnterior" -> "versionAnterior";
            case "newVersion", "versionNueva" -> "versionNueva";
            default -> "creadoEn";
        });
    }

    private String minusculaANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim().toLowerCase(Locale.ROOT);
    }

    private String nuloAValor(String valor) {
        return valor == null ? "" : valor;
    }
}
