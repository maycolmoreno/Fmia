package com.farmamia.operations.infraestructura.persistencia.repositorio;

import com.farmamia.operations.infraestructura.persistencia.entidad.AuditoriaEntidad;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditoriaRepositorioJpa extends JpaRepository<AuditoriaEntidad, UUID> {

    @EntityGraph(attributePaths = "usuarioActor")
    List<AuditoriaEntidad> findByOrderByCreadoEnDesc(Pageable pageable);

    @Query("""
        select auditoria
        from AuditoriaEntidad auditoria
        left join fetch auditoria.usuarioActor usuario
        where (:accion is null or lower(auditoria.accion) = :accion)
          and (:tipoEntidad is null or lower(auditoria.tipoEntidad) = :tipoEntidad)
          and (:usuarioActor is null or lower(usuario.usuario) = :usuarioActor)
          and (:desde is null or auditoria.creadoEn >= :desde)
          and (:hasta is null or auditoria.creadoEn <= :hasta)
        order by auditoria.creadoEn desc
        """)
    List<AuditoriaEntidad> buscarConFiltros(
        @Param("accion") String accion,
        @Param("tipoEntidad") String tipoEntidad,
        @Param("usuarioActor") String usuarioActor,
        @Param("desde") OffsetDateTime desde,
        @Param("hasta") OffsetDateTime hasta,
        Pageable pageable
    );

    @EntityGraph(attributePaths = "usuarioActor")
    @Query("""
        select auditoria
        from AuditoriaEntidad auditoria
        left join auditoria.usuarioActor usuario
        where (:accion is null or lower(auditoria.accion) = :accion)
          and (:tipoEntidad is null or lower(auditoria.tipoEntidad) = :tipoEntidad)
          and (:usuarioActor is null or lower(usuario.usuario) = :usuarioActor)
          and (:desde is null or auditoria.creadoEn >= :desde)
          and (:hasta is null or auditoria.creadoEn <= :hasta)
        """)
    Page<AuditoriaEntidad> buscarConFiltrosPaginado(
        @Param("accion") String accion,
        @Param("tipoEntidad") String tipoEntidad,
        @Param("usuarioActor") String usuarioActor,
        @Param("desde") OffsetDateTime desde,
        @Param("hasta") OffsetDateTime hasta,
        Pageable pageable
    );
}
