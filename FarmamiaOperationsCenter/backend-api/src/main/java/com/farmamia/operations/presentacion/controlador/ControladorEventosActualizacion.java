package com.farmamia.operations.presentacion.controlador;

import com.farmamia.operations.aplicacion.casouso.ConsultarCatalogoOperativoCasoUso;
import com.farmamia.operations.dominio.modelo.EventoActualizacionRegistrado;
import com.farmamia.operations.presentacion.dto.RespuestaEventoActualizacion;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/update-events")
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
