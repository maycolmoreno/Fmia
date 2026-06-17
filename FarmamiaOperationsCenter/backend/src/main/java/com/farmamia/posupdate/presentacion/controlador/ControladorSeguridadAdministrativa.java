package com.farmamia.posupdate.presentacion.controlador;

import com.farmamia.posupdate.aplicacion.casouso.CambiarContrasenaAdministrativaCasoUso;
import com.farmamia.posupdate.aplicacion.casouso.GestionarAuditoriaCasoUso;
import com.farmamia.posupdate.dominio.modelo.DatosAuditoria;
import com.farmamia.posupdate.presentacion.dto.SolicitudCambioContrasena;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/security")
public class ControladorSeguridadAdministrativa {

    private final CambiarContrasenaAdministrativaCasoUso cambiarContrasenaAdministrativaCasoUso;
    private final GestionarAuditoriaCasoUso gestionarAuditoriaCasoUso;

    public ControladorSeguridadAdministrativa(
        CambiarContrasenaAdministrativaCasoUso cambiarContrasenaAdministrativaCasoUso,
        GestionarAuditoriaCasoUso gestionarAuditoriaCasoUso
    ) {
        this.cambiarContrasenaAdministrativaCasoUso = cambiarContrasenaAdministrativaCasoUso;
        this.gestionarAuditoriaCasoUso = gestionarAuditoriaCasoUso;
    }

    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cambiarContrasena(
        @Valid @RequestBody SolicitudCambioContrasena solicitud,
        Authentication autenticacion,
        HttpServletRequest request
    ) {
        String usuario = autenticacion.getName();
        cambiarContrasenaAdministrativaCasoUso.cambiar(usuario, solicitud.contrasenaActual(), solicitud.contrasenaNueva());
        gestionarAuditoriaCasoUso.registrar(new DatosAuditoria(
            usuario,
            "ADMIN_PASSWORD_CHANGED",
            "APP_USER",
            null,
            null,
            Map.of("username", usuario),
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
