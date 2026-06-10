package com.farmamia.operations.aplicacion.casouso;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.farmamia.operations.dominio.modelo.UsuarioAdministrativo;
import com.farmamia.operations.dominio.puerto.CodificadorContrasenas;
import com.farmamia.operations.dominio.puerto.RepositorioUsuariosAdministrativos;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GestionarUsuariosAdministrativosCasoUsoTest {

    @Test
    void adminCreaUsuarioConHashYRolValido() {
        RepositorioUsuariosEnMemoria repositorio = new RepositorioUsuariosEnMemoria();
        GestionarUsuariosAdministrativosCasoUso casoUso = new GestionarUsuariosAdministrativosCasoUso(
            repositorio,
            new CodificadorContrasenasFake()
        );

        UsuarioAdministrativo usuario = casoUso.crear(
            "operador-demo",
            "Operator12345",
            "Operador Demo",
            "operador@farmamia.local",
            "OPERATOR"
        );

        assertEquals("operador-demo", usuario.usuario());
        assertEquals("hash:Operator12345", usuario.hashContrasena());
        assertEquals("OPERATOR", usuario.rol());
        assertTrue(usuario.activo());
    }

    @Test
    void noPermiteUsuarioOCorreoDuplicado() {
        RepositorioUsuariosEnMemoria repositorio = new RepositorioUsuariosEnMemoria();
        repositorio.usuarios.add(usuario("admin", "admin@farmamia.local", "ADMIN", true));
        GestionarUsuariosAdministrativosCasoUso casoUso = new GestionarUsuariosAdministrativosCasoUso(
            repositorio,
            new CodificadorContrasenasFake()
        );

        assertThrows(IllegalArgumentException.class, () -> casoUso.crear(
            "admin",
            "Admin12345X",
            "Admin Repetido",
            "otro@farmamia.local",
            "ADMIN"
        ));

        assertThrows(IllegalArgumentException.class, () -> casoUso.crear(
            "nuevo-admin",
            "Admin12345X",
            "Admin Repetido",
            "admin@farmamia.local",
            "ADMIN"
        ));
    }

    @Test
    void noPermiteAutoDesactivacionNiQuitarRolAdminPropio() {
        RepositorioUsuariosEnMemoria repositorio = new RepositorioUsuariosEnMemoria();
        UsuarioAdministrativo admin = usuario("admin", "admin@farmamia.local", "ADMIN", true);
        repositorio.usuarios.add(admin);
        GestionarUsuariosAdministrativosCasoUso casoUso = new GestionarUsuariosAdministrativosCasoUso(
            repositorio,
            new CodificadorContrasenasFake()
        );

        assertThrows(IllegalArgumentException.class, () -> casoUso.desactivar(admin.id(), "admin"));
        assertThrows(IllegalArgumentException.class, () -> casoUso.cambiarRol(admin.id(), "VIEWER", "admin"));
    }

    @Test
    void desactivacionLogicaMantieneUsuarioEnListado() {
        RepositorioUsuariosEnMemoria repositorio = new RepositorioUsuariosEnMemoria();
        UsuarioAdministrativo operador = usuario("operador", "operador@farmamia.local", "OPERATOR", true);
        repositorio.usuarios.add(operador);
        GestionarUsuariosAdministrativosCasoUso casoUso = new GestionarUsuariosAdministrativosCasoUso(
            repositorio,
            new CodificadorContrasenasFake()
        );

        UsuarioAdministrativo desactivado = casoUso.desactivar(operador.id(), "admin");

        assertFalse(desactivado.activo());
        assertEquals(1, casoUso.listar().size());
    }

    private static UsuarioAdministrativo usuario(String usuario, String correo, String rol, boolean activo) {
        return new UsuarioAdministrativo(
            UUID.randomUUID(),
            usuario,
            "{bcrypt}hash",
            usuario,
            correo,
            rol,
            activo,
            0,
            null,
            null,
            OffsetDateTime.now(),
            OffsetDateTime.now()
        );
    }

    private static final class CodificadorContrasenasFake implements CodificadorContrasenas {

        @Override
        public boolean coincide(String contrasenaPlano, String hashContrasena) {
            return ("hash:" + contrasenaPlano).equals(hashContrasena);
        }

        @Override
        public String codificar(String contrasenaPlano) {
            return "hash:" + contrasenaPlano;
        }
    }

    private static final class RepositorioUsuariosEnMemoria implements RepositorioUsuariosAdministrativos {

        private final List<UsuarioAdministrativo> usuarios = new ArrayList<>();

        @Override
        public Optional<UsuarioAdministrativo> buscarActivoPorUsuario(String usuario) {
            return buscarPorUsuario(usuario).filter(UsuarioAdministrativo::activo);
        }

        @Override
        public Optional<UsuarioAdministrativo> buscarPorUsuario(String usuario) {
            return usuarios.stream().filter(item -> item.usuario().equals(usuario)).findFirst();
        }

        @Override
        public Optional<UsuarioAdministrativo> buscarPorId(UUID id) {
            return usuarios.stream().filter(item -> item.id().equals(id)).findFirst();
        }

        @Override
        public List<UsuarioAdministrativo> listar() {
            return List.copyOf(usuarios);
        }

        @Override
        public UsuarioAdministrativo crear(String usuario, String hashContrasena, String nombreCompleto, String correo, String rol) {
            UsuarioAdministrativo creado = new UsuarioAdministrativo(
                UUID.randomUUID(),
                usuario,
                hashContrasena,
                nombreCompleto,
                correo,
                rol,
                true,
                0,
                null,
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now()
            );
            usuarios.add(creado);
            return creado;
        }

        @Override
        public UsuarioAdministrativo actualizar(UUID id, String nombreCompleto, String correo) {
            UsuarioAdministrativo actual = buscarPorId(id).orElseThrow();
            UsuarioAdministrativo actualizado = reemplazo(actual, actual.hashContrasena(), nombreCompleto, correo, actual.rol(), actual.activo());
            reemplazar(actualizado);
            return actualizado;
        }

        @Override
        public void actualizarHashContrasena(String usuario, String hashContrasena) {
            UsuarioAdministrativo actual = buscarPorUsuario(usuario).orElseThrow();
            reemplazar(reemplazo(actual, hashContrasena, actual.nombreCompleto(), actual.correo(), actual.rol(), actual.activo()));
        }

        @Override
        public UsuarioAdministrativo cambiarHashContrasena(UUID id, String hashContrasena) {
            UsuarioAdministrativo actual = buscarPorId(id).orElseThrow();
            UsuarioAdministrativo actualizado = reemplazo(actual, hashContrasena, actual.nombreCompleto(), actual.correo(), actual.rol(), actual.activo());
            reemplazar(actualizado);
            return actualizado;
        }

        @Override
        public UsuarioAdministrativo activar(UUID id) {
            UsuarioAdministrativo actual = buscarPorId(id).orElseThrow();
            UsuarioAdministrativo actualizado = reemplazo(actual, actual.hashContrasena(), actual.nombreCompleto(), actual.correo(), actual.rol(), true);
            reemplazar(actualizado);
            return actualizado;
        }

        @Override
        public UsuarioAdministrativo desactivar(UUID id) {
            UsuarioAdministrativo actual = buscarPorId(id).orElseThrow();
            UsuarioAdministrativo actualizado = reemplazo(actual, actual.hashContrasena(), actual.nombreCompleto(), actual.correo(), actual.rol(), false);
            reemplazar(actualizado);
            return actualizado;
        }

        @Override
        public UsuarioAdministrativo cambiarRol(UUID id, String rol) {
            UsuarioAdministrativo actual = buscarPorId(id).orElseThrow();
            UsuarioAdministrativo actualizado = reemplazo(actual, actual.hashContrasena(), actual.nombreCompleto(), actual.correo(), rol, actual.activo());
            reemplazar(actualizado);
            return actualizado;
        }

        @Override
        public boolean existeUsuario(String usuario) {
            return buscarPorUsuario(usuario).isPresent();
        }

        @Override
        public boolean existeCorreo(String correo) {
            return correo != null && usuarios.stream().anyMatch(item -> correo.equalsIgnoreCase(item.correo()));
        }

        private UsuarioAdministrativo reemplazo(
            UsuarioAdministrativo actual,
            String hashContrasena,
            String nombreCompleto,
            String correo,
            String rol,
            boolean activo
        ) {
            return new UsuarioAdministrativo(
                actual.id(),
                actual.usuario(),
                hashContrasena,
                nombreCompleto,
                correo,
                rol,
                activo,
                actual.intentosFallidosLogin(),
                actual.bloqueadoHasta(),
                actual.ultimoAccesoEn(),
                actual.creadoEn(),
                OffsetDateTime.now()
            );
        }

        private void reemplazar(UsuarioAdministrativo actualizado) {
            usuarios.removeIf(item -> item.id().equals(actualizado.id()));
            usuarios.add(actualizado);
        }
    }
}
