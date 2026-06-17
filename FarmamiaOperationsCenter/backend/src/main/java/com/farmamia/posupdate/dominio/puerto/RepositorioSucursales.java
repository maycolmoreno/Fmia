package com.farmamia.posupdate.dominio.puerto;

import com.farmamia.posupdate.dominio.modelo.FiltroSucursales;
import com.farmamia.posupdate.dominio.modelo.Pagina;
import com.farmamia.posupdate.dominio.modelo.Sucursal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioSucursales {

    UUID obtenerOCrearPorCodigo(String codigoSucursal);

    Optional<Sucursal> buscarPorCodigo(String codigo);

    List<Sucursal> listar();

    Pagina<Sucursal> listarPaginado(FiltroSucursales filtro);
}
