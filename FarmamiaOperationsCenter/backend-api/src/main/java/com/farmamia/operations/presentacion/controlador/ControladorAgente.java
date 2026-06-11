package com.farmamia.operations.presentacion.controlador;

import com.farmamia.operations.aplicacion.casouso.ConsultarInstruccionesAgenteCasoUso;
import com.farmamia.operations.aplicacion.casouso.RegistrarAgenteCasoUso;
import com.farmamia.operations.aplicacion.casouso.RegistrarEventoAgenteCasoUso;
import com.farmamia.operations.aplicacion.casouso.RegistrarLatidoCasoUso;
import com.farmamia.operations.dominio.modelo.DatosEventoAgente;
import com.farmamia.operations.dominio.modelo.DatosLatido;
import com.farmamia.operations.dominio.modelo.DatosRegistroAgente;
import com.farmamia.operations.dominio.modelo.InstruccionAgente;
import com.farmamia.operations.dominio.modelo.RegistroAgente;
import com.farmamia.operations.dominio.modelo.ResultadoActualizacion;
import com.farmamia.operations.presentacion.dto.RespuestaInstruccionAgente;
import com.farmamia.operations.presentacion.dto.RespuestaRegistroAgente;
import com.farmamia.operations.presentacion.dto.SolicitudEventoAgente;
import com.farmamia.operations.presentacion.dto.SolicitudLatido;
import com.farmamia.operations.presentacion.dto.SolicitudRegistroAgente;
import com.farmamia.operations.presentacion.dto.SolicitudResultadoActualizacion;
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

    public ControladorAgente(
        RegistrarAgenteCasoUso registrarAgenteCasoUso,
        RegistrarLatidoCasoUso registrarLatidoCasoUso,
        ConsultarInstruccionesAgenteCasoUso consultarInstruccionesAgenteCasoUso,
        RegistrarEventoAgenteCasoUso registrarEventoAgenteCasoUso
    ) {
        this.registrarAgenteCasoUso = registrarAgenteCasoUso;
        this.registrarLatidoCasoUso = registrarLatidoCasoUso;
        this.consultarInstruccionesAgenteCasoUso = consultarInstruccionesAgenteCasoUso;
        this.registrarEventoAgenteCasoUso = registrarEventoAgenteCasoUso;
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
            instruccion.horaOficialActualizacion(),
            instruccion.horaForzadaActualizacion(),
            instruccion.avisos()
        );
    }
}
