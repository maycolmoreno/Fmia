package com.farmamia.operations.aplicacion.casouso;

import com.farmamia.operations.dominio.modelo.DatosCrearDespliegue;
import com.farmamia.operations.dominio.modelo.Despliegue;
import com.farmamia.operations.dominio.modelo.EstadoDespliegue;
import com.farmamia.operations.dominio.puerto.RepositorioDespliegues;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GestionarDesplieguesCasoUso {

    private final RepositorioDespliegues repositorioDespliegues;

    public GestionarDesplieguesCasoUso(RepositorioDespliegues repositorioDespliegues) {
        this.repositorioDespliegues = repositorioDespliegues;
    }

    @Transactional
    public Despliegue crear(DatosCrearDespliegue datos) {
        return repositorioDespliegues.crear(datos);
    }

    public List<Despliegue> listar() {
        return repositorioDespliegues.listar();
    }

    public Despliegue obtener(UUID id) {
        return repositorioDespliegues.obtener(id);
    }

    @Transactional
    public Despliegue programar(UUID id, OffsetDateTime programadoEn) {
        return repositorioDespliegues.programar(id, programadoEn);
    }

    @Transactional
    public Despliegue pausar(UUID id) {
        return repositorioDespliegues.pausar(id);
    }

    @Transactional
    public Despliegue reanudar(UUID id) {
        return repositorioDespliegues.reanudar(id);
    }

    @Transactional
    public Despliegue cancelar(UUID id) {
        return repositorioDespliegues.cancelar(id);
    }

    public EstadoDespliegue estado(UUID id) {
        return repositorioDespliegues.estado(id);
    }
}
