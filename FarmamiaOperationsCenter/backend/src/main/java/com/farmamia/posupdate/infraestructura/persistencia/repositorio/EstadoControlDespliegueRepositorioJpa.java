package com.farmamia.posupdate.infraestructura.persistencia.repositorio;

import com.farmamia.posupdate.infraestructura.persistencia.entidad.EstadoControlDespliegueEntidad;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstadoControlDespliegueRepositorioJpa extends JpaRepository<EstadoControlDespliegueEntidad, UUID> {

    List<EstadoControlDespliegueEntidad> findByEstado(String estado);
}
