package com.farmamia.operations.infraestructura.persistencia.repositorio;

import com.farmamia.operations.infraestructura.persistencia.entidad.EventoActualizacionEntidad;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventoActualizacionRepositorioJpa extends JpaRepository<EventoActualizacionEntidad, UUID> {

    long countByTipoEventoIn(List<String> tiposEvento);

    boolean existsByEquipo_IdAndIdempotencyKey(UUID idEquipo, String idempotencyKey);

    Optional<EventoActualizacionEntidad> findByEquipo_IdAndIdempotencyKey(UUID idEquipo, String idempotencyKey);

    @EntityGraph(attributePaths = {"equipo", "despliegue", "objetivoDespliegue"})
    List<EventoActualizacionEntidad> findByOrderByCreadoEnDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"equipo", "despliegue", "objetivoDespliegue"})
    List<EventoActualizacionEntidad> findByEquipo_IdOrderByCreadoEnDesc(UUID idEquipo, Pageable pageable);

    @EntityGraph(attributePaths = {"equipo", "despliegue", "objetivoDespliegue"})
    @Query("""
        select evento
        from EventoActualizacionEntidad evento
        where (:idEquipo is null or evento.equipo.id = :idEquipo)
          and (:idDespliegue is null or evento.despliegue.id = :idDespliegue)
          and (:tipoEvento is null or lower(evento.tipoEvento) = :tipoEvento)
          and (:desde is null or evento.creadoEn >= :desde)
          and (:hasta is null or evento.creadoEn <= :hasta)
        """)
    Page<EventoActualizacionEntidad> buscarConFiltros(
        @Param("idEquipo") UUID idEquipo,
        @Param("idDespliegue") UUID idDespliegue,
        @Param("tipoEvento") String tipoEvento,
        @Param("desde") OffsetDateTime desde,
        @Param("hasta") OffsetDateTime hasta,
        Pageable pageable
    );
}
