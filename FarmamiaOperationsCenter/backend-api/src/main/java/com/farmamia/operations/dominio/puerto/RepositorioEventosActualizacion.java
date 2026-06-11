package com.farmamia.operations.dominio.puerto;

import com.farmamia.operations.dominio.modelo.EventoActualizacion;
import com.farmamia.operations.dominio.modelo.EventoActualizacionRegistrado;
import com.farmamia.operations.dominio.modelo.FiltroEventosActualizacion;
import com.farmamia.operations.dominio.modelo.Pagina;
import java.util.List;
import java.util.UUID;

public interface RepositorioEventosActualizacion {

    void guardar(EventoActualizacion evento);

    boolean existeConIdempotencia(UUID idEquipo, String idempotencyKey);

    boolean coincideConIdempotencia(EventoActualizacion evento);

    List<EventoActualizacionRegistrado> listarRecientes(int limite);

    Pagina<EventoActualizacionRegistrado> listarPaginado(FiltroEventosActualizacion filtro);

    List<EventoActualizacionRegistrado> listarRecientesPorEquipo(UUID idEquipo, int limite);
}
