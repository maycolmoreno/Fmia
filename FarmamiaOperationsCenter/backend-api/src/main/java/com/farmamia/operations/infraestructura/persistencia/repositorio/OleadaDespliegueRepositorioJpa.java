package com.farmamia.operations.infraestructura.persistencia.repositorio;

import com.farmamia.operations.infraestructura.persistencia.entidad.OleadaDespliegueEntidad;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OleadaDespliegueRepositorioJpa extends JpaRepository<OleadaDespliegueEntidad, UUID> {

    List<OleadaDespliegueEntidad> findByDespliegue_IdOrderByNumeroAsc(UUID idDespliegue);

    Optional<OleadaDespliegueEntidad> findByIdAndDespliegue_Id(UUID id, UUID idDespliegue);

    void deleteByDespliegue_Id(UUID idDespliegue);

    long countByDespliegue_Id(UUID idDespliegue);
}
