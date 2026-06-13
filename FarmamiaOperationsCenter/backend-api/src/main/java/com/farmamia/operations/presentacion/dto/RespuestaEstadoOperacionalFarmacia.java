package com.farmamia.operations.presentacion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RespuestaEstadoOperacionalFarmacia(
    @JsonProperty("farmaciaId") UUID idFarmacia,
    @JsonProperty("codigoFarmacia") String codigoFarmacia,
    @JsonProperty("nombreFarmacia") String nombreFarmacia,
    @JsonProperty("ciudad") String ciudad,
    @JsonProperty("zona") String zona,
    @JsonProperty("deTurno") boolean deTurno,
    @JsonProperty("activa") boolean activa,
    @JsonProperty("estadoOperacional") String estadoOperacional,
    @JsonProperty("critica") boolean critica,
    @JsonProperty("turnoEnRiesgo") boolean turnoEnRiesgo,
    @JsonProperty("totalEquiposPos") int totalEquiposPos,
    @JsonProperty("equiposOnline") int equiposOnline,
    @JsonProperty("equiposOffline") int equiposOffline,
    @JsonProperty("equiposSinLatido") int equiposSinLatido,
    @JsonProperty("ultimoLatidoEn") OffsetDateTime ultimoLatidoEn,
    @JsonProperty("alertasAbiertas") int alertasAbiertas,
    @JsonProperty("alertasCriticas") int alertasCriticas,
    @JsonProperty("campanasActivas") int campanasActivas,
    @JsonProperty("objetivosCampanaPendientes") int objetivosCampanaPendientes,
    @JsonProperty("objetivosCampanaFallidos") int objetivosCampanaFallidos,
    @JsonProperty("campanaActivaPrincipal") String campanaActivaPrincipal,
    @JsonProperty("grupoTrxPrincipal") String grupoTrxPrincipal,
    @JsonProperty("versionPosDominante") String versionPosDominante,
    @JsonProperty("resumenRiesgo") String resumenRiesgo
) {
}
