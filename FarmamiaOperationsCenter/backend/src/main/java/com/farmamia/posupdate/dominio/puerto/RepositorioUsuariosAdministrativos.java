package com.farmamia.posupdate.dominio.puerto;

import com.farmamia.posupdate.dominio.modelo.FiltroUsuariosAdministrativos;
import com.farmamia.posupdate.dominio.modelo.Pagina;
import com.farmamia.posupdate.dominio.modelo.UsuarioAdministrativo;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioUsuariosAdministrativos {

    Optional<UsuarioAdministrativo> buscarActivoPorUsuario(String usuario);

    Optional<UsuarioAdministrativo> buscarPorUsuario(String usuario);

    Optional<UsuarioAdministrativo> buscarPorId(UUID id);

    List<UsuarioAdministrativo> listar();

    Pagina<UsuarioAdministrativo> listarPaginado(FiltroUsuariosAdministrativos filtro);

    UsuarioAdministrativo crear(String usuario, String hashContrasena, String nombreCompleto, String correo, String rol);

    UsuarioAdministrativo actualizar(UUID id, String nombreCompleto, String correo);

    void actualizarHashContrasena(String usuario, String hashContrasena);

    UsuarioAdministrativo cambiarHashContrasena(UUID id, String hashContrasena);

    UsuarioAdministrativo activar(UUID id);

    UsuarioAdministrativo desactivar(UUID id);

    UsuarioAdministrativo cambiarRol(UUID id, String rol);

    boolean existeUsuario(String usuario);

    boolean existeCorreo(String correo);
}
