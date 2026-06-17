package com.farmamia.posupdate.presentacion.controlador;

import com.farmamia.posupdate.aplicacion.casouso.ConsultarDetalleEquipoCasoUso;
import com.farmamia.posupdate.aplicacion.casouso.ConsultarCatalogoOperativoCasoUso;
import com.farmamia.posupdate.dominio.modelo.DetalleEquipo;
import com.farmamia.posupdate.dominio.modelo.Equipo;
import com.farmamia.posupdate.dominio.modelo.EventoActualizacionRegistrado;
import com.farmamia.posupdate.dominio.modelo.FiltroEquipos;
import com.farmamia.posupdate.dominio.modelo.MetricaEquipoRegistrada;
import com.farmamia.posupdate.dominio.modelo.ObjetivoDespliegueEquipo;
import com.farmamia.posupdate.dominio.modelo.Pagina;
import com.farmamia.posupdate.presentacion.dto.RespuestaDetalleEquipo;
import com.farmamia.posupdate.presentacion.dto.RespuestaEquipo;
import com.farmamia.posupdate.presentacion.dto.RespuestaEventoActualizacion;
import com.farmamia.posupdate.presentacion.dto.RespuestaMetricaEquipo;
import com.farmamia.posupdate.presentacion.dto.RespuestaObjetivoEquipo;
import com.farmamia.posupdate.presentacion.dto.RespuestaPagina;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/devices", "/api/equipos-pos"})
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

    @GetMapping("/page")
    public RespuestaPagina<RespuestaEquipo> listarPaginado(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String branchCode,
        @RequestParam(required = false) String posVersion,
        @RequestParam(required = false) String agentVersion,
        @RequestParam(required = false) OffsetDateTime lastHeartbeatFrom,
        @RequestParam(required = false) OffsetDateTime lastHeartbeatTo,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size,
        @RequestParam(defaultValue = "nombreEquipo,asc") String sort
    ) {
        Pagina<Equipo> pagina = consultarCatalogoOperativoCasoUso.listarEquiposPaginado(new FiltroEquipos(
            q,
            status,
            branchCode,
            posVersion,
            agentVersion,
            lastHeartbeatFrom,
            lastHeartbeatTo,
            page,
            size,
            sort
        ));
        return aRespuestaPagina(pagina);
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

    private RespuestaPagina<RespuestaEquipo> aRespuestaPagina(Pagina<Equipo> pagina) {
        return new RespuestaPagina<>(
            pagina.contenido().stream().map(this::aRespuesta).toList(),
            pagina.pagina(),
            pagina.tamano(),
            pagina.totalElementos(),
            pagina.totalPaginas(),
            pagina.tieneSiguiente()
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
