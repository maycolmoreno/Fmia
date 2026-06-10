package com.farmamia.operations.aplicacion.casouso;

import com.farmamia.operations.dominio.modelo.InstruccionAgente;
import com.farmamia.operations.dominio.puerto.RepositorioInstruccionesAgente;
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
