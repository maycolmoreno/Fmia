package com.farmamia.posupdate.infraestructura.persistencia.repositorio;

import com.farmamia.posupdate.infraestructura.persistencia.entidad.DespliegueEntidad;
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

public interface DespliegueRepositorioJpa extends JpaRepository<DespliegueEntidad, UUID> {

    long countByEstadoIn(List<String> estados);

    @EntityGraph(attributePaths = "paquete")
    Optional<DespliegueEntidad> findFirstByEstadoInOrderByCreadoEnDesc(List<String> estados);

    @Override
    @EntityGraph(attributePaths = "paquete")
    List<DespliegueEntidad> findAll();

    @Override
    @EntityGraph(attributePaths = "paquete")
    Optional<DespliegueEntidad> findById(UUID id);

    @EntityGraph(attributePaths = "paquete")
    @Query("""
        select despliegue
        from DespliegueEntidad despliegue
        join despliegue.paquete paquete
        where (:filtrarQ = false
            or lower(despliegue.nombre) like concat('%', :q, '%')
            or lower(coalesce(despliegue.descripcion, '')) like concat('%', :q, '%'))
          and (:filtrarEstado = false or lower(despliegue.estado) = :estado)
          and (:filtrarVersionPaquete = false or lower(paquete.version) = :versionPaquete)
          and (:filtrarCreadoDesde = false or despliegue.creadoEn >= :creadoDesde)
          and (:filtrarCreadoHasta = false or despliegue.creadoEn <= :creadoHasta)
        """)
    Page<DespliegueEntidad> buscarConFiltros(
        @Param("filtrarQ") boolean filtrarQ,
        @Param("q") String q,
        @Param("filtrarEstado") boolean filtrarEstado,
        @Param("estado") String estado,
        @Param("filtrarVersionPaquete") boolean filtrarVersionPaquete,
        @Param("versionPaquete") String versionPaquete,
        @Param("filtrarCreadoDesde") boolean filtrarCreadoDesde,
        @Param("creadoDesde") OffsetDateTime creadoDesde,
        @Param("filtrarCreadoHasta") boolean filtrarCreadoHasta,
        @Param("creadoHasta") OffsetDateTime creadoHasta,
        Pageable pageable
    );
}
