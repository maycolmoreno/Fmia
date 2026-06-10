package com.farmamia.operations.infraestructura.persistencia.repositorio;

import com.farmamia.operations.infraestructura.persistencia.entidad.MetricaEquipoEntidad;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MetricaEquipoRepositorioJpa extends JpaRepository<MetricaEquipoEntidad, UUID> {

    Optional<MetricaEquipoEntidad> findFirstByEquipo_IdOrderByRecolectadoEnDesc(UUID idEquipo);
}
