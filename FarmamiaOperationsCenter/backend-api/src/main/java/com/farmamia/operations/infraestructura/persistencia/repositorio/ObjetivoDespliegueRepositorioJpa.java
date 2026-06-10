package com.farmamia.operations.infraestructura.persistencia.repositorio;

import com.farmamia.operations.infraestructura.persistencia.entidad.ObjetivoDespliegueEntidad;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ObjetivoDespliegueRepositorioJpa extends JpaRepository<ObjetivoDespliegueEntidad, UUID> {

    @EntityGraph(attributePaths = {"despliegue", "despliegue.paquete"})
    Optional<ObjetivoDespliegueEntidad> findFirstByEquipo_IdAndEstadoOrderByActualizadoEnDesc(
        UUID idEquipo,
        String estado
    );

    @EntityGraph(attributePaths = {"despliegue", "despliegue.paquete"})
    Optional<ObjetivoDespliegueEntidad> findByIdAndEquipo_Id(UUID id, UUID idEquipo);

    long countByDespliegue_Id(UUID idDespliegue);

    List<ObjetivoDespliegueEntidad> findByDespliegue_Id(UUID idDespliegue);

    @EntityGraph(attributePaths = {"despliegue", "despliegue.paquete"})
    List<ObjetivoDespliegueEntidad> findByEquipo_IdOrderByActualizadoEnDesc(UUID idEquipo);
}
