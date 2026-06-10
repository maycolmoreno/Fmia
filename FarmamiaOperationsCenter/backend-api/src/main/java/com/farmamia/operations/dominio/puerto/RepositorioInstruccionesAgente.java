package com.farmamia.operations.dominio.puerto;

import com.farmamia.operations.dominio.modelo.InstruccionAgente;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioInstruccionesAgente {

    Optional<InstruccionAgente> buscarSiguienteParaEquipo(UUID idEquipo);
}
