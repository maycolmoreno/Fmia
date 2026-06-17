package com.farmamia.posupdate.dominio.puerto;

import com.farmamia.posupdate.dominio.modelo.PaquetePos;
import com.farmamia.posupdate.dominio.modelo.FiltroPaquetesPos;
import com.farmamia.posupdate.dominio.modelo.Pagina;
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
