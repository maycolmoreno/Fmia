package com.farmamia.posupdate.aplicacion.casouso;

import com.farmamia.posupdate.aplicacion.excepcion.RecursoNoEncontradoException;
import com.farmamia.posupdate.dominio.modelo.EstadoOperacionalFarmacia;
import com.farmamia.posupdate.dominio.puerto.RepositorioEstadoFarmacias;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultarEstadoFarmaciasCasoUso {

    private final RepositorioEstadoFarmacias repositorioEstadoFarmacias;

    public ConsultarEstadoFarmaciasCasoUso(RepositorioEstadoFarmacias repositorioEstadoFarmacias) {
        this.repositorioEstadoFarmacias = repositorioEstadoFarmacias;
    }

    @Transactional(readOnly = true)
    public List<EstadoOperacionalFarmacia> listar() {
        return repositorioEstadoFarmacias.listar()
            .stream()
            .sorted(Comparator
                .comparing(EstadoOperacionalFarmacia::critica, Comparator.reverseOrder())
                .thenComparing(EstadoOperacionalFarmacia::turnoEnRiesgo, Comparator.reverseOrder())
                .thenComparing(EstadoOperacionalFarmacia::codigoFarmacia))
            .toList();
    }

    @Transactional(readOnly = true)
    public EstadoOperacionalFarmacia obtener(UUID idFarmacia) {
        return repositorioEstadoFarmacias.buscarPorId(idFarmacia)
            .orElseThrow(() -> new RecursoNoEncontradoException("Farmacia no encontrada: " + idFarmacia));
    }
}
