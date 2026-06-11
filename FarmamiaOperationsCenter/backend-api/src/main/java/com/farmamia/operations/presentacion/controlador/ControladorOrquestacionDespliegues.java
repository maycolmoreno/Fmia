package com.farmamia.operations.presentacion.controlador;

import com.farmamia.operations.aplicacion.casouso.GestionarAuditoriaCasoUso;
import com.farmamia.operations.aplicacion.casouso.OrquestarDesplieguesCasoUso;
import com.farmamia.operations.dominio.modelo.DatosAuditoria;
import com.farmamia.operations.dominio.modelo.OleadaOrquestacion;
import com.farmamia.operations.dominio.modelo.PlanOrquestacionDespliegue;
import com.farmamia.operations.presentacion.dto.RespuestaOleadaOrquestacion;
import com.farmamia.operations.presentacion.dto.RespuestaPlanOrquestacion;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orchestration/deployments")
public class ControladorOrquestacionDespliegues {

    private final OrquestarDesplieguesCasoUso orquestarDesplieguesCasoUso;
    private final GestionarAuditoriaCasoUso gestionarAuditoriaCasoUso;

    public ControladorOrquestacionDespliegues(
        OrquestarDesplieguesCasoUso orquestarDesplieguesCasoUso,
        GestionarAuditoriaCasoUso gestionarAuditoriaCasoUso
    ) {
        this.orquestarDesplieguesCasoUso = orquestarDesplieguesCasoUso;
        this.gestionarAuditoriaCasoUso = gestionarAuditoriaCasoUso;
    }

    @PostMapping("/{deploymentId}/plan")
    public RespuestaPlanOrquestacion planificar(
        @PathVariable UUID deploymentId,
        @RequestBody(required = false) com.farmamia.operations.presentacion.dto.SolicitudPlanOrquestacion solicitud,
        Authentication autenticacion,
        HttpServletRequest request
    ) {
        exigirOperador(autenticacion);
        RespuestaPlanOrquestacion respuesta = aRespuesta(orquestarDesplieguesCasoUso.planificar(
            deploymentId,
            new com.farmamia.operations.dominio.modelo.SolicitudPlanOrquestacion(
                solicitud == null ? null : solicitud.porcentajeMaximoFallo(),
                solicitud == null || solicitud.pausaAutomaticaHabilitada() == null || solicitud.pausaAutomaticaHabilitada(),
                solicitud == null || solicitud.limiteReintentos() == null ? 2 : solicitud.limiteReintentos(),
                solicitud == null ? null : solicitud.ventanaInicio(),
                solicitud == null ? null : solicitud.ventanaFin()
            )
        ));
        auditar(autenticacion, request, "DEPLOYMENT_ORCHESTRATION_PLANNED", deploymentId, Map.of(
            "waves", respuesta.oleadas().size(),
            "maxFailurePercent", respuesta.porcentajeMaximoFallo()
        ));
        return respuesta;
    }

    @GetMapping("/{deploymentId}/plan")
    public RespuestaPlanOrquestacion obtenerPlan(@PathVariable UUID deploymentId, Authentication autenticacion) {
        exigirLectura(autenticacion);
        return aRespuesta(orquestarDesplieguesCasoUso.obtenerPlan(deploymentId));
    }

    @GetMapping("/{deploymentId}/runtime-status")
    public RespuestaPlanOrquestacion estadoRuntime(@PathVariable UUID deploymentId, Authentication autenticacion) {
        exigirLectura(autenticacion);
        return aRespuesta(orquestarDesplieguesCasoUso.obtenerPlan(deploymentId));
    }

    @PostMapping("/{deploymentId}/evaluate")
    public RespuestaPlanOrquestacion evaluar(
        @PathVariable UUID deploymentId,
        Authentication autenticacion,
        HttpServletRequest request
    ) {
        exigirOperador(autenticacion);
        RespuestaPlanOrquestacion respuesta = aRespuesta(orquestarDesplieguesCasoUso.evaluar(deploymentId));
        auditar(autenticacion, request, "DEPLOYMENT_ORCHESTRATION_EVALUATED", deploymentId, Map.of(
            "controlStatus", respuesta.estadoControl(),
            "pausedReason", respuesta.motivoPausa() == null ? "" : respuesta.motivoPausa()
        ));
        return respuesta;
    }

