package com.farmamia.operations.infraestructura.persistencia.adaptador;

import com.farmamia.operations.aplicacion.excepcion.RecursoNoEncontradoException;
import com.farmamia.operations.dominio.modelo.FiltroUsuariosAdministrativos;
import com.farmamia.operations.dominio.modelo.Pagina;
import com.farmamia.operations.dominio.modelo.UsuarioAdministrativo;
import com.farmamia.operations.dominio.puerto.RepositorioUsuariosAdministrativos;
import com.farmamia.operations.infraestructura.persistencia.entidad.UsuarioAppEntidad;
import com.farmamia.operations.infraestructura.persistencia.repositorio.UsuarioAppRepositorioJpa;
import java.util.Optional;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Repository
public class RepositorioUsuariosAdministrativosJpaAdaptador implements RepositorioUsuariosAdministrativos {

    private final UsuarioAppRepositorioJpa usuarioAppRepositorioJpa;

    public RepositorioUsuariosAdministrativosJpaAdaptador(UsuarioAppRepositorioJpa usuarioAppRepositorioJpa) {
        this.usuarioAppRepositorioJpa = usuarioAppRepositorioJpa;
    }

    @Override
    public Optional<UsuarioAdministrativo> buscarActivoPorUsuario(String usuario) {
        return usuarioAppRepositorioJpa.findByUsuario(usuario)
            .filter(UsuarioAppEntidad::isActivo)
            .map(this::aDominio);
    }

    @Override
    public Optional<UsuarioAdministrativo> buscarPorUsuario(String usuario) {
        return usuarioAppRepositorioJpa.findByUsuario(usuario).map(this::aDominio);
    }

    @Override
    public Optional<UsuarioAdministrativo> buscarPorId(UUID id) {
        return usuarioAppRepositorioJpa.findById(id).map(this::aDominio);
    }

    @Override
    public List<UsuarioAdministrativo> listar() {
        return usuarioAppRepositorioJpa.findAll().stream().map(this::aDominio).toList();
    }

    @Override
    public Pagina<UsuarioAdministrativo> listarPaginado(FiltroUsuariosAdministrativos filtro) {
        String q = minusculaANulo(filtro.q());
        String rol = minusculaANulo(filtro.rol());
        org.springframework.data.domain.Page<UsuarioAppEntidad> pagina = usuarioAppRepositorioJpa.buscarConFiltros(
            q != null,
            nuloAValor(q),
            rol != null,
            nuloAValor(rol),
            filtro.activo() != null,
            filtro.activo() != null && filtro.activo(),
            filtro.bloqueado() != null,
            filtro.bloqueado() != null && filtro.bloqueado(),
            PageRequest.of(filtro.pagina(), filtro.tamano(), aOrden(filtro.orden()))
        );

        return new Pagina<>(
            pagina.getContent().stream().map(this::aDominio).toList(),
            pagina.getNumber(),
            pagina.getSize(),
            pagina.getTotalElements(),
            pagina.getTotalPages(),
            pagina.hasNext()
        );
    }

    @Override
    public UsuarioAdministrativo crear(String usuario, String hashContrasena, String nombreCompleto, String correo, String rol) {
        return aDominio(usuarioAppRepositorioJpa.save(new UsuarioAppEntidad(
            usuario,
            hashContrasena,
            nombreCompleto,
            correo,
            rol,
            true
        )));
    }

    @Override
    public UsuarioAdministrativo actualizar(UUID id, String nombreCompleto, String correo) {
        UsuarioAppEntidad entidad = buscarEntidad(id);
        entidad.actualizarPerfil(nombreCompleto, correo);
        return aDominio(entidad);
    }

    @Override
    public void actualizarHashContrasena(String usuario, String hashContrasena) {
        UsuarioAppEntidad entidad = usuarioAppRepositorioJpa.findByUsuario(usuario)
            .orElseThrow(() -> new RecursoNoEncontradoException("Usuario administrativo no encontrado: " + usuario));
        entidad.cambiarHashContrasena(hashContrasena);
    }

    @Override
    public UsuarioAdministrativo cambiarHashContrasena(UUID id, String hashContrasena) {
        UsuarioAppEntidad entidad = buscarEntidad(id);
        entidad.cambiarHashContrasena(hashContrasena);
        return aDominio(entidad);
    }

    @Override
    public UsuarioAdministrativo activar(UUID id) {
        UsuarioAppEntidad entidad = buscarEntidad(id);
        entidad.activar();
        return aDominio(entidad);
    }

    @Override
    public UsuarioAdministrativo desactivar(UUID id) {
        UsuarioAppEntidad entidad = buscarEntidad(id);
        entidad.desactivar();
        return aDominio(entidad);
    }

    @Override
    public UsuarioAdministrativo cambiarRol(UUID id, String rol) {
        UsuarioAppEntidad entidad = buscarEntidad(id);
        entidad.cambiarRol(rol);
        return aDominio(entidad);
    }

    @Override
    public boolean existeUsuario(String usuario) {
        return usuarioAppRepositorioJpa.existsByUsuario(usuario);
    }

    @Override
    public boolean existeCorreo(String correo) {
        return correo != null && !correo.isBlank() && usuarioAppRepositorioJpa.existsByCorreo(correo);
    }

    private UsuarioAppEntidad buscarEntidad(UUID id) {
        return usuarioAppRepositorioJpa.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Usuario administrativo no encontrado: " + id));
    }

    private UsuarioAdministrativo aDominio(UsuarioAppEntidad entidad) {
        return new UsuarioAdministrativo(
            entidad.getId(),
            entidad.getUsuario(),
            entidad.getHashContrasena(),
            entidad.getNombreCompleto(),
            entidad.getCorreo(),
            entidad.getRol(),
            entidad.isActivo(),
            entidad.getIntentosFallidosLogin(),
            entidad.getBloqueadoHasta(),
            entidad.getUltimoAccesoEn(),
            entidad.getCreadoEn(),
            entidad.getActualizadoEn()
        );
    }

    private Sort aOrden(String orden) {
        String[] partes = orden == null ? new String[0] : orden.split(",", 2);
        String campo = partes.length > 0 ? partes[0] : "usuario";
        Sort.Direction direccion = partes.length > 1 && "desc".equalsIgnoreCase(partes[1])
            ? Sort.Direction.DESC
            : Sort.Direction.ASC;

        return Sort.by(direccion, switch (campo) {
            case "fullName", "nombreCompleto" -> "nombreCompleto";
            case "email", "correo" -> "correo";
            case "role", "rol" -> "rol";
            case "active", "activo" -> "activo";
            case "lockedUntil", "bloqueadoHasta" -> "bloqueadoHasta";
            case "createdAt", "creadoEn" -> "creadoEn";
            case "updatedAt", "actualizadoEn" -> "actualizadoEn";
            default -> "usuario";
        });
    }

    private String minusculaANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim().toLowerCase(Locale.ROOT);
    }

    private String nuloAValor(String valor) {
        return valor == null ? "" : valor;
    }
}
