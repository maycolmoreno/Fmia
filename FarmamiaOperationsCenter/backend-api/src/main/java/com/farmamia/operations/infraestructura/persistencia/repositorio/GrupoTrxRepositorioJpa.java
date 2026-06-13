package com.farmamia.operations.infraestructura.persistencia.repositorio;

import com.farmamia.operations.infraestructura.persistencia.entidad.GrupoTrxEntidad;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GrupoTrxRepositorioJpa extends JpaRepository<GrupoTrxEntidad, UUID> {

    Optional<GrupoTrxEntidad> findByCodigo(String codigo);

    @Query("""
        select grupo
        from GrupoTrxEntidad grupo
        where (:filtrarCodigo = false or lower(grupo.codigo) like concat('%', :codigo, '%'))
          and (:filtrarEstado = false or grupo.estado = :estado)
          and (:filtrarActivo = false or grupo.activo = :activo)
        """)
    Page<GrupoTrxEntidad> buscarConFiltros(
        @Param("filtrarCodigo") boolean filtrarCodigo,
        @Param("codigo") String codigo,
        @Param("filtrarEstado") boolean filtrarEstado,
        @Param("estado") String estado,
        @Param("filtrarActivo") boolean filtrarActivo,
        @Param("activo") boolean activo,
        Pageable pageable
    );
}
