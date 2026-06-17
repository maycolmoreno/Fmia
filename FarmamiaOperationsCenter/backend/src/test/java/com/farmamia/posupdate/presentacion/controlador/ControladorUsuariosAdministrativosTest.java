package com.farmamia.posupdate.presentacion.controlador;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.farmamia.posupdate.aplicacion.casouso.GestionarAuditoriaCasoUso;
import com.farmamia.posupdate.aplicacion.casouso.GestionarUsuariosAdministrativosCasoUso;
import com.farmamia.posupdate.dominio.modelo.UsuarioAdministrativo;
import com.farmamia.posupdate.presentacion.dto.SolicitudCrearUsuarioAdministrativo;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class ControladorUsuariosAdministrativosTest {

    @Test
    void adminListaUsuarios() {
        GestionarUsuariosAdministrativosCasoUso casoUso = Mockito.mock(GestionarUsuariosAdministrativosCasoUso.class);
        ControladorUsuariosAdministrativos controlador = new ControladorUsuariosAdministrativos(
            casoUso,
            Mockito.mock(GestionarAuditoriaCasoUso.class)
        );
        when(casoUso.listar()).thenReturn(List.of(usuario("admin", "ADMIN")));

        assertEquals(1, controlador.listar(autenticacion("admin", "ADMIN")).size());
    }

    @Test
    void operatorNoPuedeCrearUsuarios() {
        ControladorUsuariosAdministrativos controlador = new ControladorUsuariosAdministrativos(
            Mockito.mock(GestionarUsuariosAdministrativosCasoUso.class),
            Mockito.mock(GestionarAuditoriaCasoUso.class)
        );

        assertThrows(AccessDeniedException.class, () -> controlador.crear(
            new SolicitudCrearUsuarioAdministrativo(
                "viewer-demo",
                "Viewer12345",
                "Viewer Demo",
                "viewer@farmamia.local",
                "VIEWER"
            ),
            autenticacion("operador", "OPERATOR"),
            null
        ));
    }

    private static UsernamePasswordAuthenticationToken autenticacion(String usuario, String rol) {
        return new UsernamePasswordAuthenticationToken(
            usuario,
            null,
            List.of(new SimpleGrantedAuthority("ROLE_" + rol))
        );
    }

    private static UsuarioAdministrativo usuario(String usuario, String rol) {
        return new UsuarioAdministrativo(
            UUID.randomUUID(),
            usuario,
            "{bcrypt}hash",
            usuario,
            usuario + "@farmamia.local",
            rol,
            true,
            0,
            null,
            null,
            OffsetDateTime.now(),
            OffsetDateTime.now()
        );
    }
}
