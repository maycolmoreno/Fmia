package com.farmamia.posupdate.presentacion.controlador;

import com.farmamia.posupdate.aplicacion.casouso.ConsultarInstruccionesAgenteCasoUso;
import com.farmamia.posupdate.aplicacion.casouso.RegistrarAgenteCasoUso;
import com.farmamia.posupdate.aplicacion.casouso.RegistrarEventoAgenteCasoUso;
import com.farmamia.posupdate.aplicacion.casouso.RegistrarLatidoCasoUso;
import com.farmamia.posupdate.dominio.modelo.DatosEventoAgente;
import com.farmamia.posupdate.dominio.modelo.DatosLatido;
import com.farmamia.posupdate.dominio.modelo.DatosRegistroAgente;
import com.farmamia.posupdate.dominio.modelo.InstruccionAgente;
import com.farmamia.posupdate.dominio.modelo.RegistroAgente;
import com.farmamia.posupdate.dominio.modelo.ResultadoActualizacion;
import com.farmamia.posupdate.infraestructura.observabilidad.MetricasOperativasFarmamia;
import com.farmamia.posupdate.presentacion.dto.RespuestaInstruccionAgente;
import com.farmamia.posupdate.presentacion.dto.RespuestaRegistroAgente;
import com.farmamia.posupdate.presentacion.dto.SolicitudEventoAgente;
import com.farmamia.posupdate.presentacion.dto.SolicitudLatido;
import com.farmamia.posupdate.presentacion.dto.SolicitudRegistroAgente;
import com.farmamia.posupdate.presentacion.dto.SolicitudResultadoActualizacion;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent")
public class ControladorAgente {

    private final RegistrarAgenteCasoUso registrarAgenteCasoUso;
    private final RegistrarLatidoCasoUso registrarLatidoCasoUso;
    private final ConsultarInstruccionesAgenteCasoUso consultarInstruccionesAgenteCasoUso;
    private final RegistrarEventoAgenteCasoUso registrarEventoAgenteCasoUso;
    private final MetricasOperativasFarmamia metricasOperativas;

    public ControladorAgente(
        RegistrarAgenteCasoUso registrarAgenteCasoUso,
        RegistrarLatidoCasoUso registrarLatidoCasoUso,
        ConsultarInstruccionesAgenteCasoUso consultarInstruccionesAgenteCasoUso,
        RegistrarEventoAgenteCasoUso registrarEventoAgenteCasoUso,
        MetricasOperativasFarmamia metricasOperativas
    ) {
        this.registrarAgenteCasoUso = registrarAgenteCasoUso;
        this.registrarLatidoCasoUso = registrarLatidoCasoUso;
        this.consultarInstruccionesAgenteCasoUso = consultarInstruccionesAgenteCasoUso;
        this.registrarEventoAgenteCasoUso = registrarEventoAgenteCasoUso;
        this.metricasOperativas = metricasOperativas;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RespuestaRegistroAgente registrar(@Valid @RequestBody SolicitudRegistroAgente solicitud) {
        RegistroAgente registro = registrarAgenteCasoUso.registrar(new DatosRegistroAgente(
            solicitud.codigoSucursal(),
            solicitud.nombreEquipo(),
            solicitud.direccionIp(),
            solicitud.direccionMac(),
            solicitud.versionWindows(),
            solicitud.versionAgente(),
            solicitud.versionPos(),
            solicitud.rutaPos()
        ));

        return new RespuestaRegistroAgente(
            registro.idEquipo(),
            registro.tokenAgente(),
            registro.horaServidor()
        );
    }

    @PostMapping("/heartbeat")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void registrarLatido(@Valid @RequestBody SolicitudLatido solicitud) {
        registrarLatidoCasoUso.registrar(new DatosLatido(
            solicitud.idEquipo(),
            solicitud.versionPos(),
            solicitud.discoLibreMb(),
            solicitud.discoTotalMb(),
            solicitud.procesoPosEjecutandose(),
            solicitud.latenciaMs(),
            aBigDecimal(solicitud.porcentajePerdidaPaquetes())
        ));
        metricasOperativas.registrarHeartbeat();
    }

    @GetMapping("/{idEquipo}/instructions")
    public RespuestaInstruccionAgente consultarInstrucciones(@PathVariable UUID idEquipo) {
        return aRespuestaInstruccion(consultarInstruccionesAgenteCasoUso.buscarSiguienteInstruccion(idEquipo));
    }

    @PostMapping("/{idEquipo}/events")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void registrarEvento(@PathVariable UUID idEquipo, @Valid @RequestBody SolicitudEventoAgente solicitud) {
        registrarEventoAgenteCasoUso.registrarEvento(idEquipo, new DatosEventoAgente(
            solicitud.idObjetivoDespliegue(),
            solicitud.idempotencyKey(),
            solicitud.tipoEvento(),
            solicitud.mensajeEvento(),
            solicitud.versionAnterior(),
            solicitud.versionNueva(),
            solicitud.metadatos()
        ));
        metricasOperativas.registrarEventoAgente(solicitud.tipoEvento());
    }

    @PostMapping("/{idEquipo}/update-result")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void registrarResultado(
        @PathVariable UUID idEquipo,
        @Valid @RequestBody SolicitudResultadoActualizacion solicitud
    ) {
        registrarEventoAgenteCasoUso.registrarResultadoActualizacion(idEquipo, new ResultadoActualizacion(
            solicitud.idObjetivoDespliegue(),
            solicitud.idempotencyKey(),
            solicitud.estado(),
            solicitud.versionAnterior(),
            solicitud.versionNueva(),
            solicitud.mensaje()
        ));
        metricasOperativas.registrarResultadoActualizacion(solicitud.estado());
    }

    private BigDecimal aBigDecimal(Double valor) {
        return valor == null ? null : BigDecimal.valueOf(valor);
    }

    private RespuestaInstruccionAgente aRespuestaInstruccion(InstruccionAgente instruccion) {
        return new RespuestaInstruccionAgente(
            instruccion.tieneInstruccion(),
            instruccion.tipoInstruccion(),
            instruccion.idObjetivoDespliegue(),
            instruccion.idPaquete(),
            instruccion.version(),
            instruccion.urlDescarga(),
            instruccion.checksumSha256(),
            instruccion.firma(),
            instruccion.algoritmoFirma(),
            instruccion.idClaveFirma(),
            instruccion.clavePublicaFirmaPem(),
            instruccion.horaOficialActualizacion(),
            instruccion.horaForzadaActualizacion(),
            instruccion.avisos()
        );
    }
}
