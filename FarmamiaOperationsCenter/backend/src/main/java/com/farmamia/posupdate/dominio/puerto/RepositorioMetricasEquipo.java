package com.farmamia.posupdate.dominio.puerto;

import com.farmamia.posupdate.dominio.modelo.MetricaEquipo;
import com.farmamia.posupdate.dominio.modelo.MetricaEquipoRegistrada;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface RepositorioMetricasEquipo {

    void guardar(MetricaEquipo metrica);

    Optional<MetricaEquipoRegistrada> buscarUltimaPorEquipo(UUID idEquipo);

    List<MetricaEquipoRegistrada> listarUltimasPorEquipo(UUID idEquipo, int limite);
}
