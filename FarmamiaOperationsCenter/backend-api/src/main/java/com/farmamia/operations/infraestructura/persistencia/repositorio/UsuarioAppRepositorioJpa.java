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
        where (:q is null
            or lower(usuario.usuario) like concat('%', :q, '%')
            or lower(usuario.nombreCompleto) like concat('%', :q, '%')
            or lower(coalesce(usuario.correo, '')) like concat('%', :q, '%'))
          and (:rol is null or lower(usuario.rol) = :rol)
          and (:activo is null or usuario.activo = :activo)
          and (:bloqueado is null
              or (:bloqueado = true and usuario.bloqueadoHasta is not null and usuario.bloqueadoHasta > current_timestamp)
              or (:bloqueado = false and (usuario.bloqueadoHasta is null or usuario.bloqueadoHasta <= current_timestamp)))
        """)
    Page<UsuarioAppEntidad> buscarConFiltros(
        @Param("q") String q,
        @Param("rol") String rol,
        @Param("activo") Boolean activo,
        @Param("bloqueado") Boolean bloqueado,
        Pageable pageable
    );
}
