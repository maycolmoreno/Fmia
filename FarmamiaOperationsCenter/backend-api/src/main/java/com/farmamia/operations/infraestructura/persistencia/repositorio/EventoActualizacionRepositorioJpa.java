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
import org.springframework.data.jpa.repository.Modifying;
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
        where (:filtrarIdEquipo = false or evento.equipo.id = :idEquipo)
          and (:filtrarIdDespliegue = false or evento.despliegue.id = :idDespliegue)
          and (:filtrarTipoEvento = false or lower(evento.tipoEvento) = :tipoEvento)
          and (:filtrarDesde = false or evento.creadoEn >= :desde)
          and (:filtrarHasta = false or evento.creadoEn <= :hasta)
        """)
    Page<EventoActualizacionEntidad> buscarConFiltros(
        @Param("filtrarIdEquipo") boolean filtrarIdEquipo,
        @Param("idEquipo") UUID idEquipo,
        @Param("filtrarIdDespliegue") boolean filtrarIdDespliegue,
        @Param("idDespliegue") UUID idDespliegue,
        @Param("filtrarTipoEvento") boolean filtrarTipoEvento,
        @Param("tipoEvento") String tipoEvento,
        @Param("filtrarDesde") boolean filtrarDesde,
        @Param("desde") OffsetDateTime desde,
        @Param("filtrarHasta") boolean filtrarHasta,
        @Param("hasta") OffsetDateTime hasta,
        Pageable pageable
    );

    @Modifying
    @Query("delete from EventoActualizacionEntidad evento where evento.creadoEn < :fechaCorte")
    int eliminarAnterioresA(@Param("fechaCorte") OffsetDateTime fechaCorte);
}
