package com.farmamia.operations.presentacion.controlador;

import com.farmamia.operations.aplicacion.casouso.GestionarAuditoriaCasoUso;
import com.farmamia.operations.aplicacion.casouso.GestionarUsuariosAdministrativosCasoUso;
import com.farmamia.operations.dominio.modelo.DatosAuditoria;
import com.farmamia.operations.dominio.modelo.UsuarioAdministrativo;
import com.farmamia.operations.presentacion.dto.RespuestaUsuarioAdministrativo;
import com.farmamia.operations.presentacion.dto.SolicitudActualizarUsuarioAdministrativo;
import com.farmamia.operations.presentacion.dto.SolicitudCambioRolUsuario;
import com.farmamia.operations.presentacion.dto.SolicitudCrearUsuarioAdministrativo;
import com.farmamia.operations.presentacion.dto.SolicitudResetContrasenaUsuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class ControladorUsuariosAdministrativos {

    private final GestionarUsuariosAdministrativosCasoUso gestionarUsuariosCasoUso;
    private final GestionarAuditoriaCasoUso gestionarAuditoriaCasoUso;

    public ControladorUsuariosAdministrativos(
        GestionarUsuariosAdministrativosCasoUso gestionarUsuariosCasoUso,
        GestionarAuditoriaCasoUso gestionarAuditoriaCasoUso
    ) {
        this.gestionarUsuariosCasoUso = gestionarUsuariosCasoUso;
        this.gestionarAuditoriaCasoUso = gestionarAuditoriaCasoUso;
    }

    @GetMapping
    public List<RespuestaUsuarioAdministrativo> listar(Authentication autenticacion) {
        exigirAdmin(autenticacion);
        return gestionarUsuariosCasoUso.listar().stream().map(this::aRespuesta).toList();
    }

    @GetMapping("/{id}")
    public RespuestaUsuarioAdministrativo obtener(@PathVariable UUID id, Authentication autenticacion) {
        exigirAdmin(autenticacion);
        return aRespuesta(gestionarUsuariosCasoUso.obtener(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RespuestaUsuarioAdministrativo crear(
        @Valid @RequestBody SolicitudCrearUsuarioAdministrativo solicitud,
        Authentication autenticacion,
        HttpServletRequest request
    ) {
        exigirAdmin(autenticacion);
        UsuarioAdministrativo usuario = gestionarUsuariosCasoUso.crear(
            solicitud.usuario(),
            solicitud.contrasena(),
            solicitud.nombreCompleto(),
            solicitud.correo(),
            solicitud.rol()
        );
        auditar(autenticacion, request, "ADMIN_USER_CREATED", usuario);
        return aRespuesta(usuario);
    }

    @PutMapping("/{id}")
    public RespuestaUsuarioAdministrativo actualizar(
        @PathVariable UUID id,
        @Valid @RequestBody SolicitudActualizarUsuarioAdministrativo solicitud,
        Authentication autenticacion,
        HttpServletRequest request
    ) {
        exigirAdmin(autenticacion);
        UsuarioAdministrativo usuario = gestionarUsuariosCasoUso.actualizar(id, solicitud.nombreCompleto(), solicitud.correo());
        auditar(autenticacion, request, "ADMIN_USER_UPDATED", usuario);
        return aRespuesta(usuario);
    }

    @PostMapping("/{id}/activate")
    public RespuestaUsuarioAdministrativo activar(@PathVariable UUID id, Authentication autenticacion, HttpServletRequest request) {
        exigirAdmin(autenticacion);
        UsuarioAdministrativo usuario = gestionarUsuariosCasoUso.activar(id);
        auditar(autenticacion, request, "ADMIN_USER_ACTIVATED", usuario);
        return aRespuesta(usuario);
    }

    @PostMapping("/{id}/deactivate")
    public RespuestaUsuarioAdministrativo desactivar(@PathVariable UUID id, Authentication autenticacion, HttpServletRequest request) {
        exigirAdmin(autenticacion);
        UsuarioAdministrativo usuario = gestionarUsuariosCasoUso.desactivar(id, autenticacion.getName());
        auditar(autenticacion, request, "ADMIN_USER_DEACTIVATED", usuario);
        return aRespuesta(usuario);
    }

    @PostMapping("/{id}/reset-password")
    public RespuestaUsuarioAdministrativo resetearContrasena(
        @PathVariable UUID id,
        @Valid @RequestBody SolicitudResetContrasenaUsuario solicitud,
        Authentication autenticacion,
        HttpServletRequest request
    ) {
        exigirAdmin(autenticacion);
        UsuarioAdministrativo usuario = gestionarUsuariosCasoUso.resetearContrasena(id, solicitud.contrasenaNueva());
        auditar(autenticacion, request, "ADMIN_USER_PASSWORD_RESET", usuario);
        return aRespuesta(usuario);
    }

    @PostMapping("/{id}/change-role")
    public RespuestaUsuarioAdministrativo cambiarRol(
        @PathVariable UUID id,
        @Valid @RequestBody SolicitudCambioRolUsuario solicitud,
        Authentication autenticacion,
        HttpServletRequest request
    ) {
        exigirAdmin(autenticacion);
        UsuarioAdministrativo usuario = gestionarUsuariosCasoUso.cambiarRol(id, solicitud.rol(), autenticacion.getName());
        auditar(autenticacion, request, "ADMIN_USER_ROLE_CHANGED", usuario);
        return aRespuesta(usuario);
    }

    private void exigirAdmin(Authentication autenticacion) {
        PermisosAdministrativos.exigirRol(autenticacion, "Solo ADMIN puede administrar usuarios.", "ADMIN");
    }

    private void auditar(Authentication autenticacion, HttpServletRequest request, String accion, UsuarioAdministrativo usuario) {
        gestionarAuditoriaCasoUso.registrar(new DatosAuditoria(
            autenticacion.getName(),
            accion,
            "APP_USER",
            usuario.id(),
            null,
            Map.of(
                "username", usuario.usuario(),
                "role", usuario.rol(),
                "active", usuario.activo()
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

    private RespuestaUsuarioAdministrativo aRespuesta(UsuarioAdministrativo usuario) {
        return new RespuestaUsuarioAdministrativo(
            usuario.id(),
            usuario.usuario(),
            usuario.nombreCompleto(),
            usuario.correo(),
            usuario.rol(),
            usuario.activo(),
            usuario.intentosFallidosLogin(),
            usuario.bloqueadoHasta(),
            usuario.ultimoAccesoEn(),
            usuario.creadoEn(),
            usuario.actualizadoEn()
        );
    }
}
