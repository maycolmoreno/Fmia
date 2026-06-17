package com.farmamia.posupdate.infraestructura.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.UsuarioAppEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.UsuarioAppRepositorioJpa;
import com.farmamia.posupdate.presentacion.dto.RespuestaLogin;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class ServicioLoginAdministrativoTest {

    @Test
    void loginCorrectoReiniciaIntentosYActualizaUltimoAcceso() {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        UsuarioAppEntidad usuario = new UsuarioAppEntidad(
            "admin",
            encoder.encode("Admin12345X"),
            "Administrador",
            "admin@farmamia.local",
            "ADMIN",
            true
        );
        usuario.registrarLoginFallido(5, java.time.Duration.ofMinutes(15));
        ServicioLoginAdministrativo servicio = servicioCon(usuario, encoder);

        RespuestaLogin respuesta = servicio.autenticar("admin", "Admin12345X");

        assertEquals("admin", respuesta.usuario());
        assertEquals("ADMIN", respuesta.rol());
        assertEquals(0, usuario.getIntentosFallidosLogin());
        assertNotNull(usuario.getUltimoAccesoEn());
    }

    @Test
    void quintoIntentoFallidoBloqueaUsuarioDuranteQuinceMinutos() {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        UsuarioAppEntidad usuario = new UsuarioAppEntidad(
            "operador",
            encoder.encode("Operator12345"),
            "Operador",
            "operador@farmamia.local",
            "OPERATOR",
            true
        );
        ServicioLoginAdministrativo servicio = servicioCon(usuario, encoder);

        for (int intento = 1; intento <= 5; intento++) {
            assertThrows(BadCredentialsException.class, () -> servicio.autenticar("operador", "mal"));
        }

        assertEquals(5, usuario.getIntentosFallidosLogin());
        assertNotNull(usuario.getBloqueadoHasta());
        assertThrows(BadCredentialsException.class, () -> servicio.autenticar("operador", "Operator12345"));
    }

    @Test
    void usuarioInactivoNoPuedeIniciarSesion() {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        UsuarioAppEntidad usuario = new UsuarioAppEntidad(
            "viewer",
            encoder.encode("Viewer12345"),
            "Viewer",
            "viewer@farmamia.local",
            "VIEWER",
            false
        );
        ServicioLoginAdministrativo servicio = servicioCon(usuario, encoder);

        assertThrows(BadCredentialsException.class, () -> servicio.autenticar("viewer", "Viewer12345"));
    }

    @Test
    void perfilProduccionRechazaSecretoJwtDemo() {
        assertThrows(IllegalStateException.class, () -> new ServicioJwtAdministrativo(
            new ObjectMapper(),
            new MockEnvironment().withProperty("spring.profiles.active", "prod"),
            "dev-secret-change-me",
            480
        ));
    }

    @Test
    void perfilProduccionAceptaSecretoJwtFuerte() {
        assertDoesNotThrow(() -> new ServicioJwtAdministrativo(
            new ObjectMapper(),
            new MockEnvironment().withProperty("spring.profiles.active", "prod"),
            "prod-secret-farmamia-2026-con-suficiente-longitud",
            480
        ));
    }

    private ServicioLoginAdministrativo servicioCon(UsuarioAppEntidad usuario, PasswordEncoder encoder) {
        UsuarioAppRepositorioJpa repositorio = Mockito.mock(UsuarioAppRepositorioJpa.class);
        when(repositorio.findByUsuario(usuario.getUsuario())).thenReturn(Optional.of(usuario));
        ServicioJwtAdministrativo jwt = new ServicioJwtAdministrativo(
            new ObjectMapper(),
            new MockEnvironment().withProperty("spring.profiles.active", "dev"),
            "dev-secret-change-me",
            480
        );
        return new ServicioLoginAdministrativo(repositorio, encoder, jwt);
    }
}
