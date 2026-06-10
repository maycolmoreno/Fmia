package com.farmamia.operations.presentacion.controlador;

import com.farmamia.operations.aplicacion.casouso.ConsultarDetalleEquipoCasoUso;
import com.farmamia.operations.aplicacion.casouso.ConsultarCatalogoOperativoCasoUso;
import com.farmamia.operations.dominio.modelo.DetalleEquipo;
import com.farmamia.operations.dominio.modelo.Equipo;
import com.farmamia.operations.dominio.modelo.EventoActualizacionRegistrado;
import com.farmamia.operations.dominio.modelo.MetricaEquipoRegistrada;
import com.farmamia.operations.dominio.modelo.ObjetivoDespliegueEquipo;
import com.farmamia.operations.presentacion.dto.RespuestaDetalleEquipo;
import com.farmamia.operations.presentacion.dto.RespuestaEquipo;
import com.farmamia.operations.presentacion.dto.RespuestaEventoActualizacion;
import com.farmamia.operations.presentacion.dto.RespuestaMetricaEquipo;
import com.farmamia.operations.presentacion.dto.RespuestaObjetivoEquipo;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/devices")
public class ControladorEquipos {

    private final ConsultarCatalogoOperativoCasoUso consultarCatalogoOperativoCasoUso;
    private final ConsultarDetalleEquipoCasoUso consultarDetalleEquipoCasoUso;

    public ControladorEquipos(
        ConsultarCatalogoOperativoCasoUso consultarCatalogoOperativoCasoUso,
        ConsultarDetalleEquipoCasoUso consultarDetalleEquipoCasoUso
    ) {
        this.consultarCatalogoOperativoCasoUso = consultarCatalogoOperativoCasoUso;
        this.consultarDetalleEquipoCasoUso = consultarDetalleEquipoCasoUso;
    }

    @GetMapping
    public List<RespuestaEquipo> listar() {
        return consultarCatalogoOperativoCasoUso.listarEquipos()
            .stream()
            .map(this::aRespuesta)
            .toList();
    }

    @GetMapping("/{id}")
    public RespuestaDetalleEquipo obtenerDetalle(@PathVariable UUID id) {
        DetalleEquipo detalle = consultarDetalleEquipoCasoUso.consultar(id);
        return new RespuestaDetalleEquipo(
            aRespuesta(detalle.equipo()),
            aRespuestaMetrica(detalle.ultimaMetrica()),
            detalle.eventosRecientes().stream().map(this::aRespuestaEvento).toList(),
            detalle.despliegues().stream().map(this::aRespuestaObjetivo).toList()
        );
    }

    private RespuestaEquipo aRespuesta(Equipo equipo) {
        return new RespuestaEquipo(
            equipo.id(),
            equipo.idSucursal(),
            equipo.codigoSucursal(),
            equipo.nombreSucursal(),
            equipo.nombreEquipo(),
            equipo.direccionIp(),
            equipo.direccionMac(),
            equipo.versionWindows(),
            equipo.versionAgente(),
            equipo.versionPos(),
            equipo.rutaPos(),
            equipo.estado(),
            equipo.ultimoLatidoEn(),
            equipo.registradoEn(),
            equipo.actualizadoEn()
        );
    }

    private RespuestaMetricaEquipo aRespuestaMetrica(MetricaEquipoRegistrada metrica) {
        if (metrica == null) {
            return null;
        }

        return new RespuestaMetricaEquipo(
            metrica.id(),
            metrica.versionPos(),
            metrica.discoLibreMb(),
            metrica.discoTotalMb(),
            metrica.procesoPosEjecutandose(),
            metrica.latenciaMs(),
            metrica.porcentajePerdidaPaquetes(),
            metrica.estadoAgente(),
            metrica.recolectadoEn()
        );
    }

    private RespuestaEventoActualizacion aRespuestaEvento(EventoActualizacionRegistrado evento) {
        return new RespuestaEventoActualizacion(
            evento.id(),
            evento.idEquipo(),
            evento.nombreEquipo(),
            evento.idDespliegue(),
            evento.idObjetivoDespliegue(),
            evento.tipoEvento(),
            evento.mensajeEvento(),
            evento.versionAnterior(),
            evento.versionNueva(),
            evento.metadatos(),
            evento.creadoEn()
        );
    }

    private RespuestaObjetivoEquipo aRespuestaObjetivo(ObjetivoDespliegueEquipo objetivo) {
        return new RespuestaObjetivoEquipo(
            objetivo.idObjetivo(),
            objetivo.idDespliegue(),
            objetivo.nombreDespliegue(),
            objetivo.versionPaquete(),
            objetivo.estadoDespliegue(),
            objetivo.estadoObjetivo(),
            objetivo.grupoObjetivo(),
            objetivo.piloto(),
            objetivo.versionAnterior(),
            objetivo.versionNueva(),
            objetivo.ultimoError(),
            objetivo.autorizadoEn(),
            objetivo.iniciadoEn(),
            objetivo.completadoEn(),
            objetivo.actualizadoEn()
        );
    }
}
