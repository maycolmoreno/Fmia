package com.farmamia.operations.dominio.puerto;

import com.farmamia.operations.dominio.modelo.DatosRegistroAgente;
import com.farmamia.operations.dominio.modelo.Equipo;
import com.farmamia.operations.dominio.modelo.FiltroEquipos;
import com.farmamia.operations.dominio.modelo.Pagina;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioEquipos {

    Equipo registrarOActualizar(UUID idSucursal, DatosRegistroAgente datosRegistro);

    Optional<Equipo> buscarPorId(UUID idEquipo);

    List<Equipo> listar();

    Pagina<Equipo> listarPaginado(FiltroEquipos filtro);

    void registrarLatido(UUID idEquipo, String versionPos);

    void actualizarVersionPos(UUID idEquipo, String versionPos);
}
