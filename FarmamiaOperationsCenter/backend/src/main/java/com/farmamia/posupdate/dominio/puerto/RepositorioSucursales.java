package com.farmamia.posupdate.dominio.puerto;

import com.farmamia.posupdate.dominio.modelo.FiltroSucursales;
import com.farmamia.posupdate.dominio.modelo.CatalogoRegion;
import com.farmamia.posupdate.dominio.modelo.Pagina;
import com.farmamia.posupdate.dominio.modelo.Sucursal;
import com.farmamia.posupdate.dominio.modelo.SucursalSugerida;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface RepositorioSucursales {

    UUID obtenerOCrearPorCodigo(String codigoSucursal);

    Optional<Sucursal> buscarPorCodigo(String codigo);

    Optional<SucursalSugerida> buscarSugeridaPorCodigo(String codigo);

    long contarPorIds(Set<UUID> idsSucursales);

    List<Sucursal> listar();

    List<CatalogoRegion> listarCatalogoRegiones();

    Pagina<Sucursal> listarPaginado(FiltroSucursales filtro);
}
