package com.farmamia.operations.dominio.puerto;

import com.farmamia.operations.dominio.modelo.EventoActualizacion;
import com.farmamia.operations.dominio.modelo.EventoActualizacionRegistrado;
import java.util.List;
import java.util.UUID;

public interface RepositorioEventosActualizacion {

    void guardar(EventoActualizacion evento);

    List<EventoActualizacionRegistrado> listarRecientes(int limite);

    List<EventoActualizacionRegistrado> listarRecientesPorEquipo(UUID idEquipo, int limite);
}
