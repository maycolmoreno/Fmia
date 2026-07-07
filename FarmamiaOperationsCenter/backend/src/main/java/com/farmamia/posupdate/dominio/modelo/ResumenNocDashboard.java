package com.farmamia.posupdate.dominio.modelo;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ResumenNocDashboard(
    List<FarmaciaCriticaNoc> farmaciasCriticas,
    List<FarmaciaCriticaNoc> farmaciasDeTurnoEnRiesgo,
    EstadoRedNoc red,
    List<EnlaceCaidoNoc> enlacesCaidosDetalle,
    EstadoPosNoc pos,
    List<EquipoSinActualizarNoc> equiposSinActualizar,
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
        long vpnCaidas,
        long totalEnlaces,
        long enlacesUp
    ) {}

    public record EnlaceCaidoNoc(
        String codigoPdv,
        String codigoSucursal,
        String nombreSucursal,
        String direccionIp,
        OffsetDateTime ultimoLatidoEn
    ) {}

    public record EstadoPosNoc(
        long totalPos,
        long posOnline,
        long posOffline,
        long posEnRiesgo,
        String versionActual
    ) {}

    public record EquipoSinActualizarNoc(
        String nombreEquipo,
        String codigoSucursal,
        String estadoObjetivo,
        UUID idEquipo,
        String versionNueva,
        OffsetDateTime actualizadoEn
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
        String codigoPdv,
        String severidad,
        String tipoAlerta,
        String titulo,
        String estado,
        OffsetDateTime abiertaEn,
        boolean eventoDeRed
    ) {}
}
