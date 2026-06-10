package com.farmamia.operations.aplicacion.casouso;

import com.farmamia.operations.dominio.modelo.UsuarioAdministrativo;
import com.farmamia.operations.dominio.puerto.CodificadorContrasenas;
import com.farmamia.operations.dominio.puerto.RepositorioUsuariosAdministrativos;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CambiarContrasenaAdministrativaCasoUso {

    private static final int LONGITUD_MINIMA = 10;

    private final RepositorioUsuariosAdministrativos repositorioUsuariosAdministrativos;
    private final CodificadorContrasenas codificadorContrasenas;

    public CambiarContrasenaAdministrativaCasoUso(
        RepositorioUsuariosAdministrativos repositorioUsuariosAdministrativos,
        CodificadorContrasenas codificadorContrasenas
    ) {
        this.repositorioUsuariosAdministrativos = repositorioUsuariosAdministrativos;
        this.codificadorContrasenas = codificadorContrasenas;
    }

    @Transactional
    public void cambiar(String usuario, String contrasenaActual, String contrasenaNueva) {
        UsuarioAdministrativo usuarioAdministrativo = repositorioUsuariosAdministrativos.buscarActivoPorUsuario(usuario)
            .orElseThrow(() -> new BadCredentialsException("Usuario administrativo no encontrado"));

        if (!codificadorContrasenas.coincide(contrasenaActual, usuarioAdministrativo.hashContrasena())) {
            throw new BadCredentialsException("Contrasena actual invalida");
        }

        validarFortaleza(contrasenaNueva);
        repositorioUsuariosAdministrativos.actualizarHashContrasena(usuario, codificadorContrasenas.codificar(contrasenaNueva));
    }

    private void validarFortaleza(String contrasena) {
        if (contrasena == null || contrasena.length() < LONGITUD_MINIMA) {
            throw new IllegalArgumentException("La contrasena nueva debe tener al menos 10 caracteres.");
        }
        if (!contrasena.matches(".*[A-Z].*")
            || !contrasena.matches(".*[a-z].*")
            || !contrasena.matches(".*\\d.*")) {
            throw new IllegalArgumentException("La contrasena nueva debe incluir mayusculas, minusculas y numeros.");
        }
    }
}
