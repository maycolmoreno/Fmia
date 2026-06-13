package com.farmamia.operations.dominio.modelo;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EstadoOperacionalFarmacia(
    UUID idFarmacia,
    String codigoFarmacia,
    String nombreFarmacia,
    String ciudad,
    String zona,
    boolean deTurno,
    boolean activa,
    String estadoOperacional,
    boolean critica,
    boolean turnoEnRiesgo,
    int totalEquiposPos,
    int equiposOnline,
    int equiposOffline,
    int equiposSinLatido,
    OffsetDateTime ultimoLatidoEn,
    int alertasAbiertas,
    int alertasCriticas,
    int campanasActivas,
    int objetivosCampanaPendientes,
    int objetivosCampanaFallidos,
    String campanaActivaPrincipal,
    String grupoTrxPrincipal,
    String versionPosDominante,
    String resumenRiesgo
) {
}
