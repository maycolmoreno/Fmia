package com.farmamia.posupdate.dominio.modelo;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EquipoHuerfano(
    UUID id,
    String nombreEquipo,
    String direccionIp,
    String versionAgente,
    String versionPos,
    OffsetDateTime registradoEn,
    EstadoSugerenciaAprovisionamiento estadoSugerencia,
    UUID idSucursalSugerida,
    String codigoSucursalSugerida,
    String nombreSucursalSugerida,
    String codigoGrupoTrxSugerido
) {
}
