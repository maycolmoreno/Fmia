package com.farmamia.operations.dominio.puerto;

import com.farmamia.operations.dominio.modelo.PaquetePos;
import com.farmamia.operations.dominio.modelo.FiltroPaquetesPos;
import com.farmamia.operations.dominio.modelo.Pagina;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioPaquetesPos {

    boolean existeVersion(String version);

    PaquetePos guardarNuevo(PaquetePos paquete);

    List<PaquetePos> listar();

    Pagina<PaquetePos> listarPaginado(FiltroPaquetesPos filtro);

    Optional<PaquetePos> buscarPorId(UUID id);

    PaquetePos aprobar(UUID id);

    PaquetePos retirar(UUID id);
}
