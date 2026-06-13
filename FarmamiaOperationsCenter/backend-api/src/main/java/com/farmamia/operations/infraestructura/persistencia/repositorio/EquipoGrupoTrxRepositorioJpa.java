package com.farmamia.operations.infraestructura.persistencia.repositorio;

import com.farmamia.operations.infraestructura.persistencia.entidad.EquipoGrupoTrxEntidad;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EquipoGrupoTrxRepositorioJpa extends JpaRepository<EquipoGrupoTrxEntidad, UUID> {

    long countByGrupoTrxId(UUID grupoTrxId);

    @Query("""
        select count(distinct sucursal.id)
        from EquipoGrupoTrxEntidad asignacion
        join asignacion.equipo equipo
        join equipo.sucursal sucursal
        where asignacion.grupoTrx.id = :grupoTrxId
        """)
    long contarFarmaciasPorGrupo(@Param("grupoTrxId") UUID grupoTrxId);

    Optional<EquipoGrupoTrxEntidad> findByEquipoId(UUID equipoId);

    boolean existsByGrupoTrxId(UUID grupoTrxId);

    @EntityGraph(attributePaths = {"equipo", "equipo.sucursal"})
    List<EquipoGrupoTrxEntidad> findByGrupoTrxId(UUID grupoTrxId);
}
