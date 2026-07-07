package com.farmamia.posupdate.integracion;

import com.farmamia.posupdate.infraestructura.persistencia.entidad.EquipoEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.SucursalEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.EquipoRepositorioJpa;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.SucursalRepositorioJpa;
import com.farmamia.posupdate.infraestructura.seguridad.FiltroAutenticacionAdministrativa;
import jakarta.servlet.FilterChain;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EquiposAsignacionIntegracionTest extends BaseIntegracionApiTest {

    @Autowired
    private EquipoRepositorioJpa equipoRepositorioJpa;

    @Autowired
    private SucursalRepositorioJpa sucursalRepositorioJpa;

    // Mockeamos el filtro administrativo para evitar que intercepte y aborte la petición
    // por falta del token JWT real en la cabecera. De esta forma, Spring Security delega
    // el contexto de seguridad a la anotación @WithMockUser del test.
    @MockBean
    private FiltroAutenticacionAdministrativa filtroAutenticacionAdministrativa;

    // Configuramos el mock para que pase cada petición al siguiente filtro en la cadena
    // (pass-through), de modo que @WithMockUser pueda establecer el contexto de seguridad
    // y la autorización por roles funcione correctamente. No se hace login real porque
    // el mock no puede procesar el endpoint /api/auth/login de forma habitual.
    @BeforeEach
    @Override
    void prepararSesionAdmin() throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(filtroAutenticacionAdministrativa).doFilter(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "USER") // Rol no autorizado (se requiere OPERATOR o ADMIN)
    void asignarSucursalesRetorna403ParaUsuarioSinRolOperator() throws Exception {
        mockMvc.perform(post("/api/equipos-pos/asignacion-masiva")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "assignments", List.of(Map.of(
                        "deviceId", UUID.randomUUID().toString(),
                        "branchId", UUID.randomUUID().toString()
                    ))
                ))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void asignarSucursalesRetorna401ParaUsuarioAnonimo() throws Exception {
        // Sin autenticación, Spring Security rechaza la petición con 401 Unauthorized
        // al no satisfacer la regla .authenticated() de /api/equipos-pos/**, antes de
        // que la petición llegue al controlador.
        mockMvc.perform(post("/api/equipos-pos/asignacion-masiva")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "assignments", List.of(Map.of(
                        "deviceId", UUID.randomUUID().toString(),
                        "branchId", UUID.randomUUID().toString()
                    ))
                ))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "OPERATOR") // Rol autorizado
    void asignarSucursalesRetornaOkYResumenConConteoParaOperator() throws Exception {
        // 1. Insertar sucursal y equipo huérfano de prueba en la base de datos
        SucursalEntidad sucursal = sucursalRepositorioJpa.save(new SucursalEntidad(
            "FMA-TEST-88", "Farmacia Prueba 88"
        ));

        EquipoEntidad equipo = new EquipoEntidad(null, "TEST-ORPHAN-88", "C:\\Farmamia\\POS");
        equipo.actualizarRegistro(null, "192.168.10.88", "00:11:22:33:44:88", "Windows 11", "1.0", "1.0", "C:\\Farmamia\\POS");
        equipoRepositorioJpa.save(equipo);

        try {
            // 2. Realizar petición POST con payload válido y verificar la respuesta exitosa
            mockMvc.perform(post("/api/equipos-pos/asignacion-masiva")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(Map.of(
                        "assignments", List.of(Map.of(
                            "deviceId", equipo.getId().toString(),
                            "branchId", sucursal.getId().toString()
                        ))
                    ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigned").value(1))
                .andExpect(jsonPath("$.skipped").value(0));
                
        } finally {
            // 3. Limpiar los registros de prueba en la base de datos
            equipoRepositorioJpa.delete(equipo);
            sucursalRepositorioJpa.delete(sucursal);
        }
    }
}
