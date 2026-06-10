package com.farmamia.operations.presentacion.controlador;

import com.farmamia.operations.aplicacion.casouso.GestionarAuditoriaCasoUso;
import com.farmamia.operations.aplicacion.casouso.GestionarDesplieguesCasoUso;
import com.farmamia.operations.dominio.modelo.DatosCrearDespliegue;
import com.farmamia.operations.dominio.modelo.DatosAuditoria;
import com.farmamia.operations.dominio.modelo.Despliegue;
import com.farmamia.operations.dominio.modelo.EstadoDespliegue;
import com.farmamia.operations.presentacion.dto.RespuestaDespliegue;
import com.farmamia.operations.presentacion.dto.RespuestaEstadoDespliegue;
import com.farmamia.operations.presentacion.dto.SolicitudCrearDespliegue;
import com.farmamia.operations.presentacion.dto.SolicitudProgramarDespliegue;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/deployments")
public class ControladorDespliegues {

    private final GestionarDesplieguesCasoUso gestionarDesplieguesCasoUso;
    private final GestionarAuditoriaCasoUso gestionarAuditoriaCasoUso;

    public ControladorDespliegues(
        GestionarDesplieguesCasoUso gestionarDesplieguesCasoUso,
        GestionarAuditoriaCasoUso gestionarAuditoriaCasoUso
    ) {
        this.gestionarDesplieguesCasoUso = gestionarDesplieguesCasoUso;
        this.gestionarAuditoriaCasoUso = gestionarAuditoriaCasoUso;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RespuestaDespliegue crear(
        @Valid @RequestBody SolicitudCrearDespliegue solicitud,
        Authentication autenticacion,
        HttpServletRequest request
    ) {
        exigirOperador(autenticacion);
        RespuestaDespliegue respuesta = aRespuesta(gestionarDesplieguesCasoUso.crear(new DatosCrearDespliegue(
            solicitud.idPaquete(),
            solicitud.nombre(),
            solicitud.descripcion(),
            solicitud.programadoEn(),
            solicitud.grupoObjetivo(),
            solicitud.piloto(),
            solicitud.idsEquipos()
        )));
        auditar(autenticacion, request, "DEPLOYMENT_CREATED", respuesta.id(), Map.of(
            "name", respuesta.nombre(),
            "status", respuesta.estado(),
            "packageId", respuesta.idPaquete(),
            "targetCount", respuesta.cantidadObjetivos()
        ));
        return respuesta;
    }

    @GetMapping
    public List<RespuestaDespliegue> listar() {
        return gestionarDesplieguesCasoUso.listar()
            .stream()
            .map(this::aRespuesta)
            .toList();
    }

    @GetMapping("/{id}")
    public RespuestaDespliegue obtener(@PathVariable UUID id) {
        return aRespuesta(gestionarDesplieguesCasoUso.obtener(id));
    }

    @PostMapping("/{id}/schedule")
    public RespuestaDespliegue programar(
        @PathVariable UUID id,
        @Valid @RequestBody SolicitudProgramarDespliegue solicitud,
        Authentication autenticacion,
        HttpServletRequest request
    ) {
        exigirOperador(autenticacion);
        RespuestaDespliegue respuesta = aRespuesta(gestionarDesplieguesCasoUso.programar(id, solicitud.programadoEn()));
        auditarCambioEstado(autenticacion, request, "DEPLOYMENT_SCHEDULED", respuesta);
        return respuesta;
    }

    @PostMapping("/{id}/pause")
    public RespuestaDespliegue pausar(@PathVariable UUID id, Authentication autenticacion, HttpServletRequest request) {
        exigirOperador(autenticacion);
        RespuestaDespliegue respuesta = aRespuesta(gestionarDesplieguesCasoUso.pausar(id));
        auditarCambioEstado(autenticacion, request, "DEPLOYMENT_PAUSED", respuesta);
        return respuesta;
    }

    @PostMapping("/{id}/resume")
    public RespuestaDespliegue reanudar(@PathVariable UUID id, Authentication autenticacion, HttpServletRequest request) {
        exigirOperador(autenticacion);
        RespuestaDespliegue respuesta = aRespuesta(gestionarDesplieguesCasoUso.reanudar(id));
        auditarCambioEstado(autenticacion, request, "DEPLOYMENT_RESUMED", respuesta);
        return respuesta;
    }

    @PostMapping("/{id}/cancel")
    public RespuestaDespliegue cancelar(@PathVariable UUID id, Authentication autenticacion, HttpServletRequest request) {
        exigirOperador(autenticacion);
        RespuestaDespliegue respuesta = aRespuesta(gestionarDesplieguesCasoUso.cancelar(id));
        auditarCambioEstado(autenticacion, request, "DEPLOYMENT_CANCELLED", respuesta);
        return respuesta;
    }

    @GetMapping("/{id}/status")
    public RespuestaEstadoDespliegue estado(@PathVariable UUID id) {
        return aRespuestaEstado(gestionarDesplieguesCasoUso.estado(id));
    }

    private RespuestaDespliegue aRespuesta(Despliegue despliegue) {
        return new RespuestaDespliegue(
            despliegue.id(),
            despliegue.idPaquete(),
            despliegue.versionPaquete(),
            despliegue.nombre(),
            despliegue.descripcion(),
            despliegue.estado(),
            despliegue.programadoEn(),
            despliegue.creadoEn(),
            despliegue.cantidadObjetivos()
        );
    }

    private RespuestaEstadoDespliegue aRespuestaEstado(EstadoDespliegue estado) {
        return new RespuestaEstadoDespliegue(
            estado.idDespliegue(),
            estado.estado(),
            estado.objetivosPorEstado()
        );
    }

    private void auditarCambioEstado(
        Authentication autenticacion,
        HttpServletRequest request,
        String accion,
        RespuestaDespliegue despliegue
    ) {
        auditar(autenticacion, request, accion, despliegue.id(), Map.of(
            "name", despliegue.nombre(),
            "status", despliegue.estado()
        ));
    }

    private void auditar(
        Authentication autenticacion,
        HttpServletRequest request,
        String accion,
        UUID idEntidad,
        Map<String, Object> valores
    ) {
        gestionarAuditoriaCasoUso.registrar(new DatosAuditoria(
            usuario(autenticacion),
            accion,
            "DEPLOYMENT",
            idEntidad,
            null,
            valores,
            direccionIp(request)
        ));
    }

    private String usuario(Authentication autenticacion) {
        return autenticacion == null ? null : autenticacion.getName();
    }

    private void exigirOperador(Authentication autenticacion) {
        PermisosAdministrativos.exigirRol(
            autenticacion,
            "Solo ADMIN u OPERATOR pueden operar despliegues.",
            "ADMIN",
            "OPERATOR"
        );
    }

    private String direccionIp(HttpServletRequest request) {
        String reenviada = request.getHeader("X-Forwarded-For");
        if (reenviada != null && !reenviada.isBlank()) {
            return reenviada.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
