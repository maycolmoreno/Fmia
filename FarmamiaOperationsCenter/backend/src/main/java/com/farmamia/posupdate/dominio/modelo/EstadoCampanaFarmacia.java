package com.farmamia.posupdate.dominio.modelo;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record EstadoCampanaFarmacia(
    UUID farmaciaId,
    String codigoFarmacia,
    String nombreFarmacia,
    UUID campanaId,
    UUID grupoTrxId,
    String codigoGrupoTrx,
    boolean deTurno,
    int totalEquiposPos,
    int completados,
    int pendientes,
    int fallidos,
    int rollbacks,
    OffsetDateTime ultimoHeartbeatRelacionado,
    int alertasCriticas,
    int alertasAbiertas,
    EstadoTecnicoCampanaFarmacia estadoTecnico,
    EstadoOperacionalCampanaFarmacia estadoOperacional,
    String resumenRiesgo,
    List<EquipoEstadoCampanaFarmacia> equipos
) {
}
