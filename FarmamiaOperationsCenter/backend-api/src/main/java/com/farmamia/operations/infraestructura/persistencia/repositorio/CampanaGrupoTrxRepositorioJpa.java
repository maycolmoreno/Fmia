package com.farmamia.operations.infraestructura.persistencia.repositorio;

import com.farmamia.operations.infraestructura.persistencia.entidad.CampanaGrupoTrxEntidad;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampanaGrupoTrxRepositorioJpa extends JpaRepository<CampanaGrupoTrxEntidad, UUID> {

    @EntityGraph(attributePaths = {"campana", "campana.paquete", "grupoTrx"})
    List<CampanaGrupoTrxEntidad> findByCampana_IdOrderByOrdenAsc(UUID campanaId);

    @EntityGraph(attributePaths = {"campana", "campana.paquete", "grupoTrx"})
    Optional<CampanaGrupoTrxEntidad> findByCampana_IdAndGrupoTrx_Id(UUID campanaId, UUID grupoTrxId);

    boolean existsByCampana_IdAndGrupoTrx_Id(UUID campanaId, UUID grupoTrxId);

    long countByCampana_Id(UUID campanaId);
}
