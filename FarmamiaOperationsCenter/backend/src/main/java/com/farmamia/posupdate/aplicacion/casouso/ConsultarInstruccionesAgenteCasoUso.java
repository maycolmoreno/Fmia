package com.farmamia.posupdate.aplicacion.casouso;

import com.farmamia.posupdate.dominio.modelo.InstruccionAgente;
import com.farmamia.posupdate.dominio.puerto.RepositorioInstruccionesAgente;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ConsultarInstruccionesAgenteCasoUso {

    private final RepositorioInstruccionesAgente repositorioInstruccionesAgente;

    public ConsultarInstruccionesAgenteCasoUso(RepositorioInstruccionesAgente repositorioInstruccionesAgente) {
        this.repositorioInstruccionesAgente = repositorioInstruccionesAgente;
    }

    public InstruccionAgente buscarSiguienteInstruccion(UUID idEquipo) {
        return repositorioInstruccionesAgente
            .buscarSiguienteParaEquipo(idEquipo)
            .orElseGet(InstruccionAgente::vacia);
    }
}
