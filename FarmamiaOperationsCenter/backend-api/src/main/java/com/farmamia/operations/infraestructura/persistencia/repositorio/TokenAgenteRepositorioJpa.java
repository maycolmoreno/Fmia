package com.farmamia.operations.infraestructura.persistencia.repositorio;

import com.farmamia.operations.infraestructura.persistencia.entidad.EquipoEntidad;
import com.farmamia.operations.infraestructura.persistencia.entidad.TokenAgenteEntidad;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenAgenteRepositorioJpa extends JpaRepository<TokenAgenteEntidad, UUID> {

    Optional<TokenAgenteEntidad> findByEquipoAndRevocadoEnIsNull(EquipoEntidad equipo);

    Optional<TokenAgenteEntidad> findByHashTokenAndRevocadoEnIsNull(String hashToken);
}
