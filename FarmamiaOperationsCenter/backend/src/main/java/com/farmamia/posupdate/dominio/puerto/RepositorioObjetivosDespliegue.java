package com.farmamia.posupdate.dominio.puerto;

import com.farmamia.posupdate.dominio.modelo.ResultadoActualizacion;
import java.util.UUID;

public interface RepositorioObjetivosDespliegue {

    void validarPerteneceAEquipo(UUID idObjetivoDespliegue, UUID idEquipo);

    void registrarResultado(UUID idEquipo, ResultadoActualizacion resultado);
}
