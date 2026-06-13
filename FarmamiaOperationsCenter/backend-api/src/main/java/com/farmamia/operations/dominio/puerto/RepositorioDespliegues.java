package com.farmamia.operations.dominio.puerto;

import com.farmamia.operations.dominio.modelo.DatosCrearDespliegue;
import com.farmamia.operations.dominio.modelo.Despliegue;
import com.farmamia.operations.dominio.modelo.EstadoDespliegue;
import com.farmamia.operations.dominio.modelo.FiltroDespliegues;
import com.farmamia.operations.dominio.modelo.Pagina;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface RepositorioDespliegues {

    Despliegue crear(DatosCrearDespliegue datos);

    List<Despliegue> listar();

    Pagina<Despliegue> listarPaginado(FiltroDespliegues filtro);

    Despliegue obtener(UUID id);

    Despliegue programar(UUID id, OffsetDateTime programadoEn);

    Despliegue pausar(UUID id);

    Despliegue reanudar(UUID id);

    Despliegue cancelar(UUID id);

    EstadoDespliegue estado(UUID id);

    int contarFarmaciasTurno(UUID id);
}
