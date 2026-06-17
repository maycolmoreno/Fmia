package com.farmamia.posupdate.dominio.modelo;

import java.util.List;

public record DetalleGrupoTrx(
    GrupoTrx grupo,
    List<EquipoGrupoTrx> equipos,
    List<String> codigosFarmacia
) {
}
