package com.farmamia.operations.infraestructura.persistencia.repositorio;

import com.farmamia.operations.infraestructura.persistencia.entidad.SucursalEntidad;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SucursalRepositorioJpa extends JpaRepository<SucursalEntidad, UUID> {

    Optional<SucursalEntidad> findByCodigo(String codigo);
}
