package com.farmamia.operations.presentacion.controlador;

import com.farmamia.operations.aplicacion.casouso.ConsultarCatalogoOperativoCasoUso;
import com.farmamia.operations.dominio.modelo.EventoActualizacionRegistrado;
import com.farmamia.operations.dominio.modelo.FiltroEventosActualizacion;
import com.farmamia.operations.dominio.modelo.Pagina;
import com.farmamia.operations.presentacion.dto.RespuestaEventoActualizacion;
import com.farmamia.operations.presentacion.dto.RespuestaPagina;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/update-events", "/api/eventos-agente"})
public class ControladorEventosActualizacion {

    private final ConsultarCatalogoOperativoCasoUso consultarCatalogoOperativoCasoUso;

    public ControladorEventosActualizacion(ConsultarCatalogoOperativoCasoUso consultarCatalogoOperativoCasoUso) {
        this.consultarCatalogoOperativoCasoUso = consultarCatalogoOperativoCasoUso;
    }

    @GetMapping
    public List<RespuestaEventoActualizacion> listar(@RequestParam(defaultValue = "100") int limit, Authentication autenticacion) {
        PermisosAdministrativos.exigirRol(
            autenticacion,
            "Solo ADMIN, OPERATOR o AUDITOR pueden consultar eventos de actualizacion.",
            "ADMIN",
            "OPERATOR",
            "AUDITOR"
        );
        return consultarCatalogoOperativoCasoUso.listarEventosRecientes(limit)
            .stream()
            .map(this::aRespuesta)
            .toList();
    }

    @GetMapping("/page")
    public RespuestaPagina<RespuestaEventoActualizacion> listarPaginado(
        @RequestParam(required = false) UUID deviceId,
        @RequestParam(required = false) UUID deploymentId,
        @RequestParam(required = false) String eventType,
        @RequestParam(required = false) OffsetDateTime from,
        @RequestParam(required = false) OffsetDateTime to,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size,
        @RequestParam(defaultValue = "creadoEn,desc") String sort,
        Authentication autenticacion
    ) {
        PermisosAdministrativos.exigirRol(
            autenticacion,
            "Solo ADMIN, OPERATOR o AUDITOR pueden consultar eventos de actualizacion.",
            "ADMIN",
            "OPERATOR",
            "AUDITOR"
        );
        Pagina<EventoActualizacionRegistrado> pagina = consultarCatalogoOperativoCasoUso.listarEventosPaginado(
            new FiltroEventosActualizacion(deviceId, deploymentId, eventType, from, to, page, size, sort)
        );
        return new RespuestaPagina<>(
            pagina.contenido().stream().map(this::aRespuesta).toList(),
            pagina.pagina(),
            pagina.tamano(),
            pagina.totalElementos(),
            pagina.totalPaginas(),
            pagina.tieneSiguiente()
        );
    }

    private RespuestaEventoActualizacion aRespuesta(EventoActualizacionRegistrado evento) {
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
}
