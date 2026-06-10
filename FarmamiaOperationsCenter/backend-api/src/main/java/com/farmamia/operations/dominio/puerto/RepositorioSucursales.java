package com.farmamia.operations.dominio.puerto;

import com.farmamia.operations.dominio.modelo.Sucursal;
import java.util.List;
import java.util.UUID;

public interface RepositorioSucursales {

    UUID obtenerOCrearPorCodigo(String codigoSucursal);

    List<Sucursal> listar();
}
