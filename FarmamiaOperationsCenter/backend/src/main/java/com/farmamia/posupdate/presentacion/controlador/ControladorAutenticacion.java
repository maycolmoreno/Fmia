package com.farmamia.posupdate.presentacion.controlador;

import com.farmamia.posupdate.aplicacion.casouso.GestionarAuditoriaCasoUso;
import com.farmamia.posupdate.dominio.modelo.DatosAuditoria;
import com.farmamia.posupdate.infraestructura.seguridad.ServicioLoginAdministrativo;
import com.farmamia.posupdate.presentacion.dto.RespuestaLogin;
import com.farmamia.posupdate.presentacion.dto.SolicitudLogin;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class ControladorAutenticacion {

    private final ServicioLoginAdministrativo servicioLoginAdministrativo;
    private final GestionarAuditoriaCasoUso gestionarAuditoriaCasoUso;

    public ControladorAutenticacion(
        ServicioLoginAdministrativo servicioLoginAdministrativo,
        GestionarAuditoriaCasoUso gestionarAuditoriaCasoUso
    ) {
        this.servicioLoginAdministrativo = servicioLoginAdministrativo;
        this.gestionarAuditoriaCasoUso = gestionarAuditoriaCasoUso;
    }

    @PostMapping("/login")
    public RespuestaLogin login(@Valid @RequestBody SolicitudLogin solicitud, HttpServletRequest request) {
        RespuestaLogin respuesta;
        try {
            respuesta = servicioLoginAdministrativo.autenticar(solicitud.usuario(), solicitud.contrasena());
        } catch (BadCredentialsException ex) {
            auditarLoginFallido(solicitud.usuario(), ex.getMessage(), request);
            throw ex;
        }

        gestionarAuditoriaCasoUso.registrar(new DatosAuditoria(
            respuesta.usuario(),
            "ADMIN_LOGIN",
            "AUTH",
            null,
            null,
            Map.of("username", respuesta.usuario(), "role", respuesta.rol()),
            direccionIp(request)
        ));
        return respuesta;
    }

    private void auditarLoginFallido(String usuario, String mensaje, HttpServletRequest request) {
        String accion = mensaje != null && mensaje.toLowerCase().contains("bloqueado")
            ? "ADMIN_LOGIN_LOCKED"
            : "ADMIN_LOGIN_FAILED";
        gestionarAuditoriaCasoUso.registrar(new DatosAuditoria(
            usuario,
            accion,
            "AUTH",
            null,
            null,
            Map.of("username", usuario, "message", mensaje == null ? "Credenciales invalidas" : mensaje),
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
