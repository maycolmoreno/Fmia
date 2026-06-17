package com.farmamia.posupdate.aplicacion.casouso;

import com.farmamia.posupdate.aplicacion.excepcion.RecursoNoEncontradoException;
import com.farmamia.posupdate.dominio.modelo.FiltroUsuariosAdministrativos;
import com.farmamia.posupdate.dominio.modelo.Pagina;
import com.farmamia.posupdate.dominio.modelo.UsuarioAdministrativo;
import com.farmamia.posupdate.dominio.puerto.CodificadorContrasenas;
import com.farmamia.posupdate.dominio.puerto.RepositorioUsuariosAdministrativos;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GestionarUsuariosAdministrativosCasoUso {

    private static final Set<String> ROLES_VALIDOS = Set.of("ADMIN", "OPERATOR", "AUDITOR", "VIEWER");

    private final RepositorioUsuariosAdministrativos repositorioUsuariosAdministrativos;
    private final CodificadorContrasenas codificadorContrasenas;

    public GestionarUsuariosAdministrativosCasoUso(
        RepositorioUsuariosAdministrativos repositorioUsuariosAdministrativos,
        CodificadorContrasenas codificadorContrasenas
    ) {
        this.repositorioUsuariosAdministrativos = repositorioUsuariosAdministrativos;
        this.codificadorContrasenas = codificadorContrasenas;
    }

    @Transactional(readOnly = true)
    public List<UsuarioAdministrativo> listar() {
        return repositorioUsuariosAdministrativos.listar();
    }

    @Transactional(readOnly = true)
    public Pagina<UsuarioAdministrativo> listarPaginado(FiltroUsuariosAdministrativos filtro) {
        return repositorioUsuariosAdministrativos.listarPaginado(new FiltroUsuariosAdministrativos(
            blancoANulo(filtro.q()),
            blancoANulo(filtro.rol()),
            filtro.activo(),
            filtro.bloqueado(),
            Math.max(0, filtro.pagina()),
            Math.max(1, Math.min(filtro.tamano(), 200)),
            blancoANulo(filtro.orden()) == null ? "usuario,asc" : filtro.orden()
        ));
    }

    @Transactional(readOnly = true)
    public UsuarioAdministrativo obtener(UUID id) {
        return repositorioUsuariosAdministrativos.buscarPorId(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Usuario administrativo no encontrado: " + id));
    }

    @Transactional
    public UsuarioAdministrativo crear(
        String usuario,
        String contrasena,
        String nombreCompleto,
        String correo,
        String rol
    ) {
        validarRol(rol);
        validarContrasena(contrasena);
        validarUnicos(usuario, correo);
        return repositorioUsuariosAdministrativos.crear(
            usuario,
            codificadorContrasenas.codificar(contrasena),
            nombreCompleto,
            correo,
            rol
        );
    }

    @Transactional
    public UsuarioAdministrativo actualizar(UUID id, String nombreCompleto, String correo) {
        UsuarioAdministrativo actual = obtener(id);
        if (correo != null && !correo.isBlank() && !correo.equalsIgnoreCase(actual.correo())
            && repositorioUsuariosAdministrativos.existeCorreo(correo)) {
            throw new IllegalArgumentException("El correo ya esta registrado.");
        }
        return repositorioUsuariosAdministrativos.actualizar(id, nombreCompleto, correo);
    }

    @Transactional
    public UsuarioAdministrativo activar(UUID id) {
        return repositorioUsuariosAdministrativos.activar(id);
    }

    @Transactional
    public UsuarioAdministrativo desactivar(UUID id, String usuarioActor) {
        UsuarioAdministrativo usuario = obtener(id);
        if (usuario.usuario().equals(usuarioActor)) {
            throw new IllegalArgumentException("Un administrador no puede desactivarse a si mismo.");
        }
        return repositorioUsuariosAdministrativos.desactivar(id);
    }

    @Transactional
    public UsuarioAdministrativo cambiarRol(UUID id, String rol, String usuarioActor) {
        validarRol(rol);
        UsuarioAdministrativo usuario = obtener(id);
        if (usuario.usuario().equals(usuarioActor) && "ADMIN".equals(usuario.rol()) && !"ADMIN".equals(rol)) {
            throw new IllegalArgumentException("Un administrador no puede quitarse su propio rol ADMIN.");
        }
        return repositorioUsuariosAdministrativos.cambiarRol(id, rol);
    }

    @Transactional
    public UsuarioAdministrativo resetearContrasena(UUID id, String contrasenaNueva) {
        validarContrasena(contrasenaNueva);
        return repositorioUsuariosAdministrativos.cambiarHashContrasena(id, codificadorContrasenas.codificar(contrasenaNueva));
    }

    private void validarUnicos(String usuario, String correo) {
        if (repositorioUsuariosAdministrativos.existeUsuario(usuario)) {
            throw new IllegalArgumentException("El usuario ya esta registrado.");
        }
        if (repositorioUsuariosAdministrativos.existeCorreo(correo)) {
            throw new IllegalArgumentException("El correo ya esta registrado.");
        }
    }

    private void validarRol(String rol) {
        if (!ROLES_VALIDOS.contains(rol)) {
            throw new IllegalArgumentException("Rol administrativo invalido: " + rol);
        }
    }

    private void validarContrasena(String contrasena) {
        if (contrasena == null || contrasena.length() < 10) {
            throw new IllegalArgumentException("La contrasena debe tener al menos 10 caracteres.");
        }
        if (!contrasena.matches(".*[A-Z].*")
            || !contrasena.matches(".*[a-z].*")
            || !contrasena.matches(".*\\d.*")) {
            throw new IllegalArgumentException("La contrasena debe incluir mayusculas, minusculas y numeros.");
        }
    }

    private String blancoANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
