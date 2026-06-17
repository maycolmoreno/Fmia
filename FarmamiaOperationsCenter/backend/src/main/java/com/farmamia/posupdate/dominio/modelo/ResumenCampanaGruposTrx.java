package com.farmamia.posupdate.dominio.modelo;

import java.util.List;
import java.util.UUID;

public record ResumenCampanaGruposTrx(
    UUID campanaId,
    String nombreCampana,
    String versionPos,
    String estadoCampana,
    int totalGrupos,
    int gruposEnRiesgo,
    int gruposPausados,
    int farmaciasAfectadas,
    int farmaciasTurnoAfectadas,
    int farmaciasCriticas,
    List<CampanaGrupoTrx> grupos
) {
}
