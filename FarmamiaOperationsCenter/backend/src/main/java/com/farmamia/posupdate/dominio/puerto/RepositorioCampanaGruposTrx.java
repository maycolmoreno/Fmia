package com.farmamia.posupdate.dominio.puerto;

import com.farmamia.posupdate.dominio.modelo.CampanaGrupoTrx;
import com.farmamia.posupdate.dominio.modelo.ResumenCampanaGruposTrx;
import java.util.UUID;

public interface RepositorioCampanaGruposTrx {

    ResumenCampanaGruposTrx estadoPorTrx(UUID idCampana);

    CampanaGrupoTrx asociar(UUID idCampana, UUID idGrupoTrx);

    void quitar(UUID idCampana, UUID idGrupoTrx);

    CampanaGrupoTrx pausar(UUID idCampana, UUID idGrupoTrx, String motivo);

    CampanaGrupoTrx reanudar(UUID idCampana, UUID idGrupoTrx);

    boolean instruccionBloqueada(UUID idCampana, UUID idGrupoTrx, String codigoGrupoLegacy);
}
