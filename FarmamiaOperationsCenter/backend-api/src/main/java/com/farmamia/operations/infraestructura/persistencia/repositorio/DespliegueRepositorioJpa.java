package com.farmamia.operations.infraestructura.persistencia.repositorio;

import com.farmamia.operations.infraestructura.persistencia.entidad.DespliegueEntidad;
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
        where (:q is null
            or lower(despliegue.nombre) like concat('%', :q, '%')
            or lower(coalesce(despliegue.descripcion, '')) like concat('%', :q, '%'))
          and (:estado is null or lower(despliegue.estado) = :estado)
          and (:versionPaquete is null or lower(paquete.version) = :versionPaquete)
          and (:creadoDesde is null or despliegue.creadoEn >= :creadoDesde)
          and (:creadoHasta is null or despliegue.creadoEn <= :creadoHasta)
        """)
    Page<DespliegueEntidad> buscarConFiltros(
        @Param("q") String q,
        @Param("estado") String estado,
        @Param("versionPaquete") String versionPaquete,
        @Param("creadoDesde") OffsetDateTime creadoDesde,
        @Param("creadoHasta") OffsetDateTime creadoHasta,
        Pageable pageable
    );
}
