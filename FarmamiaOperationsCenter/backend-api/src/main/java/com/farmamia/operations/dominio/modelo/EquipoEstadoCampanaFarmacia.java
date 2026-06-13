package com.farmamia.operations.dominio.modelo;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EquipoEstadoCampanaFarmacia(
    UUID equipoId,
    String nombreEquipo,
    String estadoEquipo,
    String estadoObjetivo,
    String codigoGrupoTrx,
    String versionAnterior,
    String versionNueva,
    String ultimoError,
    OffsetDateTime ultimoHeartbeatEn,
    boolean rollback
) {
}
