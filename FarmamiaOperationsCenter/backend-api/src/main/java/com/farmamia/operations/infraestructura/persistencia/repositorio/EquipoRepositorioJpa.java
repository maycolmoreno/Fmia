package com.farmamia.operations.infraestructura.persistencia.repositorio;

import com.farmamia.operations.infraestructura.persistencia.entidad.EquipoEntidad;
import java.util.Optional;
import java.util.UUID;
import java.time.OffsetDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EquipoRepositorioJpa extends JpaRepository<EquipoEntidad, UUID> {

    Optional<EquipoEntidad> findByNombreEquipo(String nombreEquipo);

    long countByEstado(String estado);

    @Override
    @EntityGraph(attributePaths = "sucursal")
    java.util.List<EquipoEntidad> findAll();

    @EntityGraph(attributePaths = "sucursal")
    @Query("""
        select equipo
        from EquipoEntidad equipo
        join equipo.sucursal sucursal
        where (:q is null
            or lower(equipo.nombreEquipo) like concat('%', :q, '%')
            or lower(coalesce(equipo.direccionIp, '')) like concat('%', :q, '%')
            or lower(coalesce(equipo.direccionMac, '')) like concat('%', :q, '%'))
          and (:estado is null or lower(equipo.estado) = :estado)
          and (:codigoSucursal is null or lower(sucursal.codigo) = :codigoSucursal)
          and (:versionPos is null or lower(coalesce(equipo.versionPos, '')) = :versionPos)
          and (:versionAgente is null or lower(coalesce(equipo.versionAgente, '')) = :versionAgente)
          and (:ultimoLatidoDesde is null or equipo.ultimoLatidoEn >= :ultimoLatidoDesde)
          and (:ultimoLatidoHasta is null or equipo.ultimoLatidoEn <= :ultimoLatidoHasta)
        """)
    Page<EquipoEntidad> buscarConFiltros(
        @Param("q") String q,
        @Param("estado") String estado,
        @Param("codigoSucursal") String codigoSucursal,
        @Param("versionPos") String versionPos,
        @Param("versionAgente") String versionAgente,
        @Param("ultimoLatidoDesde") OffsetDateTime ultimoLatidoDesde,
        @Param("ultimoLatidoHasta") OffsetDateTime ultimoLatidoHasta,
        Pageable pageable
    );
}
