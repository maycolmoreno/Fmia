package com.farmamia.posupdate.dominio.puerto;

import com.farmamia.posupdate.dominio.modelo.ObjetivoDespliegueEquipo;
import java.util.List;
import java.util.UUID;

public interface RepositorioObjetivosEquipo {

    List<ObjetivoDespliegueEquipo> listarPorEquipo(UUID idEquipo);
}
