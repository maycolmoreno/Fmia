package com.farmamia.operations.infraestructura.persistencia.adaptador;

import com.farmamia.operations.dominio.modelo.Sucursal;
import com.farmamia.operations.dominio.puerto.RepositorioSucursales;
import com.farmamia.operations.infraestructura.persistencia.entidad.SucursalEntidad;
import com.farmamia.operations.infraestructura.persistencia.repositorio.SucursalRepositorioJpa;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioSucursalesJpaAdaptador implements RepositorioSucursales {

    private final SucursalRepositorioJpa sucursalRepositorioJpa;

    public RepositorioSucursalesJpaAdaptador(SucursalRepositorioJpa sucursalRepositorioJpa) {
        this.sucursalRepositorioJpa = sucursalRepositorioJpa;
    }

    @Override
    public UUID obtenerOCrearPorCodigo(String codigoSucursal) {
        return sucursalRepositorioJpa.findByCodigo(codigoSucursal)
            .orElseGet(() -> sucursalRepositorioJpa.save(new SucursalEntidad(codigoSucursal, codigoSucursal)))
            .getId();
    }

    @Override
    public List<Sucursal> listar() {
        return sucursalRepositorioJpa.findAll()
            .stream()
            .sorted(Comparator.comparing(SucursalEntidad::getCodigo))
            .map(this::aDominio)
            .toList();
    }

    private Sucursal aDominio(SucursalEntidad entidad) {
        return new Sucursal(
            entidad.getId(),
            entidad.getCodigo(),
            entidad.getNombre(),
            entidad.getCiudad(),
            entidad.getZona(),
            entidad.getDireccion(),
            entidad.isDeTurno(),
            entidad.isActiva(),
            entidad.getCreadoEn(),
            entidad.getActualizadoEn()
        );
    }
}
