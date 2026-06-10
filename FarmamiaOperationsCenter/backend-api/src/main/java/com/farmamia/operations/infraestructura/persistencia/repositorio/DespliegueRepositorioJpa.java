package com.farmamia.operations.infraestructura.persistencia.repositorio;

import com.farmamia.operations.infraestructura.persistencia.entidad.DespliegueEntidad;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DespliegueRepositorioJpa extends JpaRepository<DespliegueEntidad, UUID> {

    @Override
    @EntityGraph(attributePaths = "paquete")
    List<DespliegueEntidad> findAll();

    @Override
    @EntityGraph(attributePaths = "paquete")
    Optional<DespliegueEntidad> findById(UUID id);
}
