package com.farmamia.posupdate.dominio.puerto;

import com.farmamia.posupdate.dominio.modelo.InstruccionAgente;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioInstruccionesAgente {

    Optional<InstruccionAgente> buscarSiguienteParaEquipo(UUID idEquipo);
}
