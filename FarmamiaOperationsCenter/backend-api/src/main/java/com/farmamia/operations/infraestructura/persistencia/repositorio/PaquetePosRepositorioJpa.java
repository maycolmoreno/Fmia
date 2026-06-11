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
        where (:q is null
            or lower(paquete.version) like concat('%', :q, '%')
            or lower(paquete.nombreArchivo) like concat('%', :q, '%')
            or lower(paquete.checksumSha256) like concat('%', :q, '%'))
          and (:estado is null or lower(paquete.estado) = :estado)
          and (:version is null or lower(paquete.version) = :version)
          and (:cargadoDesde is null or paquete.cargadoEn >= :cargadoDesde)
          and (:cargadoHasta is null or paquete.cargadoEn <= :cargadoHasta)
        """)
    Page<PaquetePosEntidad> buscarConFiltros(
        @Param("q") String q,
        @Param("estado") String estado,
        @Param("version") String version,
        @Param("cargadoDesde") OffsetDateTime cargadoDesde,
        @Param("cargadoHasta") OffsetDateTime cargadoHasta,
        Pageable pageable
    );
}
