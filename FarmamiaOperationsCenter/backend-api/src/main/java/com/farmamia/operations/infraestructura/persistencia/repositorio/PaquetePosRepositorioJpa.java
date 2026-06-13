package com.farmamia.operations.infraestructura.persistencia.repositorio;

import com.farmamia.operations.infraestructura.persistencia.entidad.PaquetePosEntidad;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaquetePosRepositorioJpa extends JpaRepository<PaquetePosEntidad, UUID> {

    Optional<PaquetePosEntidad> findByVersion(String version);

    long countByEstado(String estado);

    @Query("""
        select paquete
        from PaquetePosEntidad paquete
        where (:filtrarQ = false
            or lower(paquete.version) like concat('%', :q, '%')
            or lower(paquete.nombreArchivo) like concat('%', :q, '%')
            or lower(paquete.checksumSha256) like concat('%', :q, '%'))
          and (:filtrarEstado = false or lower(paquete.estado) = :estado)
          and (:filtrarVersion = false or lower(paquete.version) = :version)
          and (:filtrarCargadoDesde = false or paquete.cargadoEn >= :cargadoDesde)
          and (:filtrarCargadoHasta = false or paquete.cargadoEn <= :cargadoHasta)
        """)
    Page<PaquetePosEntidad> buscarConFiltros(
        @Param("filtrarQ") boolean filtrarQ,
        @Param("q") String q,
        @Param("filtrarEstado") boolean filtrarEstado,
        @Param("estado") String estado,
        @Param("filtrarVersion") boolean filtrarVersion,
        @Param("version") String version,
        @Param("filtrarCargadoDesde") boolean filtrarCargadoDesde,
        @Param("cargadoDesde") OffsetDateTime cargadoDesde,
        @Param("filtrarCargadoHasta") boolean filtrarCargadoHasta,
        @Param("cargadoHasta") OffsetDateTime cargadoHasta,
        Pageable pageable
    );
}
