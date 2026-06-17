package com.farmamia.posupdate.presentacion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record RespuestaResumenNocDashboard(
    @JsonProperty("criticFarms") List<FarmaciaCriticaNocDto> farmaciasCriticas,
    @JsonProperty("atRiskFarms") List<FarmaciaCriticaNocDto> farmaciasDeTurnoEnRiesgo,
    @JsonProperty("network") EstadoRedNocDto red,
    @JsonProperty("pos") EstadoPosNocDto pos,
    @JsonProperty("activeCampaign") CampanaActivaNocDto campanaActiva,
    @JsonProperty("recentAlerts") List<AlertaResumenNocDto> alertasRecientes,
    @JsonProperty("generatedAt") OffsetDateTime generadoEn
) {

    public record FarmaciaCriticaNocDto(
        @JsonProperty("id") UUID id,
        @JsonProperty("code") String codigo,
        @JsonProperty("name") String nombre,
        @JsonProperty("onDuty") boolean deTurno,
        @JsonProperty("operationalStatus") String estadoOperacional,
        @JsonProperty("critical") boolean critica,
        @JsonProperty("dutyAtRisk") boolean turnoEnRiesgo,
        @JsonProperty("criticalAlerts") int alertasCriticas,
        @JsonProperty("riskSummary") String resumenRiesgo
    ) {}

    public record EstadoRedNocDto(
        @JsonProperty("linkDown") long enlacesCaidos,
        @JsonProperty("highLatency") long latenciaAlta,
        @JsonProperty("vpnDown") long vpnCaidas
    ) {}

    public record EstadoPosNocDto(
        @JsonProperty("total") long totalPos,
        @JsonProperty("online") long posOnline,
        @JsonProperty("offline") long posOffline,
        @JsonProperty("atRisk") long posEnRiesgo,
        @JsonProperty("currentVersion") String versionActual
    ) {}

    public record CampanaActivaNocDto(
        @JsonProperty("id") UUID id,
        @JsonProperty("name") String nombre,
        @JsonProperty("posVersion") String versionPos,
        @JsonProperty("progressPercent") int progresoPorcentaje,
        @JsonProperty("totalDevices") long totalEquipos,
        @JsonProperty("completed") long completados,
        @JsonProperty("failed") long fallidos
    ) {}

    public record AlertaResumenNocDto(
        @JsonProperty("id") UUID id,
        @JsonProperty("farmId") UUID idFarmacia,
        @JsonProperty("farmCode") String codigoFarmacia,
        @JsonProperty("severity") String severidad,
        @JsonProperty("alertType") String tipoAlerta,
        @JsonProperty("title") String titulo,
        @JsonProperty("status") String estado,
        @JsonProperty("openedAt") OffsetDateTime abiertaEn,
        @JsonProperty("networkEvent") boolean eventoDeRed
    ) {}
}
