package com.farmamia.posupdate.dominio.puerto;

import com.farmamia.posupdate.dominio.modelo.EventoActualizacion;
import com.farmamia.posupdate.dominio.modelo.EventoActualizacionRegistrado;
import com.farmamia.posupdate.dominio.modelo.FiltroEventosActualizacion;
import com.farmamia.posupdate.dominio.modelo.Pagina;
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
