package com.farmamia.operations.dominio.puerto;

import com.farmamia.operations.dominio.modelo.ObjetivoDespliegueEquipo;
import java.util.List;
import java.util.UUID;

public interface RepositorioObjetivosEquipo {

    List<ObjetivoDespliegueEquipo> listarPorEquipo(UUID idEquipo);
}
