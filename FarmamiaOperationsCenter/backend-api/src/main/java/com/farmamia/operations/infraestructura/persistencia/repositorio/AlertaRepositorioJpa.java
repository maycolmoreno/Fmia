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

    @EntityGraph(attributePaths = {"equipo", "equipo.sucursal"})
    List<AlertaEntidad> findByEstadoIn(List<String> estados);

    @Query("""
        select alerta
        from AlertaEntidad alerta
        join fetch alerta.equipo equipo
        join fetch equipo.sucursal sucursal
        left join fetch alerta.reconocidaPor reconocidaPor
        left join fetch alerta.cerradaPor cerradaPor
        where (:filtrarEstado = false or lower(alerta.estado) = :estado)
          and (:filtrarSeveridad = false or lower(alerta.severidad) = :severidad)
          and (:filtrarTipo = false or lower(alerta.tipoAlerta) = :tipo)
          and (:filtrarIdEquipo = false or equipo.id = :idEquipo)
          and (:filtrarIdSucursal = false or sucursal.id = :idSucursal)
          and (:filtrarCodigoSucursal = false or lower(sucursal.codigo) = :codigoSucursal)
          and (:filtrarNombreEquipo = false or lower(equipo.nombreEquipo) like concat('%', :nombreEquipo, '%'))
          and (:filtrarFechaDesde = false or alerta.abiertaEn >= :fechaDesde)
          and (:filtrarFechaHasta = false or alerta.abiertaEn <= :fechaHasta)
        """)
    List<AlertaEntidad> buscarConFiltros(
        @Param("filtrarEstado") boolean filtrarEstado,
        @Param("estado") String estado,
        @Param("filtrarSeveridad") boolean filtrarSeveridad,
        @Param("severidad") String severidad,
        @Param("filtrarTipo") boolean filtrarTipo,
        @Param("tipo") String tipo,
        @Param("filtrarIdEquipo") boolean filtrarIdEquipo,
        @Param("idEquipo") UUID idEquipo,
        @Param("filtrarIdSucursal") boolean filtrarIdSucursal,
        @Param("idSucursal") UUID idSucursal,
        @Param("filtrarCodigoSucursal") boolean filtrarCodigoSucursal,
        @Param("codigoSucursal") String codigoSucursal,
        @Param("filtrarNombreEquipo") boolean filtrarNombreEquipo,
        @Param("nombreEquipo") String nombreEquipo,
        @Param("filtrarFechaDesde") boolean filtrarFechaDesde,
        @Param("fechaDesde") OffsetDateTime fechaDesde,
        @Param("filtrarFechaHasta") boolean filtrarFechaHasta,
        @Param("fechaHasta") OffsetDateTime fechaHasta,
        Pageable pageable
    );

    @EntityGraph(attributePaths = {"equipo", "equipo.sucursal", "reconocidaPor", "cerradaPor"})
    @Query("""
        select alerta
        from AlertaEntidad alerta
        join alerta.equipo equipo
        join equipo.sucursal sucursal
        where (:filtrarEstado = false or lower(alerta.estado) = :estado)
          and (:filtrarSeveridad = false or lower(alerta.severidad) = :severidad)
          and (:filtrarTipo = false or lower(alerta.tipoAlerta) = :tipo)
          and (:filtrarIdEquipo = false or equipo.id = :idEquipo)
          and (:filtrarIdSucursal = false or sucursal.id = :idSucursal)
          and (:filtrarCodigoSucursal = false or lower(sucursal.codigo) = :codigoSucursal)
          and (:filtrarNombreEquipo = false or lower(equipo.nombreEquipo) like concat('%', :nombreEquipo, '%'))
          and (:filtrarFechaDesde = false or alerta.abiertaEn >= :fechaDesde)
          and (:filtrarFechaHasta = false or alerta.abiertaEn <= :fechaHasta)
        """)
    Page<AlertaEntidad> buscarConFiltrosPaginado(
        @Param("filtrarEstado") boolean filtrarEstado,
        @Param("estado") String estado,
        @Param("filtrarSeveridad") boolean filtrarSeveridad,
        @Param("severidad") String severidad,
        @Param("filtrarTipo") boolean filtrarTipo,
        @Param("tipo") String tipo,
        @Param("filtrarIdEquipo") boolean filtrarIdEquipo,
        @Param("idEquipo") UUID idEquipo,
        @Param("filtrarIdSucursal") boolean filtrarIdSucursal,
        @Param("idSucursal") UUID idSucursal,
        @Param("filtrarCodigoSucursal") boolean filtrarCodigoSucursal,
        @Param("codigoSucursal") String codigoSucursal,
        @Param("filtrarNombreEquipo") boolean filtrarNombreEquipo,
        @Param("nombreEquipo") String nombreEquipo,
        @Param("filtrarFechaDesde") boolean filtrarFechaDesde,
        @Param("fechaDesde") OffsetDateTime fechaDesde,
        @Param("filtrarFechaHasta") boolean filtrarFechaHasta,
        @Param("fechaHasta") OffsetDateTime fechaHasta,
        Pageable pageable
    );
}
