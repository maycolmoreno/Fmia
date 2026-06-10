package com.farmamia.operations.infraestructura.persistencia.repositorio;

import com.farmamia.operations.infraestructura.persistencia.entidad.EquipoEntidad;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface EquipoRepositorioJpa extends JpaRepository<EquipoEntidad, UUID> {

    Optional<EquipoEntidad> findByNombreEquipo(String nombreEquipo);

    @Override
    @EntityGraph(attributePaths = "sucursal")
    java.util.List<EquipoEntidad> findAll();
}
