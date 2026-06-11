package com.farmamia.operations.infraestructura.persistencia.repositorio;

import com.farmamia.operations.infraestructura.persistencia.entidad.AlertaEntidad;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AlertaRepositorioJpa extends JpaRepository<AlertaEntidad, UUID> {

    long countByEstado(String estado);

    long countBySeveridad(String severidad);

    long countByEstadoAndSeveridad(String estado, String severidad);

    @EntityGraph(attributePaths = {"equipo", "equipo.sucursal", "reconocidaPor", "cerradaPor"})
    List<AlertaEntidad> findByOrderByAbiertaEnDesc(Pageable pageable);

    @Query("""
        select alerta
        from AlertaEntidad alerta
        join fetch alerta.equipo equipo
        join fetch equipo.sucursal sucursal
        left join fetch alerta.reconocidaPor reconocidaPor
        left join fetch alerta.cerradaPor cerradaPor
        where (:estado is null or lower(alerta.estado) = :estado)
          and (:severidad is null or lower(alerta.severidad) = :severidad)
          and (:tipo is null or lower(alerta.tipoAlerta) = :tipo)
          and (:idEquipo is null or equipo.id = :idEquipo)
          and (:idSucursal is null or sucursal.id = :idSucursal)
          and (:codigoSucursal is null or lower(sucursal.codigo) = :codigoSucursal)
          and (:nombreEquipo is null or lower(equipo.nombreEquipo) like concat('%', :nombreEquipo, '%'))
          and (:fechaDesde is null or alerta.abiertaEn >= :fechaDesde)
          and (:fechaHasta is null or alerta.abiertaEn <= :fechaHasta)
        """)
    List<AlertaEntidad> buscarConFiltros(
        @Param("estado") String estado,
        @Param("severidad") String severidad,
        @Param("tipo") String tipo,
        @Param("idEquipo") UUID idEquipo,
        @Param("idSucursal") UUID idSucursal,
        @Param("codigoSucursal") String codigoSucursal,
        @Param("nombreEquipo") String nombreEquipo,
        @Param("fechaDesde") OffsetDateTime fechaDesde,
        @Param("fechaHasta") OffsetDateTime fechaHasta,
        Pageable pageable
    );

    @EntityGraph(attributePaths = {"equipo", "equipo.sucursal", "reconocidaPor", "cerradaPor"})
    @Query("""
        select alerta
        from AlertaEntidad alerta
        join alerta.equipo equipo
        join equipo.sucursal sucursal
        where (:estado is null or lower(alerta.estado) = :estado)
          and (:severidad is null or lower(alerta.severidad) = :severidad)
          and (:tipo is null or lower(alerta.tipoAlerta) = :tipo)
          and (:idEquipo is null or equipo.id = :idEquipo)
          and (:idSucursal is null or sucursal.id = :idSucursal)
          and (:codigoSucursal is null or lower(sucursal.codigo) = :codigoSucursal)
          and (:nombreEquipo is null or lower(equipo.nombreEquipo) like concat('%', :nombreEquipo, '%'))
          and (:fechaDesde is null or alerta.abiertaEn >= :fechaDesde)
          and (:fechaHasta is null or alerta.abiertaEn <= :fechaHasta)
        """)
    Page<AlertaEntidad> buscarConFiltrosPaginado(
        @Param("estado") String estado,
        @Param("severidad") String severidad,
        @Param("tipo") String tipo,
        @Param("idEquipo") UUID idEquipo,
        @Param("idSucursal") UUID idSucursal,
        @Param("codigoSucursal") String codigoSucursal,
        @Param("nombreEquipo") String nombreEquipo,
        @Param("fechaDesde") OffsetDateTime fechaDesde,
        @Param("fechaHasta") OffsetDateTime fechaHasta,
        Pageable pageable
    );
}
