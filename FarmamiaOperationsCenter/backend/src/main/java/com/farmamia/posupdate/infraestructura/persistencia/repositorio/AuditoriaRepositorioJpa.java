package com.farmamia.posupdate.infraestructura.persistencia.repositorio;

import com.farmamia.posupdate.infraestructura.persistencia.entidad.AuditoriaEntidad;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditoriaRepositorioJpa extends JpaRepository<AuditoriaEntidad, UUID> {

    @EntityGraph(attributePaths = "usuarioActor")
    List<AuditoriaEntidad> findByOrderByCreadoEnDesc(Pageable pageable);

    @Query("""
        select auditoria
        from AuditoriaEntidad auditoria
        left join fetch auditoria.usuarioActor usuario
        where (:filtrarAccion = false or lower(auditoria.accion) = :accion)
          and (:filtrarTipoEntidad = false or lower(auditoria.tipoEntidad) = :tipoEntidad)
          and (:filtrarUsuarioActor = false or lower(usuario.usuario) = :usuarioActor)
          and (:filtrarDesde = false or auditoria.creadoEn >= :desde)
          and (:filtrarHasta = false or auditoria.creadoEn <= :hasta)
        order by auditoria.creadoEn desc
        """)
    List<AuditoriaEntidad> buscarConFiltros(
        @Param("filtrarAccion") boolean filtrarAccion,
        @Param("accion") String accion,
        @Param("filtrarTipoEntidad") boolean filtrarTipoEntidad,
        @Param("tipoEntidad") String tipoEntidad,
        @Param("filtrarUsuarioActor") boolean filtrarUsuarioActor,
        @Param("usuarioActor") String usuarioActor,
        @Param("filtrarDesde") boolean filtrarDesde,
        @Param("desde") OffsetDateTime desde,
        @Param("filtrarHasta") boolean filtrarHasta,
        @Param("hasta") OffsetDateTime hasta,
        Pageable pageable
    );

    @EntityGraph(attributePaths = "usuarioActor")
    @Query("""
        select auditoria
        from AuditoriaEntidad auditoria
        left join auditoria.usuarioActor usuario
        where (:filtrarAccion = false or lower(auditoria.accion) = :accion)
          and (:filtrarTipoEntidad = false or lower(auditoria.tipoEntidad) = :tipoEntidad)
          and (:filtrarUsuarioActor = false or lower(usuario.usuario) = :usuarioActor)
          and (:filtrarDesde = false or auditoria.creadoEn >= :desde)
          and (:filtrarHasta = false or auditoria.creadoEn <= :hasta)
        """)
    Page<AuditoriaEntidad> buscarConFiltrosPaginado(
        @Param("filtrarAccion") boolean filtrarAccion,
        @Param("accion") String accion,
        @Param("filtrarTipoEntidad") boolean filtrarTipoEntidad,
        @Param("tipoEntidad") String tipoEntidad,
        @Param("filtrarUsuarioActor") boolean filtrarUsuarioActor,
        @Param("usuarioActor") String usuarioActor,
        @Param("filtrarDesde") boolean filtrarDesde,
        @Param("desde") OffsetDateTime desde,
        @Param("filtrarHasta") boolean filtrarHasta,
        @Param("hasta") OffsetDateTime hasta,
        Pageable pageable
    );

    @Modifying
    @Query("delete from AuditoriaEntidad auditoria where auditoria.creadoEn < :fechaCorte")
    int eliminarAnterioresA(@Param("fechaCorte") OffsetDateTime fechaCorte);
}
