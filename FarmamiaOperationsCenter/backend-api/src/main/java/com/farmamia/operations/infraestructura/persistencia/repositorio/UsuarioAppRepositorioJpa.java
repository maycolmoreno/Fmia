package com.farmamia.operations.infraestructura.persistencia.repositorio;

import com.farmamia.operations.infraestructura.persistencia.entidad.UsuarioAppEntidad;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioAppRepositorioJpa extends JpaRepository<UsuarioAppEntidad, UUID> {

    Optional<UsuarioAppEntidad> findByUsuario(String usuario);

    boolean existsByUsuario(String usuario);

    boolean existsByCorreo(String correo);

    @Query("""
        select usuario
        from UsuarioAppEntidad usuario
        where (:filtrarQ = false
            or lower(usuario.usuario) like concat('%', :q, '%')
            or lower(usuario.nombreCompleto) like concat('%', :q, '%')
            or lower(coalesce(usuario.correo, '')) like concat('%', :q, '%'))
          and (:filtrarRol = false or lower(usuario.rol) = :rol)
          and (:filtrarActivo = false or usuario.activo = :activo)
          and (:filtrarBloqueado = false
              or (:bloqueado = true and usuario.bloqueadoHasta is not null and usuario.bloqueadoHasta > current_timestamp)
              or (:bloqueado = false and (usuario.bloqueadoHasta is null or usuario.bloqueadoHasta <= current_timestamp)))
        """)
    Page<UsuarioAppEntidad> buscarConFiltros(
        @Param("filtrarQ") boolean filtrarQ,
        @Param("q") String q,
        @Param("filtrarRol") boolean filtrarRol,
        @Param("rol") String rol,
        @Param("filtrarActivo") boolean filtrarActivo,
        @Param("activo") Boolean activo,
        @Param("filtrarBloqueado") boolean filtrarBloqueado,
        @Param("bloqueado") Boolean bloqueado,
        Pageable pageable
    );
}
