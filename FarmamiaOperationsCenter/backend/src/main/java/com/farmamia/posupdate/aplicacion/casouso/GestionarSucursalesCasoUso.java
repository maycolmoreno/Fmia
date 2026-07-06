package com.farmamia.posupdate.aplicacion.casouso;

import com.farmamia.posupdate.dominio.modelo.Sucursal;
import com.farmamia.posupdate.dominio.puerto.RepositorioSucursales;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GestionarSucursalesCasoUso {

    private final RepositorioSucursales repositorioSucursales;

    public GestionarSucursalesCasoUso(RepositorioSucursales repositorioSucursales) {
        this.repositorioSucursales = repositorioSucursales;
    }

    @Transactional
    public Sucursal actualizarCoordenadas(UUID id, Double latitud, Double longitud) {
        return repositorioSucursales.actualizarCoordenadas(id, latitud, longitud);
    }
}
