package com.farmamia.operations.presentacion.controlador;

import com.farmamia.operations.aplicacion.casouso.ConsultarAlertasCasoUso;
import com.farmamia.operations.aplicacion.casouso.GestionarAuditoriaCasoUso;
import com.farmamia.operations.dominio.modelo.AlertaRegistrada;
import com.farmamia.operations.dominio.modelo.DatosAuditoria;
import com.farmamia.operations.dominio.modelo.FiltroAlertas;
import com.farmamia.operations.presentacion.dto.RespuestaAlerta;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alerts")
public class ControladorAlertas {

    private final ConsultarAlertasCasoUso consultarAlertasCasoUso;
    private final GestionarAuditoriaCasoUso gestionarAuditoriaCasoUso;

    public ControladorAlertas(
        ConsultarAlertasCasoUso consultarAlertasCasoUso,
        GestionarAuditoriaCasoUso gestionarAuditoriaCasoUso
    ) {
        this.consultarAlertasCasoUso = consultarAlertasCasoUso;
        this.gestionarAuditoriaCasoUso = gestionarAuditoriaCasoUso;
    }

    @GetMapping
    public List<RespuestaAlerta> listar(
        @RequestParam(defaultValue = "100") int limit,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String severity,
        @RequestParam(name = "type", required = false) String type,
        @RequestParam(required = false) UUID deviceId,
        @RequestParam(required = false) UUID branchId,
        @RequestParam(required = false) String branchCode,
        @RequestParam(required = false) String hostname,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateFrom,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateTo,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(required = false) Integer size,
        @RequestParam(required = false) String sort,
        Authentication autenticacion
    ) {
        PermisosAdministrativos.exigirRol(
            autenticacion,
            "Solo ADMIN, OPERATOR o AUDITOR pueden consultar alertas operativas.",
            "ADMIN",
            "OPERATOR",
            "AUDITOR"
        );
        int tamano = size == null ? limit : size;
        return consultarAlertasCasoUso.listarConFiltros(new FiltroAlertas(
                status,
                severity,
                type,
                deviceId,
                branchId,
                branchCode,
                hostname,
                dateFrom,
                dateTo,
                page,
                tamano,
                sort
            ))
            .stream()
            .map(this::aRespuesta)
            .toList();
    }

    @PostMapping("/{id}/acknowledge")
    public RespuestaAlerta reconocer(@PathVariable UUID id, Authentication autenticacion, HttpServletRequest request) {
        exigirOperador(autenticacion);
        AlertaRegistrada alerta = consultarAlertasCasoUso.reconocer(id, autenticacion.getName());
        auditar(autenticacion, request, "ALERT_ACKNOWLEDGED", alerta);
        return aRespuesta(alerta);
    }

    @PostMapping("/{id}/close")
    public RespuestaAlerta cerrar(@PathVariable UUID id, Authentication autenticacion, HttpServletRequest request) {
        exigirOperador(autenticacion);
        AlertaRegistrada alerta = consultarAlertasCasoUso.cerrar(id, autenticacion.getName());
        auditar(autenticacion, request, "ALERT_CLOSED", alerta);
        return aRespuesta(alerta);
    }

    private RespuestaAlerta aRespuesta(AlertaRegistrada alerta) {
        return new RespuestaAlerta(
            alerta.id(),
            alerta.idEquipo(),
            alerta.nombreEquipo(),
            alerta.idSucursal(),
            alerta.codigoSucursal(),
            alerta.severidad(),
            alerta.tipoAlerta(),
            alerta.titulo(),
            alerta.mensaje(),
            alerta.estado(),
            alerta.abiertaEn(),
            alerta.reconocidaPor(),
            alerta.reconocidaEn(),
            alerta.cerradaPor(),
            alerta.cerradaEn()
        );
    }

    private void exigirOperador(Authentication autenticacion) {
        PermisosAdministrativos.exigirRol(
            autenticacion,
            "Solo ADMIN u OPERATOR pueden gestionar alertas operativas.",
            "ADMIN",
            "OPERATOR"
        );
    }

    private void auditar(Authentication autenticacion, HttpServletRequest request, String accion, AlertaRegistrada alerta) {
        gestionarAuditoriaCasoUso.registrar(new DatosAuditoria(
            autenticacion.getName(),
            accion,
            "ALERT",
            alerta.id(),
            null,
            Map.of(
                "status", alerta.estado(),
                "severity", alerta.severidad(),
                "alertType", alerta.tipoAlerta(),
                "device", alerta.nombreEquipo()
            ),
            direccionIp(request)
        ));
    }

    private String direccionIp(HttpServletRequest request) {
        String reenviada = request.getHeader("X-Forwarded-For");
        if (reenviada != null && !reenviada.isBlank()) {
            return reenviada.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
