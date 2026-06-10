package com.farmamia.operations.infraestructura.persistencia.repositorio;

import com.farmamia.operations.infraestructura.persistencia.entidad.EventoActualizacionEntidad;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoActualizacionRepositorioJpa extends JpaRepository<EventoActualizacionEntidad, UUID> {

    @EntityGraph(attributePaths = {"equipo", "despliegue", "objetivoDespliegue"})
    List<EventoActualizacionEntidad> findByOrderByCreadoEnDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"equipo", "despliegue", "objetivoDespliegue"})
    List<EventoActualizacionEntidad> findByEquipo_IdOrderByCreadoEnDesc(UUID idEquipo, Pageable pageable);
}
