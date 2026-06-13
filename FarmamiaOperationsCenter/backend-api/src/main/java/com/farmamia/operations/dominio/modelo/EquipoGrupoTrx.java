package com.farmamia.operations.dominio.modelo;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EquipoGrupoTrx(
    UUID idEquipo,
    String nombreEquipo,
    UUID idFarmacia,
    String codigoFarmacia,
    String nombreFarmacia,
    String versionPos,
    String estadoEquipo,
    OffsetDateTime ultimoLatidoEn,
    OffsetDateTime asignadoEn
) {
}
