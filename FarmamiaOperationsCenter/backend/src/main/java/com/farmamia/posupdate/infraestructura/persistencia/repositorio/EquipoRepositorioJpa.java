package com.farmamia.posupdate.infraestructura.persistencia.repositorio;

import com.farmamia.posupdate.infraestructura.persistencia.entidad.EquipoEntidad;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EquipoRepositorioJpa extends JpaRepository<EquipoEntidad, UUID> {

    Optional<EquipoEntidad> findByNombreEquipo(String nombreEquipo);

    long countByEstado(String estado);

    @Query("""
        select equipo.versionPos
        from EquipoEntidad equipo
        where equipo.estado = 'ONLINE' and equipo.versionPos is not null
        group by equipo.versionPos
        order by count(equipo.id) desc
        """)
    List<String> findVersionesPosPorFrecuencia(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "sucursal")
    java.util.List<EquipoEntidad> findAll();

    List<EquipoEntidad> findBySucursalIsNullOrderByNombreEquipoAsc();

    long countByIdInAndSucursalIsNull(Set<UUID> ids);

    @Query("""
        select equipo.id
        from EquipoEntidad equipo
        join equipo.sucursal sucursal
        join sucursal.grupoTrx grupoTrx
        where grupoTrx.codigo = :codigoGrupoTrx
        """)
    List<UUID> findIdsByGrupoTrxCodigo(@Param("codigoGrupoTrx") String codigoGrupoTrx);

    @EntityGraph(attributePaths = "sucursal")
    @Query("""
        select equipo
        from EquipoEntidad equipo
        left join equipo.sucursal sucursal
        where (:filtrarQ = false
            or lower(equipo.nombreEquipo) like concat('%', :q, '%')
            or lower(coalesce(equipo.direccionIp, '')) like concat('%', :q, '%')
            or lower(coalesce(equipo.direccionMac, '')) like concat('%', :q, '%'))
          and (:filtrarEstado = false or lower(equipo.estado) = :estado)
          and (:filtrarCodigoSucursal = false or lower(sucursal.codigo) = :codigoSucursal)
          and (:filtrarVersionPos = false or lower(coalesce(equipo.versionPos, '')) = :versionPos)
          and (:filtrarVersionAgente = false or lower(coalesce(equipo.versionAgente, '')) = :versionAgente)
          and (:filtrarUltimoLatidoDesde = false or equipo.ultimoLatidoEn >= :ultimoLatidoDesde)
          and (:filtrarUltimoLatidoHasta = false or equipo.ultimoLatidoEn <= :ultimoLatidoHasta)
        """)
    Page<EquipoEntidad> buscarConFiltros(
        @Param("filtrarQ") boolean filtrarQ,
        @Param("q") String q,
        @Param("filtrarEstado") boolean filtrarEstado,
        @Param("estado") String estado,
        @Param("filtrarCodigoSucursal") boolean filtrarCodigoSucursal,
        @Param("codigoSucursal") String codigoSucursal,
        @Param("filtrarVersionPos") boolean filtrarVersionPos,
        @Param("versionPos") String versionPos,
        @Param("filtrarVersionAgente") boolean filtrarVersionAgente,
        @Param("versionAgente") String versionAgente,
        @Param("filtrarUltimoLatidoDesde") boolean filtrarUltimoLatidoDesde,
        @Param("ultimoLatidoDesde") OffsetDateTime ultimoLatidoDesde,
        @Param("filtrarUltimoLatidoHasta") boolean filtrarUltimoLatidoHasta,
        @Param("ultimoLatidoHasta") OffsetDateTime ultimoLatidoHasta,
        Pageable pageable
    );
}
