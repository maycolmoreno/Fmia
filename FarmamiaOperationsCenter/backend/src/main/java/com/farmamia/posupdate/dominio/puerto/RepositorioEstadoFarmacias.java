package com.farmamia.posupdate.dominio.puerto;

import com.farmamia.posupdate.dominio.modelo.EstadoOperacionalFarmacia;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioEstadoFarmacias {

    List<EstadoOperacionalFarmacia> listar();

    Optional<EstadoOperacionalFarmacia> buscarPorId(UUID idFarmacia);
}
