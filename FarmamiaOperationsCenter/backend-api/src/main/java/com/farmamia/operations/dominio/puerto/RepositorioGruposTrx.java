package com.farmamia.operations.dominio.puerto;

import com.farmamia.operations.dominio.modelo.DatosGrupoTrx;
import com.farmamia.operations.dominio.modelo.DetalleGrupoTrx;
import com.farmamia.operations.dominio.modelo.EstadoGrupoTrx;
import com.farmamia.operations.dominio.modelo.FiltroGruposTrx;
import com.farmamia.operations.dominio.modelo.GrupoTrx;
import com.farmamia.operations.dominio.modelo.Pagina;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioGruposTrx {

    GrupoTrx crear(DatosGrupoTrx datos);

    GrupoTrx actualizar(UUID id, DatosGrupoTrx datos);

    GrupoTrx cambiarEstado(UUID id, EstadoGrupoTrx estado);

    Optional<DetalleGrupoTrx> buscarDetallePorId(UUID id);

    Optional<GrupoTrx> buscarPorCodigo(String codigo);

    List<GrupoTrx> listar();

    Pagina<GrupoTrx> listarPaginado(FiltroGruposTrx filtro);

    GrupoTrx asignarEquipo(UUID idGrupo, UUID idEquipo);

    GrupoTrx quitarEquipo(UUID idGrupo, UUID idEquipo);
}
