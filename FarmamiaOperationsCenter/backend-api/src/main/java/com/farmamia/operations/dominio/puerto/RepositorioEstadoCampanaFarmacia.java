package com.farmamia.operations.dominio.puerto;

import com.farmamia.operations.dominio.modelo.FiltroEstadoCampanaFarmacia;
import com.farmamia.operations.dominio.modelo.ResumenEstadoCampanaFarmacia;
import java.util.UUID;

public interface RepositorioEstadoCampanaFarmacia {

    ResumenEstadoCampanaFarmacia consultar(UUID idCampana, FiltroEstadoCampanaFarmacia filtro);
}
