package com.farmamia.operations.presentacion.controlador;

import com.farmamia.operations.aplicacion.casouso.ConsultarResumenDashboardCasoUso;
import com.farmamia.operations.aplicacion.casouso.ConsultarResumenNocCasoUso;
import com.farmamia.operations.dominio.modelo.ResumenDashboard;
import com.farmamia.operations.dominio.modelo.ResumenNocDashboard;
import com.farmamia.operations.presentacion.dto.RespuestaResumenDashboard;
import com.farmamia.operations.presentacion.dto.RespuestaResumenNocDashboard;
import com.farmamia.operations.presentacion.dto.RespuestaResumenNocDashboard.AlertaResumenNocDto;
import com.farmamia.operations.presentacion.dto.RespuestaResumenNocDashboard.CampanaActivaNocDto;
import com.farmamia.operations.presentacion.dto.RespuestaResumenNocDashboard.EstadoPosNocDto;
import com.farmamia.operations.presentacion.dto.RespuestaResumenNocDashboard.EstadoRedNocDto;
import com.farmamia.operations.presentacion.dto.RespuestaResumenNocDashboard.FarmaciaCriticaNocDto;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class ControladorDashboard {

    private final ConsultarResumenDashboardCasoUso consultarResumenDashboardCasoUso;
    private final ConsultarResumenNocCasoUso consultarResumenNocCasoUso;

    public ControladorDashboard(
        ConsultarResumenDashboardCasoUso consultarResumenDashboardCasoUso,
        ConsultarResumenNocCasoUso consultarResumenNocCasoUso
    ) {
        this.consultarResumenDashboardCasoUso = consultarResumenDashboardCasoUso;
        this.consultarResumenNocCasoUso = consultarResumenNocCasoUso;
    }

    @GetMapping("/summary")
    public RespuestaResumenDashboard resumen() {
        return aRespuesta(consultarResumenDashboardCasoUso.obtener());
    }

    @GetMapping("/resumen-noc")
    public RespuestaResumenNocDashboard resumenNoc() {
        return aRespuestaNoc(consultarResumenNocCasoUso.obtener());
    }

    private RespuestaResumenDashboard aRespuesta(ResumenDashboard resumen) {
        return new RespuestaResumenDashboard(
            resumen.totalEquipos(),
            resumen.equiposOnline(),
            resumen.totalPaquetes(),
            resumen.paquetesAprobados(),
            resumen.totalDespliegues(),
            resumen.desplieguesActivos(),
            resumen.totalEventos(),
            resumen.eventosCriticos(),
            resumen.totalAlertas(),
            resumen.alertasAbiertas(),
            resumen.alertasCriticas()
        );
    }

    private RespuestaResumenNocDashboard aRespuestaNoc(ResumenNocDashboard noc) {
        List<FarmaciaCriticaNocDto> criticas = noc.farmaciasCriticas().stream()
            .map(this::aFarmaciaNocDto)
            .toList();
        List<FarmaciaCriticaNocDto> enRiesgo = noc.farmaciasDeTurnoEnRiesgo().stream()
            .map(this::aFarmaciaNocDto)
            .toList();
        EstadoRedNocDto red = new EstadoRedNocDto(
            noc.red().enlacesCaidos(),
            noc.red().latenciaAlta(),
            noc.red().vpnCaidas()
        );
        EstadoPosNocDto pos = new EstadoPosNocDto(
            noc.pos().totalPos(),
            noc.pos().posOnline(),
            noc.pos().posOffline(),
            noc.pos().posEnRiesgo(),
            noc.pos().versionActual()
        );
        CampanaActivaNocDto campana = noc.campanaActiva() == null ? null : new CampanaActivaNocDto(
            noc.campanaActiva().id(),
            noc.campanaActiva().nombre(),
            noc.campanaActiva().versionPos(),
            noc.campanaActiva().progresoPorcentaje(),
            noc.campanaActiva().totalEquipos(),
            noc.campanaActiva().completados(),
            noc.campanaActiva().fallidos()
        );
        List<AlertaResumenNocDto> alertas = noc.alertasRecientes().stream()
            .map(a -> new AlertaResumenNocDto(
                a.id(), a.idFarmacia(), a.codigoFarmacia(),
                a.severidad(), a.tipoAlerta(), a.titulo(),
                a.estado(), a.abiertaEn(), a.eventoDeRed()
            ))
            .toList();
        return new RespuestaResumenNocDashboard(criticas, enRiesgo, red, pos, campana, alertas, noc.generadoEn());
    }

    private FarmaciaCriticaNocDto aFarmaciaNocDto(ResumenNocDashboard.FarmaciaCriticaNoc f) {
        return new FarmaciaCriticaNocDto(
            f.id(), f.codigo(), f.nombre(), f.deTurno(),
            f.estadoOperacional(), f.critica(), f.turnoEnRiesgo(),
            f.alertasCriticas(), f.resumenRiesgo()
        );
    }
}
