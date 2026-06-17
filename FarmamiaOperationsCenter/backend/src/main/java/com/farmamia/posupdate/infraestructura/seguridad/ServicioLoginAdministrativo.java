package com.farmamia.posupdate.infraestructura.seguridad;

import com.farmamia.posupdate.infraestructura.persistencia.entidad.UsuarioAppEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.UsuarioAppRepositorioJpa;
import com.farmamia.posupdate.presentacion.dto.RespuestaLogin;
import java.time.Duration;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioLoginAdministrativo {

    private static final int MAX_INTENTOS_FALLIDOS = 5;
    private static final Duration DURACION_BLOQUEO = Duration.ofMinutes(15);

    private final UsuarioAppRepositorioJpa usuarioAppRepositorioJpa;
    private final PasswordEncoder passwordEncoder;
    private final ServicioJwtAdministrativo servicioJwtAdministrativo;

    public ServicioLoginAdministrativo(
        UsuarioAppRepositorioJpa usuarioAppRepositorioJpa,
        PasswordEncoder passwordEncoder,
        ServicioJwtAdministrativo servicioJwtAdministrativo
    ) {
        this.usuarioAppRepositorioJpa = usuarioAppRepositorioJpa;
        this.passwordEncoder = passwordEncoder;
        this.servicioJwtAdministrativo = servicioJwtAdministrativo;
    }

    @Transactional
    public RespuestaLogin autenticar(String usuario, String contrasena) {
        UsuarioAppEntidad usuarioApp = usuarioAppRepositorioJpa.findByUsuario(usuario)
            .orElseThrow(() -> new BadCredentialsException("Credenciales invalidas"));

        if (!usuarioApp.isActivo()) {
            throw new BadCredentialsException("Usuario administrativo inactivo");
        }

        if (usuarioApp.estaBloqueado()) {
            throw new BadCredentialsException("Usuario bloqueado temporalmente");
        }

        if (!passwordEncoder.matches(contrasena, usuarioApp.getHashContrasena())) {
            boolean bloqueado = usuarioApp.registrarLoginFallido(MAX_INTENTOS_FALLIDOS, DURACION_BLOQUEO);
            throw new BadCredentialsException(bloqueado ? "Usuario bloqueado temporalmente" : "Credenciales invalidas");
        }

        usuarioApp.registrarLoginCorrecto();
        ServicioJwtAdministrativo.TokenAdministrativo token = servicioJwtAdministrativo.generar(usuarioApp);
        return new RespuestaLogin(
            token.valor(),
            "Bearer",
            token.expiraEn(),
            usuarioApp.getUsuario(),
            usuarioApp.getNombreCompleto(),
            usuarioApp.getRol()
        );
    }
}
