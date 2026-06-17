package com.farmamia.posupdate.aplicacion.casouso;

import com.farmamia.posupdate.dominio.modelo.CampanaGrupoTrx;
import com.farmamia.posupdate.dominio.modelo.ResumenCampanaGruposTrx;
import com.farmamia.posupdate.dominio.puerto.RepositorioCampanaGruposTrx;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GestionarCampanaGruposTrxCasoUso {

    private final RepositorioCampanaGruposTrx repositorioCampanaGruposTrx;

    public GestionarCampanaGruposTrxCasoUso(RepositorioCampanaGruposTrx repositorioCampanaGruposTrx) {
        this.repositorioCampanaGruposTrx = repositorioCampanaGruposTrx;
    }

    @Transactional(readOnly = true)
    public ResumenCampanaGruposTrx estadoPorTrx(UUID idCampana) {
        return repositorioCampanaGruposTrx.estadoPorTrx(idCampana);
    }

    @Transactional
    public CampanaGrupoTrx asociar(UUID idCampana, UUID idGrupoTrx) {
        return repositorioCampanaGruposTrx.asociar(idCampana, idGrupoTrx);
    }

    @Transactional
    public void quitar(UUID idCampana, UUID idGrupoTrx) {
        repositorioCampanaGruposTrx.quitar(idCampana, idGrupoTrx);
    }

    @Transactional
    public CampanaGrupoTrx pausar(UUID idCampana, UUID idGrupoTrx, String motivo) {
        return repositorioCampanaGruposTrx.pausar(idCampana, idGrupoTrx, limpiar(motivo));
    }

    @Transactional
    public CampanaGrupoTrx reanudar(UUID idCampana, UUID idGrupoTrx) {
        return repositorioCampanaGruposTrx.reanudar(idCampana, idGrupoTrx);
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
