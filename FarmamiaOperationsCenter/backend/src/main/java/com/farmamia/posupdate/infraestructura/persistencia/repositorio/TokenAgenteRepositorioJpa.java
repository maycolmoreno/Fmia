package com.farmamia.posupdate.infraestructura.persistencia.repositorio;

import com.farmamia.posupdate.infraestructura.persistencia.entidad.EquipoEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.TokenAgenteEntidad;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenAgenteRepositorioJpa extends JpaRepository<TokenAgenteEntidad, UUID> {

    Optional<TokenAgenteEntidad> findByEquipoAndRevocadoEnIsNull(EquipoEntidad equipo);

    Optional<TokenAgenteEntidad> findByHashTokenAndRevocadoEnIsNull(String hashToken);
}
