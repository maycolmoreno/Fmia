package com.farmamia.operations.presentacion.controlador;

import com.farmamia.operations.aplicacion.casouso.ConsultarResumenDashboardCasoUso;
import com.farmamia.operations.dominio.modelo.ResumenDashboard;
import com.farmamia.operations.presentacion.dto.RespuestaResumenDashboard;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class ControladorDashboard {

    private final ConsultarResumenDashboardCasoUso consultarResumenDashboardCasoUso;

    public ControladorDashboard(ConsultarResumenDashboardCasoUso consultarResumenDashboardCasoUso) {
        this.consultarResumenDashboardCasoUso = consultarResumenDashboardCasoUso;
    }

    @GetMapping("/summary")
    public RespuestaResumenDashboard resumen() {
        return aRespuesta(consultarResumenDashboardCasoUso.obtener());
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
}
