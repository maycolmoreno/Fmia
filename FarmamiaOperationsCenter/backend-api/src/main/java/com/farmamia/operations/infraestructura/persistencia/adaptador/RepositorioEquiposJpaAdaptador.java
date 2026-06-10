package com.farmamia.operations.infraestructura.persistencia.adaptador;

import com.farmamia.operations.aplicacion.excepcion.RecursoNoEncontradoException;
import com.farmamia.operations.dominio.modelo.DatosRegistroAgente;
import com.farmamia.operations.dominio.modelo.Equipo;
import com.farmamia.operations.dominio.puerto.RepositorioEquipos;
import com.farmamia.operations.infraestructura.persistencia.entidad.EquipoEntidad;
import com.farmamia.operations.infraestructura.persistencia.entidad.SucursalEntidad;
import com.farmamia.operations.infraestructura.persistencia.repositorio.EquipoRepositorioJpa;
import com.farmamia.operations.infraestructura.persistencia.repositorio.SucursalRepositorioJpa;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioEquiposJpaAdaptador implements RepositorioEquipos {

    private final EquipoRepositorioJpa equipoRepositorioJpa;
    private final SucursalRepositorioJpa sucursalRepositorioJpa;

    public RepositorioEquiposJpaAdaptador(
        EquipoRepositorioJpa equipoRepositorioJpa,
        SucursalRepositorioJpa sucursalRepositorioJpa
    ) {
        this.equipoRepositorioJpa = equipoRepositorioJpa;
        this.sucursalRepositorioJpa = sucursalRepositorioJpa;
    }

    @Override
    public Equipo registrarOActualizar(UUID idSucursal, DatosRegistroAgente datosRegistro) {
        SucursalEntidad sucursal = sucursalRepositorioJpa.findById(idSucursal)
            .orElseThrow(() -> new RecursoNoEncontradoException("Sucursal no encontrada: " + idSucursal));

        EquipoEntidad equipo = equipoRepositorioJpa.findByNombreEquipo(datosRegistro.nombreEquipo())
            .orElseGet(() -> new EquipoEntidad(sucursal, datosRegistro.nombreEquipo(), datosRegistro.rutaPos()));

        equipo.actualizarRegistro(
            sucursal,
            datosRegistro.direccionIp(),
            datosRegistro.direccionMac(),
            datosRegistro.versionWindows(),
            datosRegistro.versionAgente(),
            datosRegistro.versionPos(),
            datosRegistro.rutaPos()
        );

        return aDominio(equipoRepositorioJpa.save(equipo));
    }

    @Override
    public Optional<Equipo> buscarPorId(UUID idEquipo) {
        return equipoRepositorioJpa.findById(idEquipo).map(this::aDominio);
    }

    @Override
    public List<Equipo> listar() {
        return equipoRepositorioJpa.findAll()
            .stream()
            .sorted(Comparator.comparing(EquipoEntidad::getNombreEquipo))
            .map(this::aDominio)
            .toList();
    }

    @Override
    public void registrarLatido(UUID idEquipo, String versionPos) {
        EquipoEntidad equipo = equipoRepositorioJpa.findById(idEquipo)
            .orElseThrow(() -> new RecursoNoEncontradoException("Equipo no encontrado: " + idEquipo));
        equipo.registrarLatido(versionPos);
    }

    @Override
    public void actualizarVersionPos(UUID idEquipo, String versionPos) {
        EquipoEntidad equipo = equipoRepositorioJpa.findById(idEquipo)
            .orElseThrow(() -> new RecursoNoEncontradoException("Equipo no encontrado: " + idEquipo));
        equipo.actualizarVersionPos(versionPos);
    }

    private Equipo aDominio(EquipoEntidad entidad) {
        SucursalEntidad sucursal = entidad.getSucursal();
        return new Equipo(
            entidad.getId(),
            sucursal.getId(),
            sucursal.getCodigo(),
            sucursal.getNombre(),
            entidad.getNombreEquipo(),
            entidad.getDireccionIp(),
            entidad.getDireccionMac(),
            entidad.getVersionWindows(),
            entidad.getVersionAgente(),
            entidad.getVersionPos(),
            entidad.getRutaPos(),
            entidad.getEstado(),
            entidad.getUltimoLatidoEn(),
            entidad.getRegistradoEn(),
            entidad.getActualizadoEn()
        );
    }
}
