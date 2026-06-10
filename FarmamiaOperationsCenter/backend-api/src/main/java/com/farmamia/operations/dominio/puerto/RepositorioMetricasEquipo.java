package com.farmamia.operations.dominio.puerto;

import com.farmamia.operations.dominio.modelo.MetricaEquipo;
import com.farmamia.operations.dominio.modelo.MetricaEquipoRegistrada;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioMetricasEquipo {

    void guardar(MetricaEquipo metrica);

    Optional<MetricaEquipoRegistrada> buscarUltimaPorEquipo(UUID idEquipo);
}
