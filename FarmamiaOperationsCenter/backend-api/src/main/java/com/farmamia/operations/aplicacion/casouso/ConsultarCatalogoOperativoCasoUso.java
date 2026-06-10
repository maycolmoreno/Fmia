package com.farmamia.operations.aplicacion.casouso;

import com.farmamia.operations.dominio.modelo.Equipo;
import com.farmamia.operations.dominio.modelo.EventoActualizacionRegistrado;
import com.farmamia.operations.dominio.modelo.Sucursal;
import com.farmamia.operations.dominio.puerto.RepositorioEquipos;
import com.farmamia.operations.dominio.puerto.RepositorioEventosActualizacion;
import com.farmamia.operations.dominio.puerto.RepositorioSucursales;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultarCatalogoOperativoCasoUso {

    private final RepositorioEquipos repositorioEquipos;
    private final RepositorioSucursales repositorioSucursales;
    private final RepositorioEventosActualizacion repositorioEventosActualizacion;

    public ConsultarCatalogoOperativoCasoUso(
        RepositorioEquipos repositorioEquipos,
        RepositorioSucursales repositorioSucursales,
        RepositorioEventosActualizacion repositorioEventosActualizacion
    ) {
        this.repositorioEquipos = repositorioEquipos;
        this.repositorioSucursales = repositorioSucursales;
        this.repositorioEventosActualizacion = repositorioEventosActualizacion;
    }

    @Transactional(readOnly = true)
    public List<Equipo> listarEquipos() {
        return repositorioEquipos.listar();
    }

    @Transactional(readOnly = true)
    public List<Sucursal> listarSucursales() {
        return repositorioSucursales.listar();
    }

    @Transactional(readOnly = true)
    public List<EventoActualizacionRegistrado> listarEventosRecientes(int limite) {
        int limiteNormalizado = Math.max(1, Math.min(limite, 200));
        return repositorioEventosActualizacion.listarRecientes(limiteNormalizado);
    }
}