    @PostMapping("/{deploymentId}/waves/{waveId}/start")
    public RespuestaPlanOrquestacion iniciarOleada(
        @PathVariable UUID deploymentId,
        @PathVariable UUID waveId,
        Authentication autenticacion,
        HttpServletRequest request
    ) {
        exigirOperador(autenticacion);
        RespuestaPlanOrquestacion respuesta = aRespuesta(orquestarDesplieguesCasoUso.iniciarOleada(deploymentId, waveId));
        auditarOleada(autenticacion, request, "DEPLOYMENT_WAVE_STARTED", deploymentId, waveId);
        return respuesta;
    }

    @PostMapping("/{deploymentId}/waves/{waveId}/pause")
    public RespuestaPlanOrquestacion pausarOleada(
        @PathVariable UUID deploymentId,
        @PathVariable UUID waveId,
        Authentication autenticacion,
        HttpServletRequest request
    ) {
        exigirOperador(autenticacion);
        RespuestaPlanOrquestacion respuesta = aRespuesta(orquestarDesplieguesCasoUso.pausarOleada(deploymentId, waveId));
        auditarOleada(autenticacion, request, "DEPLOYMENT_WAVE_PAUSED", deploymentId, waveId);
        return respuesta;
    }

    @PostMapping("/{deploymentId}/waves/{waveId}/resume")
    public RespuestaPlanOrquestacion reanudarOleada(
        @PathVariable UUID deploymentId,
        @PathVariable UUID waveId,
        Authentication autenticacion,
        HttpServletRequest request
    ) {
        exigirOperador(autenticacion);
        RespuestaPlanOrquestacion respuesta = aRespuesta(orquestarDesplieguesCasoUso.reanudarOleada(deploymentId, waveId));
        auditarOleada(autenticacion, request, "DEPLOYMENT_WAVE_RESUMED", deploymentId, waveId);
        return respuesta;
    }

    private RespuestaPlanOrquestacion aRespuesta(PlanOrquestacionDespliegue plan) {
        return new RespuestaPlanOrquestacion(
            plan.idDespliegue(),
            plan.estadoControl(),
            plan.porcentajeMaximoFallo(),
            plan.pausaAutomaticaHabilitada(),
            plan.limiteReintentos(),
            plan.siguienteNumeroOleada(),
            plan.motivoPausa(),
            plan.evaluadoEn(),
            plan.oleadas().stream().map(this::aRespuesta).toList()
        );
    }

    private RespuestaOleadaOrquestacion aRespuesta(OleadaOrquestacion oleada) {
        return new RespuestaOleadaOrquestacion(
            oleada.id(),
            oleada.numero(),
            oleada.nombre(),
            oleada.grupoObjetivo(),
            oleada.piloto(),
            oleada.estado(),
            oleada.objetivosPlanificados(),
            oleada.objetivosCompletados(),
            oleada.objetivosFallidos(),
            oleada.objetivosPendientes(),
            oleada.farmaciasTurno(),
            oleada.porcentajeFallo(),
            oleada.ventanaInicio(),
            oleada.ventanaFin(),
            oleada.iniciadoEn(),
            oleada.completadoEn()
        );
    }

    private void auditarOleada(
        Authentication autenticacion,
        HttpServletRequest request,
        String accion,
        UUID deploymentId,
        UUID waveId
    ) {
        auditar(autenticacion, request, accion, deploymentId, Map.of("waveId", waveId));
    }

    private void auditar(
        Authentication autenticacion,
        HttpServletRequest request,
        String accion,
        UUID deploymentId,
        Map<String, Object> valores
    ) {
        gestionarAuditoriaCasoUso.registrar(new DatosAuditoria(
            autenticacion == null ? null : autenticacion.getName(),
            accion,
            "DEPLOYMENT_ORCHESTRATION",
            deploymentId,
            null,
            valores,
            direccionIp(request)
        ));
    }

    private void exigirOperador(Authentication autenticacion) {
        PermisosAdministrativos.exigirRol(
            autenticacion,
            "Solo ADMIN u OPERATOR pueden operar la orquestacion de despliegues.",
            "ADMIN",
            "OPERATOR"
        );
    }

    private void exigirLectura(Authentication autenticacion) {
        PermisosAdministrativos.exigirRol(
            autenticacion,
            "Solo ADMIN, OPERATOR o AUDITOR pueden consultar la orquestacion de despliegues.",
            "ADMIN",
            "OPERATOR",
            "AUDITOR"
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
