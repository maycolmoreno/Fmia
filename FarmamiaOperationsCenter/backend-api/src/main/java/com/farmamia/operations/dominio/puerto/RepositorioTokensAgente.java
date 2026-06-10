package com.farmamia.operations.dominio.puerto;

import java.util.UUID;

public interface RepositorioTokensAgente {

    void renovarTokenActivo(UUID idEquipo, String hashToken);
}
