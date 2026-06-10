package com.farmamia.operations.presentacion.controlador;

import com.farmamia.operations.aplicacion.casouso.GestionarAuditoriaCasoUso;
import com.farmamia.operations.dominio.modelo.AuditoriaRegistrada;
import com.farmamia.operations.dominio.modelo.FiltroAuditoria;
import com.farmamia.operations.presentacion.dto.RespuestaAuditoria;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit-logs")
public class ControladorAuditoria {

    private final GestionarAuditoriaCasoUso gestionarAuditoriaCasoUso;

    public ControladorAuditoria(GestionarAuditoriaCasoUso gestionarAuditoriaCasoUso) {
        this.gestionarAuditoriaCasoUso = gestionarAuditoriaCasoUso;
    }

    @GetMapping
    public List<RespuestaAuditoria> listar(
        @RequestParam(defaultValue = "100") int limit,
        @RequestParam(required = false) String action,
        @RequestParam(required = false) String entityType,
        @RequestParam(required = false) String actorUsername,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
        Authentication autenticacion
    ) {
        PermisosAdministrativos.exigirRol(
            autenticacion,
            "Solo ADMIN o AUDITOR pueden consultar auditoria administrativa.",
            "ADMIN",
            "AUDITOR"
        );
        return gestionarAuditoriaCasoUso.listarConFiltros(new FiltroAuditoria(
                action,
                entityType,
                actorUsername,
                from,
                to,
                limit
            ))
            .stream()
            .map(this::aRespuesta)
            .toList();
    }

    private RespuestaAuditoria aRespuesta(AuditoriaRegistrada auditoria) {
        return new RespuestaAuditoria(
            auditoria.id(),
            auditoria.idUsuarioActor(),
            auditoria.usuarioActor(),
            auditoria.accion(),
            auditoria.tipoEntidad(),
            auditoria.idEntidad(),
            auditoria.valoresAnteriores(),
            auditoria.valoresNuevos(),
            auditoria.direccionIp(),
            auditoria.creadoEn()
        );
    }
}
