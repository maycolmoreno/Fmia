package com.farmamia.operations.dominio.puerto;

import com.farmamia.operations.dominio.modelo.EstadoOperacionalFarmacia;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioEstadoFarmacias {

    List<EstadoOperacionalFarmacia> listar();

    Optional<EstadoOperacionalFarmacia> buscarPorId(UUID idFarmacia);
}
