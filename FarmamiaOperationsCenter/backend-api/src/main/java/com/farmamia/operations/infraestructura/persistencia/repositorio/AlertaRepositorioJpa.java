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

    long countByTipoAlertaAndEstado(String tipoAlerta, String estado);

    @EntityGraph(attributePaths = {"equipo", "equipo.sucursal", "sucursal", "reconocidaPor", "cerradaPor"})
    List<AlertaEntidad> findByOrderByAbiertaEnDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"equipo", "equipo.sucursal", "sucursal"})
    List<AlertaEntidad> findByEstadoIn(List<String> estados);

    @Query("""
        select alerta
        from AlertaEntidad alerta
        left join fetch alerta.equipo equipo
        left join fetch equipo.sucursal equipoSucursal
        left join fetch alerta.sucursal alertaSucursal
        left join fetch alerta.reconocidaPor reconocidaPor
        left join fetch alerta.cerradaPor cerradaPor
        where (:filtrarEstado = false or lower(alerta.estado) = :estado)
          and (:filtrarSeveridad = false or lower(alerta.severidad) = :severidad)
          and (:filtrarTipo = false or lower(alerta.tipoAlerta) = :tipo)
          and (:filtrarIdEquipo = false or equipo.id = :idEquipo)
          and (:filtrarIdSucursal = false
               or (equipo is not null and equipoSucursal.id = :idSucursal)
               or (equipo is null and alertaSucursal.id = :idSucursal))
          and (:filtrarCodigoSucursal = false
               or (equipo is not null and lower(equipoSucursal.codigo) = :codigoSucursal)
               or (equipo is null and lower(coalesce(alerta.codigoSucursalRed, '')) = :codigoSucursal))
          and (:filtrarNombreEquipo = false or lower(equipo.nombreEquipo) like concat('%', :nombreEquipo, '%'))
          and (:filtrarFechaDesde = false or alerta.abiertaEn >= :fechaDesde)
          and (:filtrarFechaHasta = false or alerta.abiertaEn <= :fechaHasta)
          and (:filtrarEventoDeRed = false or alerta.eventoDeRed = :eventoDeRed)
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
        @Param("filtrarEventoDeRed") boolean filtrarEventoDeRed,
        @Param("eventoDeRed") boolean eventoDeRed,
        Pageable pageable
    );

    @EntityGraph(attributePaths = {"equipo", "equipo.sucursal", "sucursal", "reconocidaPor", "cerradaPor"})
    @Query("""
        select alerta
        from AlertaEntidad alerta
        left join alerta.equipo equipo
        left join equipo.sucursal equipoSucursal
        left join alerta.sucursal alertaSucursal
        where (:filtrarEstado = false or lower(alerta.estado) = :estado)
          and (:filtrarSeveridad = false or lower(alerta.severidad) = :severidad)
          and (:filtrarTipo = false or lower(alerta.tipoAlerta) = :tipo)
          and (:filtrarIdEquipo = false or equipo.id = :idEquipo)
          and (:filtrarIdSucursal = false
               or (equipo is not null and equipoSucursal.id = :idSucursal)
               or (equipo is null and alertaSucursal.id = :idSucursal))
          and (:filtrarCodigoSucursal = false
               or (equipo is not null and lower(equipoSucursal.codigo) = :codigoSucursal)
               or (equipo is null and lower(coalesce(alerta.codigoSucursalRed, '')) = :codigoSucursal))
          and (:filtrarNombreEquipo = false or lower(equipo.nombreEquipo) like concat('%', :nombreEquipo, '%'))
          and (:filtrarFechaDesde = false or alerta.abiertaEn >= :fechaDesde)
          and (:filtrarFechaHasta = false or alerta.abiertaEn <= :fechaHasta)
          and (:filtrarEventoDeRed = false or alerta.eventoDeRed = :eventoDeRed)
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
        @Param("filtrarEventoDeRed") boolean filtrarEventoDeRed,
        @Param("eventoDeRed") boolean eventoDeRed,
        Pageable pageable
    );
}
