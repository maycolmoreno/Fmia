package com.farmamia.posupdate.dominio.puerto;

import com.farmamia.posupdate.dominio.modelo.DatosRegistroAgente;
import com.farmamia.posupdate.dominio.modelo.DatosRegistroEquipoTecnico;
import com.farmamia.posupdate.dominio.modelo.AsignacionEquipoSucursal;
import com.farmamia.posupdate.dominio.modelo.Equipo;
import com.farmamia.posupdate.dominio.modelo.FiltroEquipos;
import com.farmamia.posupdate.dominio.modelo.Pagina;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface RepositorioEquipos {

    Equipo registrarOActualizar(UUID idSucursal, DatosRegistroAgente datosRegistro);

    default Equipo registrarTecnico(DatosRegistroEquipoTecnico datosRegistro) {
        throw new UnsupportedOperationException("Registro tecnico no implementado");
    }

    Optional<Equipo> buscarPorId(UUID idEquipo);

    List<Equipo> listar();

    Pagina<Equipo> listarPaginado(FiltroEquipos filtro);

    List<Equipo> listarHuerfanos();

    long contarHuerfanosPorIds(Set<UUID> idsEquipos);

    void asignarSucursales(List<AsignacionEquipoSucursal> asignaciones);

    void registrarLatido(UUID idEquipo, String versionPos);

    void actualizarVersionPos(UUID idEquipo, String versionPos);
}
