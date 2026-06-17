package com.farmamia.posupdate.dominio.puerto;

import com.farmamia.posupdate.dominio.modelo.FiltroEstadoCampanaFarmacia;
import com.farmamia.posupdate.dominio.modelo.ResumenEstadoCampanaFarmacia;
import java.util.UUID;

public interface RepositorioEstadoCampanaFarmacia {

    ResumenEstadoCampanaFarmacia consultar(UUID idCampana, FiltroEstadoCampanaFarmacia filtro);
}
