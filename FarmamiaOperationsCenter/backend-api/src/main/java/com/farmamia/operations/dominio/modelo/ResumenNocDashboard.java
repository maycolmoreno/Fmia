package com.farmamia.operations.dominio.modelo;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ResumenNocDashboard(
    List<FarmaciaCriticaNoc> farmaciasCriticas,
    List<FarmaciaCriticaNoc> farmaciasDeTurnoEnRiesgo,
    EstadoRedNoc red,
    EstadoPosNoc pos,
    CampanaActivaNoc campanaActiva,
    List<AlertaResumenNoc> alertasRecientes,
    OffsetDateTime generadoEn
) {

    public record FarmaciaCriticaNoc(
        UUID id,
        String codigo,
        String nombre,
        boolean deTurno,
        String estadoOperacional,
        boolean critica,
        boolean turnoEnRiesgo,
        int alertasCriticas,
        String resumenRiesgo
    ) {}

    public record EstadoRedNoc(
        long enlacesCaidos,
        long latenciaAlta,
        long vpnCaidas
    ) {}

    public record EstadoPosNoc(
        long totalPos,
        long posOnline,
        long posOffline,
        long posEnRiesgo,
        String versionActual
    ) {}

    public record CampanaActivaNoc(
        UUID id,
        String nombre,
        String versionPos,
        int progresoPorcentaje,
        long totalEquipos,
        long completados,
        long fallidos
    ) {}

    public record AlertaResumenNoc(
        UUID id,
        UUID idFarmacia,
        String codigoFarmacia,
        String severidad,
        String tipoAlerta,
        String titulo,
        String estado,
        OffsetDateTime abiertaEn,
        boolean eventoDeRed
    ) {}
}
